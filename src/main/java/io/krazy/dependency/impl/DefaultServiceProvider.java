package io.krazy.dependency.impl;

import io.krazy.dependency.api.*;
import io.krazy.dependency.api.exception.NoSuchServiceException;
import io.krazy.dependency.api.injector.ConstructorInjector;
import io.krazy.dependency.api.injector.FieldInjector;
import io.krazy.dependency.api.injector.MethodInjector;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DefaultServiceProvider implements IServiceProvider
{
    @Getter
    private final MappingResult mappingResult;
    private final ScopeManager scopeManager = new ScopeManager();

    private static class ServiceHolder
    {
        private final Lock lock = new ReentrantLock();
        private volatile @Nullable Object instance;

        public void lock()
        {
            lock.lock();
        }

        public void unlock()
        {
            lock.unlock();
        }
    }

    private static class SingletonManager
    {
        private final Map<Class<?>, ServiceHolder> singletonMap = new ConcurrentHashMap<>();

        public ServiceHolder getHolder(Class<?> klass)
        {
            return singletonMap.computeIfAbsent(klass, it -> new ServiceHolder());
        }

        public void close() throws Exception
        {
            for (var obj : singletonMap.values())
            {
                obj.lock();
                if (obj.instance instanceof AutoCloseable closeable)
                {
                    closeable.close();
                }
                obj.unlock();
            }
        }
    }

    private static class ScopeManager
    {
        private final Map<IServiceRequestable, SingletonManager> scopeMap = new ConcurrentHashMap<>();

        public SingletonManager getSingletonHolder(IServiceRequestable requestable)
        {
            return scopeMap.computeIfAbsent(requestable, it -> new SingletonManager());
        }

        public void close(IServiceRequestable requestable) throws Exception
        {
            var holder = scopeMap.remove(requestable);

            if (holder != null)
            {
                holder.close();
            }
        }
    }

    protected static class Scope implements IServiceScope
    {
        private final DefaultServiceProvider serviceProvider;
        private final ReadWriteLock closeLock = new ReentrantReadWriteLock();
        private volatile boolean isClosed;

        protected Scope(DefaultServiceProvider serviceProvider)
        {
            this.serviceProvider = serviceProvider;
        }

        @Override
        public boolean isClosed()
        {
            return isClosed;
        }

        @Override
        public boolean hasService(Class<?> klass)
        {
            return serviceProvider.hasService(klass);
        }

        @Override
        public <T> T requestService(Class<T> klass)
        {
            closeLock.readLock().lock();
            try
            {
                if (isClosed)
                {
                    throw new IllegalStateException("Couldn't request service from closed scope.");
                }

                return serviceProvider.requestServiceScoped(this, klass);
            }
            finally
            {
                closeLock.readLock().unlock();
            }
        }

        @Override
        public void close() throws Exception
        {
            closeLock.writeLock().lock();
            try
            {
                isClosed = true;
                serviceProvider.closeScope(this);
            }
            finally
            {
                closeLock.writeLock().unlock();
            }
        }
    }

    public DefaultServiceProvider(MappingResult mappingResult)
    {
        this.mappingResult = mappingResult;
    }

    @Override
    public IServiceScope createScope()
    {
        return new Scope(this);
    }

    @Override
    public void closeScope(IServiceScope scope) throws Exception
    {
        scopeManager.close(scope);
    }

    @Override
    public boolean hasService(Class<?> klass)
    {
        return mappingResult.hasRecord(klass);
    }

    @Override
    public <T> T requestService(Class<T> klass) throws RuntimeException
    {
        return requestServiceScoped(this, klass);
    }

    @SuppressWarnings("unchecked")
    protected final <T> T requestServiceScoped(IServiceRequestable requestable, Class<T> klass)
    {
        if (!mappingResult.hasRecord(klass))
        {
            throw new NoSuchServiceException(klass);
        }

        final DependencyRecord currentRecord = mappingResult.getRecord(klass);
        final ServiceDescriptor serviceDescriptor = currentRecord.descriptor();
        final LifetimeType lifetimeType = serviceDescriptor.getLifetimeType();
        final boolean shouldLock = lifetimeType != LifetimeType.TRANSIENT;
        @Nullable SingletonManager singletonManager = null;
        @Nullable ServiceHolder holder = null;

        if (shouldLock)
        {
            singletonManager = scopeManager.getSingletonHolder(requestable);
            holder = singletonManager.getHolder(klass);

            if (holder.instance != null)
            {
                return (T) holder.instance;
            }

            holder.lock();
        }

        try
        {
            if (shouldLock && holder.instance != null)
            {
                return (T) holder.instance;
            }

            final ConstructorInjector ctorInjector = currentRecord.constructorInjector();
            final List<Class<?>> expectedCtorTypes = ctorInjector.getExpectedTypes();
            final Object[] ctorArgs = resolveParameters(requestable, expectedCtorTypes);
            final @Nullable Object defaultObject = serviceDescriptor.getDefaultInstance();

            T serviceInstance = (T) (
                defaultObject != null ?
                defaultObject :
                ctorInjector.getMethodHandle().invokeWithArguments(ctorArgs)
            );

            if (shouldLock)
            {
                holder.instance = serviceInstance;
            }

            for (FieldInjector injector : currentRecord.fieldInjectors())
            {
                Object value = requestService(injector.getExpectedType());
                injector.getMethodHandle().bindTo(serviceInstance).invoke(value);
            }

            for (MethodInjector injector : currentRecord.methodInjectors())
            {
                if (injector.isStatic())
                {
                    throw new IllegalStateException(String.format(
                        "DefaultServiceProvider couldn't handle static method. Passed method: %s",
                        injector
                    ));
                }

                Object[] args = resolveParameters(requestable, injector.getExpectedTypes());
                injector.getMethodHandle().bindTo(serviceInstance).invokeWithArguments(args);
            }

            return serviceInstance;
        }
        catch (Throwable err)
        {
            var msg = String.format(
                "Exception when trying to resolve '%s'.",
                klass.getTypeName()
            );

            throw new RuntimeException(msg, err);
        }
        finally
        {
            if (shouldLock)
            {
                holder.unlock();
            }
        }
    }

    private Object[] resolveParameters(IServiceRequestable requestable, List<Class<?>> expectedTypes)
    {
        final Object[] args = new Object[expectedTypes.size()];

        for (int i = 0; i < args.length; i++)
        {
            final Class<?> type = expectedTypes.get(i);
            args[i] = requestServiceScoped(requestable, type);
        }

        return args;
    }
}
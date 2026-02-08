package io.krazy.dependency.impl;

import io.krazy.dependency.api.*;
import io.krazy.dependency.api.exception.NoSuchServiceException;
import io.krazy.dependency.api.injector.ConstructorInjector;
import io.krazy.dependency.api.injector.FieldInjector;
import io.krazy.dependency.api.injector.MethodInjector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DefaultServiceProviderTest
{
    // Service classes for testing
    interface ServiceA
    {
    }

    interface ServiceB
    {
    }

    static class ServiceAImpl implements ServiceA
    {
    }

    static class ServiceBImpl implements ServiceB
    {
        final ServiceA a;

        public ServiceBImpl(ServiceA a)
        {
            this.a = a;
        }
    }

    static class ServiceC implements AutoCloseable
    {
        final AtomicBoolean closed = new AtomicBoolean(false);

        @Override
        public void close()
        {
            closed.set(true);
        }
    }

    static class ServiceD
    {
        ServiceA a;
    }

    static class ServiceE
    {
        ServiceA a;

        private void setA(ServiceA a)
        {
            this.a = a;
        }
    }

    // Helper to create MappingResult
    private MappingResult createMappingResult(Map<Class<?>, DependencyRecord> records)
    {
        return new MappingResult(records);
    }

    // Helper to create DependencyRecord
    private DependencyRecord createRecord(Class<?> impl, LifetimeType lifetime, Class<?>... ctorParams)
        throws Exception
    {
        ServiceDescriptor descriptor = createDescriptor(impl, lifetime);
        ConstructorInjector ctorInjector = ConstructorInjector.from(impl.getDeclaredConstructor(ctorParams));
        return new DependencyRecord(descriptor, ctorInjector, List.of(), List.of());
    }

    private DependencyRecord createRecordWithField(Class<?> impl, LifetimeType lifetime, String fieldName)
        throws Exception
    {
        ServiceDescriptor descriptor = createDescriptor(impl, lifetime);
        ConstructorInjector ctorInjector = ConstructorInjector.from(impl.getDeclaredConstructor());

        FieldInjector fieldInjector = FieldInjector.from(impl.getDeclaredField(fieldName));

        return new DependencyRecord(descriptor, ctorInjector, List.of(fieldInjector), List.of());
    }

    private DependencyRecord createRecordWithMethod(
        Class<?> impl,
        LifetimeType lifetime,
        String methodName,
        Class<?>... methodParams
    )
        throws Exception
    {
        ServiceDescriptor descriptor = createDescriptor(impl, lifetime);
        ConstructorInjector ctorInjector = ConstructorInjector.from(impl.getDeclaredConstructor());

        MethodInjector methodInjector = MethodInjector.from(impl.getDeclaredMethod(methodName, methodParams));

        return new DependencyRecord(descriptor, ctorInjector, List.of(), List.of(methodInjector));
    }

    private ServiceDescriptor createDescriptor(Class<?> impl, LifetimeType lifetime)
    {
        if (lifetime == LifetimeType.SINGLETON)
        {
            return ServiceDescriptor.forSingleton(impl);
        }
        if (lifetime == LifetimeType.TRANSIENT)
        {
            return ServiceDescriptor.forTransient(impl);
        }
        if (lifetime == LifetimeType.SCOPED)
        {
            return ServiceDescriptor.forScoped(impl);
        }
        throw new IllegalArgumentException("Unknown lifetime");
    }

    // --- Tests ---

    @Test
    void testHasService() throws Exception
    {
        Map<Class<?>, DependencyRecord> map = new HashMap<>();
        map.put(ServiceA.class, createRecord(ServiceAImpl.class, LifetimeType.SINGLETON));

        DefaultServiceProvider provider = new DefaultServiceProvider(createMappingResult(map));

        Assertions.assertTrue(provider.hasService(ServiceA.class));
        Assertions.assertFalse(provider.hasService(ServiceB.class));
    }

    @Test
    void testRequestSingleton() throws Exception
    {
        Map<Class<?>, DependencyRecord> map = new HashMap<>();
        map.put(ServiceA.class, createRecord(ServiceAImpl.class, LifetimeType.SINGLETON));

        DefaultServiceProvider provider = new DefaultServiceProvider(createMappingResult(map));

        ServiceA a1 = provider.requestService(ServiceA.class);
        ServiceA a2 = provider.requestService(ServiceA.class);

        Assertions.assertNotNull(a1);
        Assertions.assertSame(a1, a2);
    }

    @Test
    void testRequestTransient() throws Exception
    {
        Map<Class<?>, DependencyRecord> map = new HashMap<>();
        map.put(ServiceA.class, createRecord(ServiceAImpl.class, LifetimeType.TRANSIENT));

        DefaultServiceProvider provider = new DefaultServiceProvider(createMappingResult(map));

        ServiceA a1 = provider.requestService(ServiceA.class);
        ServiceA a2 = provider.requestService(ServiceA.class);

        Assertions.assertNotNull(a1);
        Assertions.assertNotSame(a1, a2);
    }

    @Test
    void testRequestScoped() throws Exception
    {
        Map<Class<?>, DependencyRecord> map = new HashMap<>();
        map.put(ServiceA.class, createRecord(ServiceAImpl.class, LifetimeType.SCOPED));

        DefaultServiceProvider provider = new DefaultServiceProvider(createMappingResult(map));
        IServiceScope scope1 = provider.createScope();
        IServiceScope scope2 = provider.createScope();

        // Scoped service via Scope
        ServiceA a1 = scope1.requestService(ServiceA.class);
        ServiceA a2 = scope1.requestService(ServiceA.class);
        Assertions.assertSame(a1, a2, "Same scope should return same instance");

        ServiceA a3 = scope2.requestService(ServiceA.class);
        Assertions.assertNotSame(a1, a3, "Different scope should return different instance");

        ServiceA aRoot = provider.requestService(ServiceA.class);
        ServiceA aRoot2 = provider.requestService(ServiceA.class);
        Assertions.assertSame(aRoot, aRoot2);
        Assertions.assertNotSame(aRoot, a1);
    }

    @Test
    void testDependencyInjection() throws Exception
    {
        Map<Class<?>, DependencyRecord> map = new HashMap<>();
        map.put(ServiceA.class, createRecord(ServiceAImpl.class, LifetimeType.SINGLETON));
        map.put(ServiceB.class, createRecord(ServiceBImpl.class, LifetimeType.TRANSIENT, ServiceA.class));

        DefaultServiceProvider provider = new DefaultServiceProvider(createMappingResult(map));

        ServiceB b = provider.requestService(ServiceB.class);
        Assertions.assertNotNull(b);
        Assertions.assertInstanceOf(ServiceBImpl.class, b);
        Assertions.assertNotNull(((ServiceBImpl) b).a);
    }

    @Test
    void testFieldInjection() throws Exception
    {
        Map<Class<?>, DependencyRecord> map = new HashMap<>();
        map.put(ServiceA.class, createRecord(ServiceAImpl.class, LifetimeType.SINGLETON));
        map.put(ServiceD.class, createRecordWithField(ServiceD.class, LifetimeType.TRANSIENT, "a"));

        DefaultServiceProvider provider = new DefaultServiceProvider(createMappingResult(map));

        ServiceD d = provider.requestService(ServiceD.class);
        Assertions.assertNotNull(d);
        Assertions.assertNotNull(d.a);
    }

    @Test
    void testMethodInjection() throws Exception
    {
        Map<Class<?>, DependencyRecord> map = new HashMap<>();
        map.put(ServiceA.class, createRecord(ServiceAImpl.class, LifetimeType.SINGLETON));
        map.put(ServiceE.class, createRecordWithMethod(ServiceE.class, LifetimeType.TRANSIENT, "setA", ServiceA.class));

        DefaultServiceProvider provider = new DefaultServiceProvider(createMappingResult(map));

        ServiceE e = provider.requestService(ServiceE.class);
        Assertions.assertNotNull(e);
        Assertions.assertNotNull(e.a);
    }

    @Test
    void testCloseScope() throws Exception
    {
        Map<Class<?>, DependencyRecord> map = new HashMap<>();
        map.put(ServiceC.class, createRecord(ServiceC.class, LifetimeType.SCOPED));

        DefaultServiceProvider provider = new DefaultServiceProvider(createMappingResult(map));

        ServiceC c;
        try (IServiceScope scope = provider.createScope())
        {
            c = scope.requestService(ServiceC.class);
            Assertions.assertFalse(c.closed.get());
        }
        Assertions.assertTrue(c.closed.get(), "Service should be closed when scope is closed");
    }

    @Test
    void testNoSuchServiceException()
    {
        Map<Class<?>, DependencyRecord> map = new HashMap<>();
        DefaultServiceProvider provider = new DefaultServiceProvider(createMappingResult(map));

        Assertions.assertThrows(NoSuchServiceException.class, () -> provider.requestService(ServiceA.class));
    }

    @Test
    void testScopeClosedException() throws Exception
    {
        Map<Class<?>, DependencyRecord> map = new HashMap<>();
        DefaultServiceProvider provider = new DefaultServiceProvider(createMappingResult(map));
        IServiceScope scope = provider.createScope();
        scope.close();

        Assertions.assertThrows(IllegalStateException.class, () -> scope.requestService(ServiceA.class));
    }

    @Test
    void testConcurrency() throws Exception
    {
        Map<Class<?>, DependencyRecord> map = new HashMap<>();
        map.put(ServiceA.class, createRecord(ServiceAImpl.class, LifetimeType.SINGLETON));

        DefaultServiceProvider provider = new DefaultServiceProvider(createMappingResult(map));

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try
        {
            List<Callable<ServiceA>> tasks = IntStream.range(0, threadCount)
                .mapToObj(i -> (Callable<ServiceA>) () -> provider.requestService(ServiceA.class))
                .collect(Collectors.toList());

            List<Future<ServiceA>> futures = executor.invokeAll(tasks);

            ServiceA firstInstance = futures.get(0).get();
            Assertions.assertNotNull(firstInstance);

            for (Future<ServiceA> future : futures)
            {
                Assertions.assertSame(
                    firstInstance, future.get(),
                    "All threads should receive availability of the same Singleton instance"
                );
            }
        }
        finally
        {
            executor.shutdownNow();
        }
    }

    @Test
    void testConcurrencyScoped() throws Exception
    {
        Map<Class<?>, DependencyRecord> map = new HashMap<>();
        map.put(ServiceA.class, createRecord(ServiceAImpl.class, LifetimeType.SCOPED));

        DefaultServiceProvider provider = new DefaultServiceProvider(createMappingResult(map));
        IServiceScope scope = provider.createScope();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try
        {
            List<Callable<ServiceA>> tasks = IntStream.range(0, threadCount)
                .mapToObj(i -> (Callable<ServiceA>) () -> scope.requestService(ServiceA.class))
                .collect(Collectors.toList());

            List<Future<ServiceA>> futures = executor.invokeAll(tasks);

            ServiceA firstInstance = futures.get(0).get();
            Assertions.assertNotNull(firstInstance);

            for (Future<ServiceA> future : futures)
            {
                Assertions.assertSame(
                    firstInstance, future.get(),
                    "All threads should receive availability of the same Scoped instance within the same scope"
                );
            }
        }
        finally
        {
            executor.shutdownNow();
        }
    }

    @Test
    void testConcurrencyMultiScope() throws Exception
    {
        Map<Class<?>, DependencyRecord> map = new HashMap<>();
        // Use SCOPED lifetime
        map.put(ServiceA.class, createRecord(ServiceAImpl.class, LifetimeType.SCOPED));

        DefaultServiceProvider provider = new DefaultServiceProvider(createMappingResult(map));
        IServiceScope scope1 = provider.createScope();
        IServiceScope scope2 = provider.createScope();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount * 2);
        CountDownLatch latch = new CountDownLatch(1);

        try
        {
            List<Callable<ServiceA>> tasks = new ArrayList<>();

            // Tasks for scope 1
            for (int i = 0; i < threadCount; i++)
            {

                tasks.add(() ->
                {
                    latch.await();
                    return scope1.requestService(ServiceA.class);
                });
            }

            // Tasks for scope 2
            for (int i = 0; i < threadCount; i++)
            {

                tasks.add(() ->
                {
                    latch.await();
                    return scope2.requestService(ServiceA.class);
                });
            }

            List<Future<ServiceA>> futures = new ArrayList<>();
            for (Callable<ServiceA> task : tasks)
            {
                futures.add(executor.submit(task));
            }

            // Start the race
            latch.countDown();

            // Collect results
            ServiceA scope1Instance = null;
            ServiceA scope2Instance = null;

            // First 10 are scope 1
            for (int i = 0; i < threadCount; i++)
            {
                ServiceA instance = futures.get(i).get(); // waits for completion
                Assertions.assertNotNull(instance);
                if (scope1Instance == null)
                {
                    scope1Instance = instance;
                }
                else
                {
                    Assertions.assertSame(
                        scope1Instance, instance,
                        "Scope 1 should consistently return the same instance"
                    );
                }
            }

            // Next 10 are scope 2
            for (int i = threadCount; i < threadCount * 2; i++)
            {
                ServiceA instance = futures.get(i).get();
                Assertions.assertNotNull(instance);
                if (scope2Instance == null)
                {
                    scope2Instance = instance;
                }
                else
                {
                    Assertions.assertSame(
                        scope2Instance, instance,
                        "Scope 2 should consistently return the same instance"
                    );
                }
            }

            Assertions.assertNotSame(scope1Instance, scope2Instance, "Scopes should have distinct instances");

        }
        finally
        {
            executor.shutdownNow();
        }
    }

    @Test
    void testAsyncScopeClose() throws Exception
    {
        Map<Class<?>, DependencyRecord> map = new HashMap<>();
        map.put(ServiceC.class, createRecord(ServiceC.class, LifetimeType.SCOPED));

        DefaultServiceProvider provider = new DefaultServiceProvider(createMappingResult(map));
        IServiceScope scope = provider.createScope();
        ServiceC c = scope.requestService(ServiceC.class);

        Assertions.assertFalse(c.closed.get());

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try
        {
            Future<?> future = executor.submit(() ->
            {
                try
                {
                    scope.close();
                    latch.countDown();
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            });

            // Wait for close to complete
            latch.await();
            future.get(); // Check for exceptions

            Assertions.assertTrue(c.closed.get(), "Service should be closed after async scope close");
            Assertions.assertTrue(scope.isClosed(), "Scope should be reportedly closed");

            Assertions.assertThrows(IllegalStateException.class, () -> scope.requestService(ServiceC.class));

        }
        finally
        {
            executor.shutdownNow();
        }
    }

    @Test
    void testScopeCloseRaceCondition() throws Exception
    {
        Map<Class<?>, DependencyRecord> map = new HashMap<>();
        map.put(ServiceA.class, createRecord(ServiceAImpl.class, LifetimeType.SCOPED));

        DefaultServiceProvider provider = new DefaultServiceProvider(createMappingResult(map));
        IServiceScope scope = provider.createScope();

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount + 1);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean running = new AtomicBoolean(true);

        try
        {
            List<Callable<Void>> tasks = new ArrayList<>();

            // Reader threads
            for (int i = 0; i < threadCount; i++)
            {
                tasks.add(() ->
                {
                    startLatch.await();
                    while (running.get())
                    {
                        try
                        {
                            ServiceA a = scope.requestService(ServiceA.class);
                            Assertions.assertNotNull(a);
                        }
                        catch (IllegalStateException e)
                        {
                            // Expected once scope is closed
                            Assertions.assertEquals("Couldn't request service from closed scope.", e.getMessage());
                        }
                    }
                    return null;
                });
            }

            // Closer thread
            tasks.add(() ->
            {
                startLatch.await();
                Thread.sleep(10); // Run a bit
                scope.close();
                running.set(false);
                return null;
            });

            List<Future<Void>> futures = new ArrayList<>();
            for (Callable<Void> task : tasks)
            {
                futures.add(executor.submit(task));
            }

            startLatch.countDown();

            for (Future<Void> future : futures)
            {
                future.get();
            }

            Assertions.assertTrue(scope.isClosed());
            Assertions.assertThrows(IllegalStateException.class, () -> scope.requestService(ServiceA.class));
        }
        finally
        {
            executor.shutdownNow();
        }
    }
}

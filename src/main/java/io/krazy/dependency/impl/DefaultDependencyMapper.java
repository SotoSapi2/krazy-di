package io.krazy.dependency.impl;

import io.krazy.dependency.api.*;
import io.krazy.dependency.api.annotation.InjectDependency;
import io.krazy.dependency.api.exception.CircularDependencyException;
import io.krazy.dependency.api.exception.NoSuchServiceException;
import io.krazy.dependency.api.exception.UnconstructableException;
import io.krazy.dependency.api.injector.AbstractDependencyInjector;
import io.krazy.dependency.api.injector.ConstructorInjector;
import io.krazy.dependency.api.injector.FieldInjector;
import io.krazy.dependency.api.injector.MethodInjector;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public class DefaultDependencyMapper implements IDependencyMapper
{
    private final boolean isAbleToResolvePrivate;
    private final IServiceConfigurator configurator;

    private static class SearchStackData
    {
        final ServiceDescriptor descriptor;
        final ConstructorInjector ctorInjector;
        final List<FieldInjector> fieldInjectorList = new ArrayList<>();
        final List<MethodInjector> methodInjectorList = new ArrayList<>();
        AbstractDependencyInjector<?> lastestInjector;
        boolean isVisited;

        public SearchStackData(ServiceDescriptor descriptor, ConstructorInjector ctorInjector)
        {
            this.descriptor = descriptor;
            this.ctorInjector = ctorInjector;
            this.lastestInjector = ctorInjector;
        }

        public void addFieldInjector(FieldInjector injector)
        {
            fieldInjectorList.add(injector);
        }

        public void addMethodInjector(MethodInjector injector)
        {
            methodInjectorList.add(injector);
        }

        public DependencyRecord toDependencyRecord()
        {
            return new DependencyRecord(
                descriptor,
                ctorInjector,
                fieldInjectorList,
                methodInjectorList
            );
        }
    }

    private static class SearchContext
    {
        final Map<Class<?>, ServiceDescriptor> descriptorMap;
        final Map<ServiceDescriptor, SearchStackData> searchMap = new HashMap<>();
        final Stack<SearchStackData> searchStack = new Stack<>();

        private SearchContext(Map<Class<?>, ServiceDescriptor> descriptorMap)
        {
            this.descriptorMap = descriptorMap;
        }
    }

    public DefaultDependencyMapper(boolean isAbleToResolvePrivate, IServiceConfigurator configurator)
    {
        this.isAbleToResolvePrivate = isAbleToResolvePrivate;
        this.configurator = configurator;
    }

    @Override
    public boolean isAbleToResolvePrivate()
    {
        return isAbleToResolvePrivate;
    }

    @Override
    public IServiceConfigurator getServiceConfigurator()
    {
        return configurator;
    }

    @Override
    public final MappingResult computeMapping()
        throws IllegalAccessException, NoSuchServiceException, CircularDependencyException
    {
        final Map<Class<?>, ServiceDescriptor> descriptorMap = configurator.getDescriptorMap();
        final SearchContext searchContext = new SearchContext(descriptorMap);

        for (ServiceDescriptor descriptor : descriptorMap.values())
        {
            search(descriptor, searchContext);
        }

        final Map<Class<?>, DependencyRecord> output = new HashMap<>();
        for (var entry : searchContext.searchMap.entrySet())
        {
            output.put(
                entry.getKey().getImplementationType(),
                entry.getValue().toDependencyRecord()
            );
        }

        return new MappingResult(
            Collections.unmodifiableMap(output));
    }

    private void search(ServiceDescriptor currentDescriptor, SearchContext context)
        throws IllegalAccessException, NoSuchServiceException, CircularDependencyException
    {
        final Class<?> implType = currentDescriptor.getImplementationType();
        final Constructor<?> constructor = findInjectorOrDefaultConstructor(implType);
        final ConstructorInjector ctorInjector = ConstructorInjector.from(constructor);
        final SearchStackData searchStackData = context.searchMap.computeIfAbsent(
            currentDescriptor,
            it -> new SearchStackData(currentDescriptor, ctorInjector)
        );

        if (context.searchStack.contains(searchStackData))
        {
            throw new CircularDependencyException(
                "Circular dependency occurred when mapping configuration",
                currentDescriptor,
                context.searchStack.stream()
                    .map(it -> it.lastestInjector)
                    .toList()
            );
        }

        if (searchStackData.isVisited)
        {
            return;
        }

        searchStackData.isVisited = true;
        context.searchStack.add(searchStackData);

        for (Class<?> type : ctorInjector.getExpectedTypes())
        {
            final @Nullable ServiceDescriptor typeDescriptor = context.descriptorMap.get(type);

            if (typeDescriptor == null)
            {
                throw new NoSuchServiceException(type);
            }

            search(typeDescriptor, context);
        }

        for (Field field : findInjectorFields(implType))
        {
            final Class<?> fieldType = field.getType();
            final @Nullable ServiceDescriptor typeDescriptor = context.descriptorMap.get(fieldType);
            final FieldInjector fieldInjector = FieldInjector.from(field);
            searchStackData.addFieldInjector(fieldInjector);

            if (typeDescriptor == null)
            {
                throw new NoSuchServiceException(fieldType);
            }

            search(typeDescriptor, context);
        }

        for (Method method : findInjectorMethods(implType))
        {
            if (Modifier.isStatic(method.getModifiers()))
            {
                throw new AssertionError("Mapper shouldn't handle static method.");
            }

            final Class<?>[] methodTypeParameters = method.getParameterTypes();
            final MethodInjector methodInjector = MethodInjector.from(method);
            searchStackData.addMethodInjector(methodInjector);

            for (Class<?> paramType : methodTypeParameters)
            {
                final @Nullable ServiceDescriptor typeDescriptor = context.descriptorMap.get(paramType);

                if (typeDescriptor == null)
                {
                    throw new NoSuchServiceException(paramType);
                }

                search(typeDescriptor, context);
            }
        }

        context.searchStack.pop();
    }

    @SuppressWarnings("unchecked")
    protected <T> Constructor<T> findInjectorOrDefaultConstructor(Class<T> type)
    {
        var constructors = type.getDeclaredConstructors();
        var injectorConstructorOpt = Arrays.stream(constructors)
            .filter(this::filterConstructor)
            .findFirst();

        if (injectorConstructorOpt.isEmpty())
        {
            throw new UnconstructableException(type, UnconstructableException.FailureType.NO_VALID_CONSTRUCTOR);
        }

        return (Constructor<T>) injectorConstructorOpt.get();
    }

    protected Collection<Method> findInjectorMethods(Class<?> type)
    {
        return Arrays.stream(type.getDeclaredMethods())
            .filter(it -> !Modifier.isStatic(it.getModifiers()))
            .filter(this::filterMethod)
            .toList();
    }

    protected Collection<Field> findInjectorFields(Class<?> type)
    {
        return Arrays.stream(type.getDeclaredFields())
            .filter(it -> !Modifier.isStatic(it.getModifiers()))
            .filter(this::filterField)
            .toList();
    }

    protected boolean filterConstructor(Constructor<?> constructor)
    {
        if (!isMemberAccessible(constructor))
        {
            return false;
        }

        boolean isResolvableByDefault = constructor.accessFlags().contains(AccessFlag.PUBLIC) ||
            constructor.getParameterTypes().length == 0;

        boolean haveInjectionAnnotation = Arrays.stream(constructor.getAnnotations())
            .anyMatch(this::isInjectionAnnotation);

        return haveInjectionAnnotation || isResolvableByDefault;
    }

    protected boolean filterField(Field field)
    {
        if (!isMemberAccessible(field))
        {
            return false;
        }

        int mods = field.getModifiers();
        boolean isFinal = Modifier.isFinal(mods);
        boolean hasInjectionAnnotation = Arrays.stream(field.getDeclaredAnnotations())
            .anyMatch(this::isInjectionAnnotation);

        return !isFinal && hasInjectionAnnotation;
    }

    protected boolean filterMethod(Method method)
    {
        return isMemberAccessible(method);
    }

    protected boolean isInjectionAnnotation(Annotation klass)
    {
        return klass.annotationType().equals(InjectDependency.class);
    }

    protected boolean isMemberAccessible(Member member)
    {
        int mods = member.getModifiers();
        return isAbleToResolvePrivate && Modifier.isPrivate(mods) || Modifier.isPublic(mods);
    }
}

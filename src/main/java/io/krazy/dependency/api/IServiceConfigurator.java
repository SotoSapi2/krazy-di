package io.krazy.dependency.api;

import io.krazy.dependency.api.exception.CircularDependencyException;
import io.krazy.dependency.api.exception.NoSuchServiceException;

import java.util.Map;

/**
 * Interface for configuring and building a dependency injection container.
 * It allows registering service descriptors and building an
 * {@link IServiceProvider}.
 */
public interface IServiceConfigurator
{
    /**
     * Adds a service descriptor for a given mapping type.
     *
     * @param mappingType the type used to request the service
     * @param descriptor  the descriptor containing service implementation and
     *                    lifetime information
     */
    void addDescriptor(Class<?> mappingType, ServiceDescriptor descriptor);

    /**
     * Returns an unmodifiable map of registered service descriptors.
     *
     * @return a map of types to their respective service descriptors
     */
    Map<Class<?>, ServiceDescriptor> getDescriptorMap();

    /**
     * Checks if a descriptor is already registered for the given mapping type.
     *
     * @param mappingType the type to check
     * @return true if a descriptor exists, false otherwise
     */
    boolean hasDescriptor(Class<?> mappingType);

    /**
     * Builds and returns a service provider based on the current configuration.
     * This method computes the dependency graft and validates the configuration.
     *
     * @return a fully configured {@link IServiceProvider}
     * @throws IllegalAccessException      if there's an issue accessing
     *                                     constructors or members
     * @throws NoSuchServiceException      if a required dependency is not
     *                                     registered
     * @throws CircularDependencyException if a circular dependency is detected
     */
    IServiceProvider buildProvider() throws IllegalAccessException, NoSuchServiceException, CircularDependencyException;

    /**
     * Registers a service with a singleton lifetime. A single instance will be
     * created and shared.
     *
     * @param <T>         the mapping type
     * @param mappingType the class of the mapping type
     * @param klass       the implementation class
     */
    default <T> void addSingleton(Class<T> mappingType, Class<? extends T> klass)
    {
        var descriptor = ServiceDescriptor.forSingleton(klass);
        addDescriptor(mappingType, descriptor);
    }

    /**
     * Registers a pre-existing instance as a singleton service.
     *
     * @param <T>         the mapping type
     * @param mappingType the class of the mapping type
     * @param instance    the instance to register
     */
    default <T> void addSingleton(Class<T> mappingType, T instance)
    {
        var descriptor = ServiceDescriptor.forSingleton(instance);
        addDescriptor(mappingType, descriptor);
    }

    /**
     * Registers a service with a transient lifetime. A new instance will be created
     * every time it's requested.
     *
     * @param <T>         the mapping type
     * @param mappingType the class of the mapping type
     * @param klass       the implementation class
     */
    default <T> void addTransient(Class<T> mappingType, Class<? extends T> klass)
    {
        var descriptor = ServiceDescriptor.forTransient(klass);
        addDescriptor(mappingType, descriptor);
    }

    /**
     * Registers a service with a scoped lifetime. A single instance will be created
     * per {@link IServiceScope}.
     *
     * @param <T>         the mapping type
     * @param mappingType the class of the mapping type
     * @param klass       the implementation class
     */
    default <T> void addScoped(Class<T> mappingType, Class<? extends T> klass)
    {
        var descriptor = ServiceDescriptor.forScoped(klass);
        addDescriptor(mappingType, descriptor);
    }
}

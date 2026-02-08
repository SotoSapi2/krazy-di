package io.krazy.dependency.api;

import io.krazy.dependency.api.exception.CircularDependencyException;
import io.krazy.dependency.api.exception.NoSuchServiceException;

/**
 * Interface for mapping service dependencies.
 * It analyzes service implementations to determine how to inject dependencies.
 */
public interface IDependencyMapper
{
    /**
     * Checks if this mapper can resolve dependencies for private members.
     *
     * @return true if private members can be resolved, false otherwise
     */
    boolean isAbleToResolvePrivate();

    /**
     * Gets the service configurator associated with this mapper.
     *
     * @return the associated {@link IServiceConfigurator}
     */
    IServiceConfigurator getServiceConfigurator();

    /**
     * Computes the mapping of dependencies for all registered services.
     *
     * @return a {@link MappingResult} containing the dependency injection records
     * @throws IllegalAccessException      if there's an issue accessing
     *                                     constructors or members
     * @throws NoSuchServiceException      if a required dependency is not
     *                                     registered
     * @throws CircularDependencyException if a circular dependency is detected
     */
    MappingResult computeMapping() throws IllegalAccessException, NoSuchServiceException, CircularDependencyException;
}

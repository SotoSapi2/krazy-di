package io.krazy.dependency.api;

import io.krazy.dependency.api.injector.ConstructorInjector;
import io.krazy.dependency.api.injector.FieldInjector;
import io.krazy.dependency.api.injector.MethodInjector;

import java.util.List;

/**
 * A record containing the dependency injection information for a specific
 * service.
 *
 * @param descriptor          the service descriptor
 * @param constructorInjector the injector for the constructor
 * @param fieldInjectors      a list of injectors for fields
 * @param methodInjectors     a list of injectors for methods
 */
public record DependencyRecord(
    ServiceDescriptor descriptor,
    ConstructorInjector constructorInjector,
    List<FieldInjector> fieldInjectors,
    List<MethodInjector> methodInjectors
)
{
}

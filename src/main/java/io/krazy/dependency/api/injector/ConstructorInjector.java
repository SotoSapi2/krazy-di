package io.krazy.dependency.api.injector;

import lombok.Getter;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.util.List;

/**
 * Injector for class constructors.
 */
public final class ConstructorInjector extends AbstractDependencyInjector<Constructor<?>> {
    /**
     * The list of types expected as arguments for the constructor.
     */
    @Getter
    private final List<Class<?>> expectedTypes;

    /**
     * Creates a new ConstructorInjector from a reflected constructor.
     *
     * @param constructor the reflected constructor
     * @return a new ConstructorInjector
     * @throws IllegalAccessException if the constructor is not accessible
     */
    public static ConstructorInjector from(Constructor<?> constructor) throws IllegalAccessException {
        final List<Class<?>> expectedTypes = List.of(constructor.getParameterTypes());
        final MethodHandle handle = getHandleLookup(constructor.getDeclaringClass())
                .unreflectConstructor(constructor);

        return new ConstructorInjector(constructor, handle, expectedTypes);
    }

    /**
     * Constructs a new ConstructorInjector.
     *
     * @param member        the constructor to inject
     * @param methodHandle  the method handle for invocation
     * @param expectedTypes the list of parameter types
     */
    private ConstructorInjector(
            Constructor<?> member,
            MethodHandle methodHandle,
            List<Class<?>> expectedTypes) {
        super(member, methodHandle);
        this.expectedTypes = expectedTypes;
    }

    @Override
    public String toString() {
        return getMember().getDeclaringClass().getName() + getParameterAsString(getMember());
    }
}

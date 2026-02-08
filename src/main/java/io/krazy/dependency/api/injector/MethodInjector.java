package io.krazy.dependency.api.injector;

import io.krazy.dependency.api.exception.UnconstructableException;
import lombok.Getter;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Injector for class methods.
 */
public final class MethodInjector extends AbstractDependencyInjector<Method>
{
    /**
     * The list of types expected as arguments for the method.
     */
    @Getter
    private final List<Class<?>> expectedTypes;

    /**
     * Creates a new MethodInjector from a reflected method.
     *
     * @param method the reflected method
     * @return a new MethodInjector
     * @throws IllegalAccessException   if the method is not accessible
     * @throws UnconstructableException if the method is abstract
     */
    public static MethodInjector from(Method method) throws IllegalAccessException, UnconstructableException
    {
        int mods = method.getModifiers();
        if (Modifier.isAbstract(mods))
        {
            String msg = String.format(
                "%s.%s is abstract.",
                method.getDeclaringClass().getName(),
                method.getName()
            );

            throw new UnconstructableException(msg, UnconstructableException.FailureType.IS_ABSTRACT);
        }

        final List<Class<?>> expectedTypes = List.of(method.getParameterTypes());
        final MethodHandle handle = getHandleLookup(method.getDeclaringClass())
            .unreflect(method);

        return new MethodInjector(method, handle, expectedTypes);
    }

    /**
     * Checks if the method being injected is static.
     *
     * @return true if static, false otherwise
     */
    public boolean isStatic()
    {
        return Modifier.isStatic(getMember().getModifiers());
    }

    /**
     * Constructs a new MethodInjector.
     *
     * @param member        the method to inject
     * @param methodHandle  the method handle for invocation
     * @param expectedTypes the list of parameter types
     */
    private MethodInjector(Method member, MethodHandle methodHandle, List<Class<?>> expectedTypes)
    {
        super(member, methodHandle);
        this.expectedTypes = expectedTypes;
    }

    @Override
    public String toString()
    {
        return getMember().getDeclaringClass().getName() +
            "." +
            getMember().getName() +
            getParameterAsString(getMember());
    }
}

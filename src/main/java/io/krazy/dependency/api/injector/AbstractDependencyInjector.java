package io.krazy.dependency.api.injector;

import lombok.Getter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Base class for all dependency injectors.
 * It manages the member being injected and the corresponding method handle.
 *
 * @param <T> the type of {@link Member} handled by this injector
 */
public abstract sealed class AbstractDependencyInjector<T extends Member>
    permits ConstructorInjector, FieldInjector, MethodInjector
{
    /**
     * The member (field, constructor, or method) to be injected.
     */
    @Getter
    private final T member;

    /**
     * The method handle used for performing the injection.
     */
    @Getter
    private final MethodHandle methodHandle;

    /**
     * Constructs a new AbstractDependencyInjector.
     *
     * @param member       the member to inject
     * @param methodHandle the method handle for injection
     */
    protected AbstractDependencyInjector(T member, MethodHandle methodHandle)
    {
        this.member = member;
        this.methodHandle = methodHandle;
    }

    /**
     * Gets a method handle lookup for the specified class, including private
     * access.
     *
     * @param klass the class to look up
     * @return the lookup object
     * @throws IllegalAccessException if private access is denied
     */
    protected static MethodHandles.Lookup getHandleLookup(Class<?> klass) throws IllegalAccessException
    {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        return MethodHandles.privateLookupIn(klass, lookup);
    }

    /**
     * Formats the parameters of an executable (method or constructor) as a string.
     *
     * @param executable the executable to format
     * @return a string representing the parameter types (e.g.,
     * "(java.lang.String,int)")
     */
    protected static String getParameterAsString(Executable executable)
    {
        return Arrays.stream(executable.getParameterTypes())
            .map(Class::getName)
            .collect(Collectors.joining(",", "(", ")"));
    }
}

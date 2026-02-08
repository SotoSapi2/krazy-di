package io.krazy.dependency.api.injector;

import lombok.Getter;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Injector for class fields.
 */
@Getter
public final class FieldInjector extends AbstractDependencyInjector<Field>
{
    /**
     * The type expected for the field being injected.
     */
    private final Class<?> expectedType;

    /**
     * Creates a new FieldInjector from a reflected field.
     *
     * @param field the reflected field
     * @return a new FieldInjector
     * @throws IllegalAccessException if the field is not accessible
     * @throws IllegalStateException  if the field is final
     */
    public static FieldInjector from(Field field) throws IllegalAccessException, IllegalStateException
    {
        int mods = field.getModifiers();
        if (Modifier.isFinal(mods))
        {
            throw new IllegalStateException(String.format(
                "%s.%s is final.",
                field.getDeclaringClass().getName(),
                field.getName()
            ));
        }

        final Class<?> type = field.getType();
        final MethodHandle handle = getHandleLookup(field.getDeclaringClass())
            .unreflectSetter(field);

        return new FieldInjector(field, handle, type);
    }

    /**
     * Constructs a new FieldInjector.
     *
     * @param member       the field to inject
     * @param methodHandle the method handle for setting the field
     * @param expectedType the expected type of the dependency
     */
    private FieldInjector(Field member, MethodHandle methodHandle, Class<?> expectedType)
    {
        super(member, methodHandle);
        this.expectedType = expectedType;
    }

    @Override
    public String toString()
    {
        return getMember().getDeclaringClass().getName() + "." + getMember().getName();
    }
}

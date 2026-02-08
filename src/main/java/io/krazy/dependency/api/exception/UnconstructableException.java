package io.krazy.dependency.api.exception;

import lombok.Getter;

import java.lang.reflect.Modifier;

/**
 * Exception thrown when a type cannot be constructed by the DI container.
 */
public class UnconstructableException extends DependencyException
{
    /**
     * Reasons why a type might be unconstructable.
     */
    public enum FailureType
    {
        /**
         * The type is an interface.
         */
        IS_INTERFACE,
        /**
         * The type is an abstract class.
         */
        IS_ABSTRACT,
        /**
         * The type is a primitive.
         */
        IS_PRIMITIVE,
        /**
         * The type is an enum.
         */
        IS_ENUM,
        /**
         * The type is void.
         */
        IS_VOID,
        /**
         * The type is an array.
         */
        IS_ARRAY,
        /**
         * No constructor exists.
         */
        NO_CONSTRUCTOR,
        /**
         * No valid public/annotated constructor exists.
         */
        NO_VALID_CONSTRUCTOR
    }

    /**
     * The reason for the failure.
     */
    @Getter
    private final FailureType failureType;

    /**
     * Constructs a new UnconstructableException with a custom message.
     *
     * @param message     the detail message
     * @param failureType the type of failure
     */
    public UnconstructableException(String message, FailureType failureType)
    {
        super(message);
        this.failureType = failureType;
    }

    /**
     * Constructs a new UnconstructableException for a specific type and failure
     * reason.
     *
     * @param type        the class that could not be constructed
     * @param failureType the type of failure
     */
    public UnconstructableException(Class<?> type, FailureType failureType)
    {
        super(getErrorMessage(type, failureType));
        this.failureType = failureType;
    }

    /**
     * Utility method to check if a type is constructable and throw an exception if
     * it is not.
     *
     * @param type the type to check
     * @throws UnconstructableException if the type is abstract, an interface, enum,
     *                                  primitive, or array
     */
    public static void throwIfUnconstructable(Class<?> type)
    {
        var mods = type.getModifiers();

        if (Modifier.isAbstract(mods))
        {
            throw new UnconstructableException(type, FailureType.IS_ABSTRACT);
        }
        else if (type.isInterface())
        {
            throw new UnconstructableException(type, FailureType.IS_INTERFACE);
        }
        else if (type.isEnum())
        {
            throw new UnconstructableException(type, FailureType.IS_ENUM);
        }
        else if (type.isPrimitive())
        {
            throw new UnconstructableException(type, FailureType.IS_PRIMITIVE);
        }
        else if (type.isArray())
        {
            throw new UnconstructableException(type, FailureType.IS_ARRAY);
        }
    }

    /**
     * Internal helper to generate a failure message.
     *
     * @param type        the class that failed construction
     * @param failureType the cause of failure
     * @return a descriptive error message
     */
    private static String getErrorMessage(Class<?> type, FailureType failureType)
    {
        String typeName = type.getName();

        return switch (failureType)
        {
            case IS_INTERFACE -> String.format("%s is interface and cannot be construct.", typeName);
            case IS_ABSTRACT -> String.format("%s is abstract class and cannot be construct.", typeName);
            case IS_PRIMITIVE -> String.format("%s is primitive type.", typeName);
            case IS_ENUM -> String.format("%s is enum type.", typeName);
            case IS_VOID -> String.format("%s is void type.", typeName);
            case IS_ARRAY -> String.format("%s is array type.", typeName);
            case NO_CONSTRUCTOR -> String.format("%s doesn't have any constructor.", typeName);
            case NO_VALID_CONSTRUCTOR -> String.format(
                "Cannot construct %s because it doesn't have public or default constructorInjector or constructorInjector with Injection annotation.",
                typeName
            );
        };
    }
}

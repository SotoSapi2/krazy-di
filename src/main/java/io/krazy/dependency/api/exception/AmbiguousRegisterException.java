package io.krazy.dependency.api.exception;

/**
 * Exception thrown when a service is registered with a mapping that already
 * exists.
 */
public class AmbiguousRegisterException extends DependencyException
{
    /**
     * Constructs a new AmbiguousRegisterException with a custom message.
     *
     * @param message the detail message
     */
    public AmbiguousRegisterException(String message)
    {
        super(message);
    }

    /**
     * Constructs a new AmbiguousRegisterException for a specific mapping conflict.
     *
     * @param type           the mapping type that already exists
     * @param implementation the new implementation type that was attempted to be
     *                       registered
     */
    public AmbiguousRegisterException(Class<?> type, Class<?> implementation)
    {
        super("Ambiguous registration for " + type.getName() + ". It is already mapped and you're trying to map it to " + implementation.getName());
    }
}

package io.krazy.dependency.api.exception;

/**
 * Exception thrown when a requested service type is not registered in the DI
 * container.
 */
public class NoSuchServiceException extends DependencyException
{
    /**
     * Constructs a new NoSuchServiceException with a custom message.
     *
     * @param message the detail message
     */
    public NoSuchServiceException(String message)
    {
        super(message);
    }

    /**
     * Constructs a new NoSuchServiceException for a specific service type.
     *
     * @param type the class of the service that could not be found
     */
    public NoSuchServiceException(Class<?> type)
    {
        super("No Such service with " + type.getName() + " type found.");
    }
}

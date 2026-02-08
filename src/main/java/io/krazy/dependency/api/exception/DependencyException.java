package io.krazy.dependency.api.exception;

/**
 * Base class for all exceptions thrown by the KrazyDI library.
 */
public class DependencyException extends RuntimeException
{
    /**
     * Constructs a new DependencyException with the specified detail message.
     *
     * @param message the detail message
     */
    public DependencyException(String message)
    {
        super(message);
    }
}

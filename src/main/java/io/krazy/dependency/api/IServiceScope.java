package io.krazy.dependency.api;

/**
 * Represents a lifetime scope for services. Scoped services are created once
 * per scope.
 * Scopes should be closed (preferably using try-with-resources) to dispose of
 * internal services.
 */
public interface IServiceScope extends IServiceRequestable, AutoCloseable
{
    /**
     * Checks if this scope has been closed.
     *
     * @return true if closed, false otherwise
     */
    boolean isClosed();
}

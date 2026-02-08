package io.krazy.dependency.api;

/**
 * Represents a service provider that can resolve dependencies and create child
 * scopes.
 */
public interface IServiceProvider extends IServiceRequestable
{
    /**
     * Creates a new child scope. Scoped services will have their lifetime tied to
     * this scope.
     *
     * @return a new {@link IServiceScope}
     */
    IServiceScope createScope();

    /**
     * Closes the specified scope and disposes of any disposable services within it.
     *
     * @param scope the scope to close
     * @throws Exception if disposal fails
     */
    void closeScope(IServiceScope scope) throws Exception;
}

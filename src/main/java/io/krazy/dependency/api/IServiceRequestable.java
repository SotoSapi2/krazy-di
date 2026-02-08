package io.krazy.dependency.api;

/**
 * Represents a component that can provide services based on their type.
 */
public interface IServiceRequestable
{
    /**
     * Checks if a service of the specified type is available.
     *
     * @param klass the type of service to check
     * @return true if the service is available, false otherwise
     */
    boolean hasService(Class<?> klass);

    /**
     * Requests an instance of the specified service type.
     *
     * @param <T>   the type of service
     * @param klass the class of the service type
     * @return an instance of the requested service
     * @throws RuntimeException if the service cannot be resolved or created
     */
    <T> T requestService(Class<T> klass) throws RuntimeException;
}

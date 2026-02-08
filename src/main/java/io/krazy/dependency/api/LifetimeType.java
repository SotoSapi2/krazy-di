package io.krazy.dependency.api;

/**
 * Defines the lifetime of a service within the dependency injection container.
 */
public enum LifetimeType
{
    /**
     * A single instance is created once and shared throughout the lifetime of the
     * {@link IServiceProvider}.
     */
    SINGLETON,

    /**
     * A new instance is created every time the service is requested.
     */
    TRANSIENT,

    /**
     * A single instance is created per {@link IServiceScope}.
     */
    SCOPED
}

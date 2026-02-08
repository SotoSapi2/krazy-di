package io.krazy.dependency.api;

import io.krazy.dependency.api.exception.UnconstructableException;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Describes a service and its lifetime. This is used by
 * {@link IServiceConfigurator} to register services.
 */
public final class ServiceDescriptor
{
    /**
     * The implementation class of the service.
     */
    @Getter
    private final Class<?> implementationType;

    /**
     * The lifetime of the service (Singleton, Transient, or Scoped).
     */
    @Getter
    private final LifetimeType lifetimeType;

    /**
     * An optional default instance for singleton services.
     */
    @Getter
    private final @Nullable Object defaultInstance;

    /**
     * Creates a singleton service descriptor with the specified implementation
     * type.
     *
     * @param implementationType the implementation class
     * @return a new singleton service descriptor
     */
    public static ServiceDescriptor forSingleton(Class<?> implementationType)
    {
        return new ServiceDescriptor(implementationType, LifetimeType.SINGLETON, null);
    }

    /**
     * Creates a singleton service descriptor with a pre-existing instance.
     *
     * @param <T>      the type of the instance
     * @param instance the instance to use
     * @return a new singleton service descriptor
     */
    public static <T> ServiceDescriptor forSingleton(T instance)
    {
        return new ServiceDescriptor(instance.getClass(), LifetimeType.SINGLETON, instance);
    }

    /**
     * Creates a transient service descriptor with the specified implementation
     * type.
     *
     * @param implementationType the implementation class
     * @return a new transient service descriptor
     */
    public static ServiceDescriptor forTransient(Class<?> implementationType)
    {
        return new ServiceDescriptor(implementationType, LifetimeType.TRANSIENT, null);
    }

    /**
     * Creates a scoped service descriptor with the specified implementation type.
     *
     * @param <T>                the implementation type
     * @param implementationType the implementation class
     * @return a new scoped service descriptor
     */
    public static <T> ServiceDescriptor forScoped(Class<T> implementationType)
    {
        return new ServiceDescriptor(implementationType, LifetimeType.SCOPED, null);
    }

    private ServiceDescriptor(
        Class<?> implementationType,
        LifetimeType lifetimeType,
        @Nullable Object defaultInstance
    )
    {
        if (defaultInstance != null && implementationType != defaultInstance.getClass())
        {
            throw new IllegalStateException("instance type and expected implementation mismatch!");
        }

        UnconstructableException.throwIfUnconstructable(implementationType);

        this.implementationType = implementationType;
        this.lifetimeType = lifetimeType;
        this.defaultInstance = defaultInstance;
    }

    @Override
    public boolean equals(Object object)
    {
        if (object instanceof ServiceDescriptor that)
        {
            return Objects.equals(implementationType, that.implementationType) &&
                lifetimeType == that.lifetimeType;
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(implementationType, lifetimeType);
    }
}
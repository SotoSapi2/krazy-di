package io.krazy.dependency.api.exception;

import io.krazy.dependency.api.ServiceDescriptor;
import io.krazy.dependency.api.injector.AbstractDependencyInjector;
import lombok.Getter;

import java.util.List;

/**
 * Exception thrown when a circular dependency is detected in the service
 * configuration.
 */
public class CircularDependencyException extends DependencyException
{
    /**
     * The descriptor of the service that caused the cycle.
     */
    @Getter
    private final ServiceDescriptor cycledDescriptor;

    /**
     * The trace of injectors leading to the circular dependency.
     */
    @Getter
    private final List<? extends AbstractDependencyInjector<?>> injectorTrace;

    /**
     * Constructs a new CircularDependencyException.
     *
     * @param message          the detail message
     * @param cycledDescriptor the descriptor of the service that completed the
     *                         cycle
     * @param injectorTrace    the list of injectors involved in the cycle
     */
    public CircularDependencyException(
        String message,
        ServiceDescriptor cycledDescriptor,
        List<? extends AbstractDependencyInjector<?>> injectorTrace
    )
    {
        super(message);
        this.cycledDescriptor = cycledDescriptor;
        this.injectorTrace = injectorTrace;
    }
}

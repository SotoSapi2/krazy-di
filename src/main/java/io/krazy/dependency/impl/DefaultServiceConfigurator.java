package io.krazy.dependency.impl;

import io.krazy.dependency.api.*;
import io.krazy.dependency.api.exception.AmbiguousRegisterException;
import io.krazy.dependency.api.exception.CircularDependencyException;
import io.krazy.dependency.api.exception.NoSuchServiceException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DefaultServiceConfigurator implements IServiceConfigurator
{
    private final Map<Class<?>, ServiceDescriptor> descriptorMapping = new HashMap<>();
    private final IDependencyMapper dependencyMapper;

    public DefaultServiceConfigurator()
    {
        this.dependencyMapper = new DefaultDependencyMapper(true, this);
    }

    public DefaultServiceConfigurator(boolean couldInjectPrivateMember)
    {
        this.dependencyMapper = new DefaultDependencyMapper(couldInjectPrivateMember, this);
    }

    public DefaultServiceConfigurator(IDependencyMapper dependencyMapper)
    {
        this.dependencyMapper = dependencyMapper;
    }

    @Override
    public void addDescriptor(Class<?> mappingType, ServiceDescriptor descriptor)
    {
        if(descriptorMapping.containsKey(mappingType))
        {
            throw new AmbiguousRegisterException(mappingType, descriptor.getImplementationType());
        }

        synchronized (descriptorMapping)
        {
            descriptorMapping.put(mappingType, descriptor);
        }
    }

    @Override
    public Map<Class<?>, ServiceDescriptor> getDescriptorMap()
    {
        return Collections.unmodifiableMap(descriptorMapping);
    }

    @Override
    public boolean hasDescriptor(Class<?> mappingType)
    {
        return descriptorMapping.containsKey(mappingType);
    }

    @Override
    public IServiceProvider buildProvider()
        throws IllegalAccessException, NoSuchServiceException, CircularDependencyException
    {
        MappingResult mappingResult = dependencyMapper.computeMapping();
        return new DefaultServiceProvider(mappingResult);
    }
}

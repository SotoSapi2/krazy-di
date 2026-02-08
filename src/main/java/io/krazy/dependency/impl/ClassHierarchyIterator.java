package io.krazy.dependency.impl;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class ClassHierarchyIterator implements Iterator<Class<?>>, Iterable<Class<?>>
{
    private Class<?> type;

    public ClassHierarchyIterator(Class<?> type)
    {
        this.type = type;
    }

    @Override
    public boolean hasNext()
    {
        return type != null && type != Object.class;
    }

    @Override
    public Class<?> next()
    {
        var current = type;
        type = type.getSuperclass();

        return current;
    }

    @Override
    public @NotNull Iterator<Class<?>> iterator()
    {
        return this;
    }
}

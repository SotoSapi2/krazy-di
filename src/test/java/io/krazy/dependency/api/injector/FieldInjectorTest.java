package io.krazy.dependency.api.injector;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class FieldInjectorTest
{
    @Test
    void shouldCreateFromField() throws IllegalAccessException, NoSuchFieldException
    {
        Field field = TestClass.class.getDeclaredField("value");
        FieldInjector injector = FieldInjector.from(field);

        assertNotNull(injector);
        assertEquals(field, injector.getMember());
        assertEquals(String.class, injector.getExpectedType());
        assertNotNull(injector.getMethodHandle());
    }

    @Test
    void shouldFailForFinalField() throws NoSuchFieldException
    {
        Field field = TestClass.class.getDeclaredField("finalValue");
        assertThrows(IllegalStateException.class, () -> FieldInjector.from(field));
    }

    @Test
    void shouldCreateFromPrimitiveField() throws IllegalAccessException, NoSuchFieldException
    {
        Field field = TestClass.class.getDeclaredField("intValue");
        FieldInjector injector = FieldInjector.from(field);

        assertNotNull(injector);
        assertEquals(int.class, injector.getExpectedType());
    }

    @Test
    void shouldReturnCorrectToString() throws NoSuchFieldException, IllegalAccessException
    {
        Field field = TestClass.class.getDeclaredField("value");
        FieldInjector injector = FieldInjector.from(field);

        String expected = TestClass.class.getName() + ".value";
        assertEquals(expected, injector.toString());
    }

    static class TestClass
    {
        String value;
        final String finalValue = "final";
        int intValue;
    }
}

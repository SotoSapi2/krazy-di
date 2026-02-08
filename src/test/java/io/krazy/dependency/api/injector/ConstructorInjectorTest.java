package io.krazy.dependency.api.injector;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConstructorInjectorTest
{
    @Test
    void shouldCreateFromConstructor() throws IllegalAccessException, NoSuchMethodException
    {
        Constructor<TestClass> constructor = TestClass.class.getDeclaredConstructor(String.class);
        ConstructorInjector injector = ConstructorInjector.from(constructor);

        assertNotNull(injector);
        assertEquals(constructor, injector.getMember());
        assertEquals(List.of(String.class), injector.getExpectedTypes());
        assertNotNull(injector.getMethodHandle());
    }

    @Test
    void shouldCreateFromDefaultConstructor() throws IllegalAccessException, NoSuchMethodException
    {
        Constructor<TestClass> constructor = TestClass.class.getDeclaredConstructor();
        ConstructorInjector injector = ConstructorInjector.from(constructor);

        assertNotNull(injector);
        assertEquals(constructor, injector.getMember());
        assertTrue(injector.getExpectedTypes().isEmpty());
    }

    @Test
    void shouldReturnCorrectToString() throws NoSuchMethodException, IllegalAccessException
    {
        Constructor<TestClass> constructor = TestClass.class.getDeclaredConstructor(String.class);
        ConstructorInjector injector = ConstructorInjector.from(constructor);

        String expected = TestClass.class.getName() + "(java.lang.String)";
        assertEquals(expected, injector.toString());
    }

    static class TestClass
    {
        public TestClass()
        {
        }

        public TestClass(String arg)
        {
        }
    }
}

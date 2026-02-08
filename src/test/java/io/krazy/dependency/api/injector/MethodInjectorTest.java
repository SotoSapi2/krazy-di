package io.krazy.dependency.api.injector;

import io.krazy.dependency.api.exception.UnconstructableException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MethodInjectorTest {
    @Test
    void shouldCreateFromMethod() throws IllegalAccessException, UnconstructableException, NoSuchMethodException {
        Method method = TestClass.class.getDeclaredMethod("method", String.class);
        MethodInjector injector = MethodInjector.from(method);

        assertNotNull(injector);
        assertEquals(method, injector.getMember());
        assertEquals(List.of(String.class), injector.getExpectedTypes());
        assertNotNull(injector.getMethodHandle());
        assertFalse(injector.isStatic());
    }

    @Test
    void shouldCreateFromStaticMethod() throws IllegalAccessException, UnconstructableException, NoSuchMethodException {
        Method method = TestClass.class.getDeclaredMethod("staticMethod");
        MethodInjector injector = MethodInjector.from(method);

        assertNotNull(injector);
        assertTrue(injector.isStatic());
    }

    @Test
    void shouldFailForAbstractMethod() throws NoSuchMethodException {
        Method method = AbstractClass.class.getDeclaredMethod("abstractMethod");
        assertThrows(UnconstructableException.class, () -> MethodInjector.from(method));
    }

    @Test
    void shouldReturnCorrectToString() throws NoSuchMethodException, IllegalAccessException, UnconstructableException {
        Method method = TestClass.class.getDeclaredMethod("method", String.class);
        MethodInjector injector = MethodInjector.from(method);

        String expected = TestClass.class.getName() + ".method(java.lang.String)";
        assertEquals(expected, injector.toString());
    }

    static class TestClass {
        void method(String arg) {
        }

        static void staticMethod() {
        }
    }

    abstract static class AbstractClass {
        abstract void abstractMethod();
    }
}

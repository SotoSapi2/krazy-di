package io.krazy.dependency.impl;

import io.krazy.dependency.api.IServiceProvider;
import io.krazy.dependency.api.LifetimeType;
import io.krazy.dependency.api.ServiceDescriptor;
import io.krazy.dependency.api.exception.NoSuchServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultServiceConfiguratorTest {
    private DefaultServiceConfigurator configurator;

    @BeforeEach
    void setUp() {
        configurator = new DefaultServiceConfigurator(true);
    }

    @Test
    void shouldAddDescriptor() {
        ServiceDescriptor descriptor = ServiceDescriptor.forSingleton(TestService.class);
        configurator.addDescriptor(TestService.class, descriptor);

        assertTrue(configurator.hasDescriptor(TestService.class));
        assertEquals(descriptor, configurator.getDescriptorMap().get(TestService.class));
    }

    @Test
    void shouldReturnImmutableMap() {
        ServiceDescriptor descriptor = ServiceDescriptor.forSingleton(TestService.class);
        configurator.addDescriptor(TestService.class, descriptor);
        Map<Class<?>, ServiceDescriptor> map = configurator.getDescriptorMap();

        assertThrows(UnsupportedOperationException.class, () -> map.put(Object.class, descriptor));
    }

    @Test
    void shouldBuildProvider() throws IllegalAccessException, NoSuchServiceException {
        configurator.addSingleton(TestService.class, TestService.class);
        IServiceProvider provider = configurator.buildProvider();

        assertNotNull(provider);
        assertNotNull(provider.requestService(TestService.class));
    }

    @Test
    void shouldAddSingletonHelper() {
        configurator.addSingleton(TestService.class, TestService.class);
        ServiceDescriptor descriptor = configurator.getDescriptorMap().get(TestService.class);
        assertEquals(LifetimeType.SINGLETON, descriptor.getLifetimeType());
        assertEquals(TestService.class, descriptor.getImplementationType());
    }

    @Test
    void shouldAddTransientHelper() {
        configurator.addTransient(TestService.class, TestService.class);
        ServiceDescriptor descriptor = configurator.getDescriptorMap().get(TestService.class);
        assertEquals(LifetimeType.TRANSIENT, descriptor.getLifetimeType());
        assertEquals(TestService.class, descriptor.getImplementationType());
    }

    @Test
    void shouldAddScopedHelper() {
        configurator.addScoped(TestService.class, TestService.class);
        ServiceDescriptor descriptor = configurator.getDescriptorMap().get(TestService.class);
        assertEquals(LifetimeType.SCOPED, descriptor.getLifetimeType());
        assertEquals(TestService.class, descriptor.getImplementationType());
    }

    @Test
    void shouldAddSingletonInstanceHelper() {
        TestService instance = new TestService();
        configurator.addSingleton(TestService.class, instance);
        ServiceDescriptor descriptor = configurator.getDescriptorMap().get(TestService.class);
        assertEquals(LifetimeType.SINGLETON, descriptor.getLifetimeType());
        assertEquals(TestService.class, descriptor.getImplementationType());
        assertEquals(instance, descriptor.getDefaultInstance());
    }

    public static class TestService {
        public TestService() {
        }
    }
}

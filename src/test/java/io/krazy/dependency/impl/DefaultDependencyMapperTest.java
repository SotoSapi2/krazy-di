package io.krazy.dependency.impl;

import io.krazy.dependency.api.IServiceConfigurator;
import io.krazy.dependency.api.IServiceProvider;
import io.krazy.dependency.api.MappingResult;
import io.krazy.dependency.api.ServiceDescriptor;
import io.krazy.dependency.api.annotation.InjectDependency;
import io.krazy.dependency.api.exception.CircularDependencyException;
import io.krazy.dependency.api.exception.NoSuchServiceException;
import io.krazy.dependency.api.exception.UnconstructableException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class DefaultDependencyMapperTest {
    static class MockConfigurator implements IServiceConfigurator {
        private final Map<Class<?>, ServiceDescriptor> map = new HashMap<>();

        public void add(Class<?> clazz) {
            addDescriptor(clazz, ServiceDescriptor.forSingleton(clazz));
        }

        @Override
        public void addDescriptor(Class<?> mappingType, ServiceDescriptor descriptor) {
            map.put(mappingType, descriptor);
        }

        @Override
        public Map<Class<?>, ServiceDescriptor> getDescriptorMap() {
            return map;
        }

        @Override
        public boolean hasDescriptor(Class<?> mappingType) {
            return map.containsKey(mappingType);
        }

        @Override
        public IServiceProvider buildProvider() throws IllegalAccessException, NoSuchServiceException {
            throw new AssertionError("Should be called!");
        }
    }

    public static class ServiceA {
    }

    public static class ServiceB {
        public ServiceB(ServiceA a) {
        }
    }

    public static class ServiceC {
        @InjectDependency
        public ServiceA a;
    }

    public static class ServiceD {
        @InjectDependency
        public void setA(ServiceA a) {
        }
    }

    public static class CircularA {
        public CircularA(CircularB b) {
        }
    }

    public static class CircularB {
        public CircularB(CircularA a) {
        }
    }

    public static class PrivateService {
        private PrivateService() {
        }
    }

    @Test
    public void testSimpleMapping() throws Exception {
        MockConfigurator config = new MockConfigurator();
        config.add(ServiceA.class);

        DefaultDependencyMapper mapper = new DefaultDependencyMapper(true, config);
        MappingResult result = mapper.computeMapping();

        Assertions.assertTrue(result.hasRecord(ServiceA.class));
    }

    @Test
    public void testConstructorInjection() throws Exception {
        MockConfigurator config = new MockConfigurator();
        config.add(ServiceA.class);
        config.add(ServiceB.class);

        DefaultDependencyMapper mapper = new DefaultDependencyMapper(true, config);
        MappingResult result = mapper.computeMapping();

        Assertions.assertTrue(result.hasRecord(ServiceA.class));
        Assertions.assertTrue(result.hasRecord(ServiceB.class));
    }

    @Test
    public void testFieldInjection() throws Exception {
        MockConfigurator config = new MockConfigurator();
        config.add(ServiceA.class);
        config.add(ServiceC.class);

        DefaultDependencyMapper mapper = new DefaultDependencyMapper(true, config);
        MappingResult result = mapper.computeMapping();

        Assertions.assertTrue(result.hasRecord(ServiceC.class));
    }

    @Test
    public void testMethodInjection() throws Exception {
        MockConfigurator config = new MockConfigurator();
        config.add(ServiceA.class);
        config.add(ServiceD.class);

        DefaultDependencyMapper mapper = new DefaultDependencyMapper(true, config);
        MappingResult result = mapper.computeMapping();

        Assertions.assertTrue(result.hasRecord(ServiceD.class));
    }

    @Test
    public void testCircularDependency() {
        MockConfigurator config = new MockConfigurator();
        config.add(CircularA.class);
        config.add(CircularB.class);

        DefaultDependencyMapper mapper = new DefaultDependencyMapper(true, config);

        Assertions.assertThrows(CircularDependencyException.class, mapper::computeMapping);
    }

    @Test
    public void testMissingService() {
        MockConfigurator config = new MockConfigurator();
        config.add(ServiceB.class);
        // missing ServiceA

        DefaultDependencyMapper mapper = new DefaultDependencyMapper(true, config);

        Assertions.assertThrows(NoSuchServiceException.class, mapper::computeMapping);
    }

    @Test
    public void testUnconstructablePrivate() {
        MockConfigurator config = new MockConfigurator();
        config.add(PrivateService.class);

        DefaultDependencyMapper mapper = new DefaultDependencyMapper(false, config);

        Assertions.assertThrows(UnconstructableException.class, mapper::computeMapping);
    }

    @Test
    public void testResolvePrivate() throws Exception {
        MockConfigurator config = new MockConfigurator();
        config.add(PrivateService.class);

        DefaultDependencyMapper mapper = new DefaultDependencyMapper(true, config);
        MappingResult result = mapper.computeMapping();

        Assertions.assertTrue(result.hasRecord(PrivateService.class));
    }
}

module krazy.main {
    exports io.krazy.dependency.api;
    exports io.krazy.dependency.api.annotation;
    exports io.krazy.dependency.api.exception;
    exports io.krazy.dependency.api.injector;
    exports io.krazy.dependency.impl;

    requires static lombok;
    requires static org.jetbrains.annotations;
}
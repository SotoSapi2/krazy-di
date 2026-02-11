# KrazyDI
Simple Java dependency injection library inspired by
.NET dependency injection system.

## Brief of service lifetimes
| Lifetime | Description |
| :--- | :--- |
| **Singleton** | A single instance is created once and shared globally within the provider. |
| **Transient** | A new instance is created every time the service is requested. |
| **Scoped** | A single instance is created per scope.  |

## Quick Start
### 1. Define your services

```java
interface IMessageService 
{
    void sendMessage(String message);
}

class EmailService implements IMessageService 
{
    @Override
    public void sendMessage(String message) 
    {
        System.out.println("Email: " + message);
    }
}

class NotificationService 
{
    private final IMessageService messageService;

    // Implicit constructor injection
    public NotificationService(IMessageService messageService)
    {
        this.messageService = messageService;
    }

    public void notify(String text) 
    {
        messageService.sendMessage(text);
    }
}
```
### 2. Configure and build

```java
public class Main 
{
    public static void main(String[] args) throws Exception 
    {
        // Create a configurator
        IServiceConfigurator configurator = new DefaultServiceConfigurator();

        // Register services
        configurator.addSingleton(IMessageService.class, EmailService.class);
        configurator.addTransient(NotificationService.class, NotificationService.class);

        // Build the provider
        IServiceProvider provider = configurator.buildProvider();

        // Request and use
        NotificationService notifier = provider.requestService(NotificationService.class);
        notifier.notify("Hello KrazyDI!");
    }
}
```
## Advance Usage

### Working with Scopes

Scope will isolate the construction and instance of a service declared
with scoped lifetime from another scope. This is useful for isolating request context.
```java
class UserContext 
{
    private String username;
    public void setUsername(String username) { this.username = username; }
    public String getUsername() { return username; }
}

// ... in main ...
configurator.addScoped(UserContext.class, UserContext.class);
IServiceProvider provider = configurator.buildProvider();

try (IServiceScope scope = provider.createScope())
{
    UserContext ctx = scope.requestService(UserContext.class);
    ctx.setUsername("krazy_juliet");
    System.out.println("Current user: " + ctx.getUsername()); // Current user: krazy_juliet
}

try (IServiceScope scope = provider.createScope())
{
    UserContext ctx = scope.requestService(UserContext.class);
    ctx.setUsername("krazy_joe");
    System.out.println("Current user: " + ctx.getUsername()); // Current user: krazy_joe
}
```

### Annotation-Based Injection

Use `@InjectDependency` to explicitly inject service to constructor, field or method.
```java
class LoggingService 
{
    @InjectDependency
    private IMessageService messageService; // Field injection

    @InjectDependency
    private LoggingService(ILoggingService loggingService) // Constructor injection
    {
        // do stuff
    }
    
    @InjectDependency
    public void init(ConfigService config) // Method injection
    {
        // do stuff
    }
}
```
### Private Member Injection

Enable private injection to private members by passing `true` to the `DefaultServiceConfigurator` constructor.
By default `DefaultServiceConfigurator` will automatically resolve private members.
```java
IServiceConfigurator configurator = new DefaultServiceConfigurator(true);
```

## Gradle
```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'io.github.sotosapi2:krazy-di:1.1'
}
```

## License

MIT License
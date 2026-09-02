# Migration Guide: `ddd4j-quarkus-auth-security` → `ddd4j-quarkus-auth-satoken`

**Status**: `ddd4j-quarkus-auth-security` is deprecated since **3.3.1** and scheduled for removal in **4.0.0**.

## Why deprecate?

Spring Security requires the Spring ecosystem (Spring IoC, Spring AOP, etc.). In a Quarkus environment, this creates unnecessary baggage:

- Quarkus uses Arc CDI (not Spring IoC)
- Quarkus uses RESTEasy Reactive (not Spring MVC)
- Spring Security in Quarkus requires `quarkus-spring-security` extension or manual adaptation

`ddd4j-quarkus-auth-satoken` provides:

- Native Quarkus integration (CDI, JTA, JAX-RS)
- No Spring runtime dependencies
- Built-in exception mapper for sa-token exceptions (`SaTokenExceptionMapper` → HTTP 401)
- Annotation-based permission control (`@SaCheckPermission`, `@SaCheckRole`)
- Distributed session support
- Multi-tenant token isolation

## Migration Steps

### 1. Update `pom.xml`

**Remove**:

```xml
<dependency>
    <groupId>io.ddd4j.quarkus</groupId>
    <artifactId>ddd4j-quarkus-auth-security</artifactId>
    <version>${ddd4j-quarkus.version}</version>
</dependency>
```

**Add**:

```xml
<dependency>
    <groupId>io.ddd4j.quarkus</groupId>
    <artifactId>ddd4j-quarkus-auth-satoken</artifactId>
    <version>${ddd4j-quarkus.version}</version>
</dependency>
```

### 2. Update Java code

**Old** (Spring Security based):

```java
@ApplicationScoped
public class MySecurityConfig {
    @Inject SecuritySubjectProvider provider;  // deprecated

    public Subject currentSubject() {
        return provider.getCurrentSubject();
    }
}
```

**New** (sa-token based):

```java
@ApplicationScoped
public class MySecurityConfig {
    @Inject SaTokenSubjectProvider provider;  // from ddd4j-quarkus-auth-satoken

    public Subject currentSubject() {
        return provider.getCurrentSubject();
    }
}
```

> Note: both providers implement the same `io.ddd4j.core.subject.SubjectProvider`
> interface and are registered into `io.ddd4j.core.util.SubjectKit` at startup —
> business code that depends only on `SubjectKit` / `SubjectProvider` needs no change.

### 3. Configuration

**Old** `application.properties`:

```properties
# Spring Security config (removed)
spring.security.user.name=admin
spring.security.user.password=xxx
```

**New** `application.properties`:

```properties
# sa-token config (added)
sa-token.token-name=Authorization
sa-token.timeout=2592000
sa-token.active-timeout=-1
sa-token.is-concurrent=true
sa-token.is-share=true
sa-token.token-style=uuid
```

### 4. Permission annotations

**Old** (Spring Security):

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long id) { ... }
```

**New** (sa-token):

```java
@SaCheckRole("ADMIN")
public void deleteUser(Long id) { ... }
```

## Reference

- sa-token documentation: https://sa-token.dev33.cn/
- ddd4j-quarkus-auth-satoken sample: `ddd4j-quarkus-samples/ddd4j-quarkus-sample-auth-satoken/`
- Issue tracker: https://github.com/ddd-4-java/ddd4j-quarkus/issues

## Support

If you cannot migrate immediately:

- The `ddd4j-quarkus-auth-security` module remains available in 3.x.y versions
- Binary compatibility is preserved (no API breakage in 3.x)
- Removal targeted for 4.0.0 (estimated 2026-Q4)

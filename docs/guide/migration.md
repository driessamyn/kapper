# Migration Guide

This section covers migration strategies for moving to newer versions of Kapper and upgrading between major releases.

## Version Migration

### Upgrading to Kapper 1.6.x

Kapper 1.6.x introduces several new features while maintaining backward compatibility.

#### New Features
- Enhanced Java Records support
- Improved connection pooling integration
- Better error messages for mapping failures
- Performance optimizations

#### Breaking Changes
None. Kapper 1.6.x is fully backward compatible with 1.5.x.

#### Migration Steps
1. Update your dependency to `1.6.1`
2. Test your existing code - no changes required
3. Consider adopting new features where beneficial

### Upgrading to Kapper 1.5.x

#### New Features
- Java Records support
- Improved auto-mapping performance
- Better null handling

#### Migration Steps
```kotlin
// Before: Manual mapping for records
val users = connection.query("SELECT * FROM users") { rs, _ ->
    User(rs.getLong("id"), rs.getString("name"), rs.getString("email"))
}

// After: Automatic mapping works with records
val users = connection.query<User>("SELECT * FROM users")
```

### Upgrading from Kapper 1.4.x to 1.5.x

#### Breaking Changes
- Deprecated `Kapper.createInstance()` - use `Kapper.instance` instead
- Changed exception hierarchy for better error handling

#### Migration Steps
```kotlin
// Before (deprecated)
val kapper = Kapper.createInstance()

// After (recommended)
val kapper = Kapper.instance
```

### Upgrading from Kapper 1.3.x to 1.4.x

#### New Features
- Coroutines support with `kapper-coroutines` module
- Flow-based query results
- Improved connection management

#### Migration Steps
1. Add coroutines dependency if needed:
   ```kotlin
   implementation("net.samyn:kapper-coroutines:1.4.0")
   ```

2. Use new coroutine extensions:
   ```kotlin
   // New: Coroutine support
   suspend fun getUsers(): List<User> {
       return dataSource.withConnection { connection ->
           connection.query<User>("SELECT * FROM users")
       }
   }
   ```

## Framework Migration

For migrating from other frameworks to Kapper, see the comprehensive [Examples Migration Guide](../examples/migration.md) which covers:

- [Migrating from Raw JDBC](../examples/migration.md#migrating-from-raw-jdbc)
- [Migrating from Hibernate/JPA](../examples/migration.md#migrating-from-hibernate-jpa)
- [Migrating from Spring Data JPA](../examples/migration.md#migrating-from-spring-data-jpa)
- [Migrating from jOOQ](../examples/migration.md#migrating-from-jooq)
- [Migrating from Ktorm](../examples/migration.md#migrating-from-ktorm)

## Configuration Migration

### Database Driver Updates

When upgrading Kapper, also consider updating your database drivers:

```kotlin
// PostgreSQL
implementation("org.postgresql:postgresql:42.7.8") // Latest

// MySQL  
implementation("com.mysql:mysql-connector-j:9.4.0") // Latest

// SQLite
implementation("org.xerial:sqlite-jdbc:3.50.3.0") // Latest
```

### Connection Pool Configuration

Optimize your connection pool settings for the new version:

```kotlin
val dataSource = HikariDataSource(HikariConfig().apply {
    // Recommended settings for Kapper 1.6.x
    maximumPoolSize = Runtime.getRuntime().availableProcessors() * 2
    minimumIdle = 5
    connectionTimeout = 20000
    idleTimeout = 300000
    maxLifetime = 1200000
    leakDetectionThreshold = 60000
})
```

## Compatibility Matrix

| Kapper Version | Java Version | Kotlin Version | Coroutines |
|----------------|--------------|----------------|------------|
| 1.6.x          | 8+           | 1.6.0+         | ✅         |
| 1.5.x          | 8+           | 1.6.0+         | ✅         |
| 1.4.x          | 8+           | 1.5.0+         | ✅         |
| 1.3.x          | 8+           | 1.5.0+         | ✅         |
| 1.2.x          | 8+           | 1.5.0+         | ❌         |
| 1.1.x          | 8+           | 1.4.0+         | ❌         |

## Common Migration Issues

### Issue: KapperMappingException after upgrade

**Solution**: Check for breaking changes in auto-mapping behavior:

```kotlin
// If auto-mapping fails, add explicit mapping
val users = connection.query("SELECT * FROM users") { rs, _ ->
    User(
        id = rs.getLong("id"),
        name = rs.getString("name"),
        email = rs.getString("email")
    )
}
```

### Issue: Performance regression

**Solution**: Review connection pool settings and query patterns:

```kotlin
// Ensure proper connection management
dataSource.connection.use { connection ->
    // Your queries here
} // Connection automatically closed
```

### Issue: Compilation errors with new Kotlin version

**Solution**: Update both Kapper and Kotlin to compatible versions:

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "1.9.21" // Latest stable
}

dependencies {
    implementation("net.samyn:kapper:1.6.1") // Latest Kapper
}
```

## Testing Migration

### Comprehensive Test Strategy

1. **Unit Tests**: Test data mapping
2. **Integration Tests**: Test with real database
3. **Performance Tests**: Ensure no regressions
4. **Load Tests**: Verify under concurrent load

```kotlin
@Test
fun `should maintain backward compatibility`() {
    // Test your existing code patterns
    val users = connection.query<User>("SELECT * FROM users")
    assertThat(users).isNotEmpty()
}

@Test  
fun `should handle new features`() {
    // Test new Kapper features
    suspend fun testCoroutines(): List<User> {
        return dataSource.withConnection { connection ->
            connection.query<User>("SELECT * FROM users")
        }
    }
}
```

## Rollback Strategy

Always plan for rollbacks:

1. **Keep previous version in version control**
2. **Test rollback procedure in staging**
3. **Document rollback steps**
4. **Monitor application health after migration**

```kotlin
// Rollback plan example:
// 1. Revert dependency version
// 2. Remove new feature usage
// 3. Deploy previous version
// 4. Verify functionality
```

## Getting Help

- Review [Changelog](../changelog.md) for detailed changes
- Check [GitHub Issues](https://github.com/driessamyn/kapper/issues) for known problems
- Join community discussions for migration support
- Review [Performance Guide](./performance-tuning.md) for optimization tips

## Next Steps

- [Framework Migration Examples](../examples/migration.md) - Detailed migration from other ORMs
- [Performance Tuning](./performance-tuning.md) - Optimize for new version
- [Best Practices](./basic-usage.md) - Learn recommended patterns
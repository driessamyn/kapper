# Performance Tuning

Get the best performance from Kapper with these optimization strategies.

## Connection Pooling

Always use connection pooling in production:

```kotlin
val dataSource = HikariDataSource().apply {
    jdbcUrl = "jdbc:postgresql://localhost:5432/mydb"
    username = "user"
    password = "password"
    
    // Optimize pool settings
    maximumPoolSize = 10
    minimumIdle = 5
    connectionTimeout = 20000
    idleTimeout = 300000
    maxLifetime = 1200000
}
```

## Query Optimization

### Use Prepared Statements
Kapper uses prepared statements by default, which provides:
- SQL injection protection
- Query plan caching
- Better performance for repeated queries

### Batch Operations
Kapper supports batch updates using the `executeAll` function:

```kotlin
val users = listOf(
    User("Alice", "alice@example.com"),
    User("Bob", "bob@example.com"),
    // ... more users
)

connection.executeAll(
    "INSERT INTO users(name, email, age) VALUES(:name, :email, :age)",
    users,
    "name" to User::name,
    "email" to User::email,
)
```

## Memory Management

### Close Resources
Kapper handles resource cleanup automatically when using `connection.use { }` blocks.

## Benchmarks

See our comprehensive [performance benchmarks](../performance/) comparing Kapper with other ORMs.

## Next Steps

- [Performance Benchmarks](../performance/) - See how Kapper performs
- [Database Support](./database-support.md) - Database-specific tips
- [API Reference](../api/kapper/) - Complete API documentation
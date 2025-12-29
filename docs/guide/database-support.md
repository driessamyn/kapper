# Database Support

Kapper is designed to work with any JDBC-compatible database, it simply extends the JDBC APIs.
We test Kapper against some standard DBs, but it has been proven to work against many more.

## Supported Databases

Kapper officially supports and is tested against:

* PostgreSQL
* MySQL
* SQLite
* MS SQL Server
* Oracle

## Connection Setup

Kapper simply uses a standard DataSource, which can be provided by any supported library.
Here as some examples using the Hikari Connection Pool:

### PostgreSQL
```kotlin
val dataSource = HikariDataSource().apply {
    jdbcUrl = "jdbc:postgresql://localhost:5432/mydb"
    username = "user"
    password = "password"
}
```

### MySQL
```kotlin
val dataSource = HikariDataSource().apply {
    jdbcUrl = "jdbc:mysql://localhost:3306/mydb?allowMultiQueries=true"
    username = "user" 
    password = "password"
}
```

### SQLite
```kotlin
val dataSource = HikariDataSource().apply {
    jdbcUrl = "jdbc:sqlite:mydb.db"
}
```

## Type Mapping

Kapper automatically handles type conversion between many SQL and JVM/Kotlin types.
When a type is not supprted, it can be supported through [custom mappers](mapping.md)

## Testing

For testing, consider using:
- **SQLite** for in-memory or file-based tests  
- **Testcontainers** for production-like integration tests

## Next Steps

- [Installation](./installation.md) - Getting started
- [Basic Usage](./basic-usage.md) - Your first queries
- [Performance](../performance/) - Optimization tips
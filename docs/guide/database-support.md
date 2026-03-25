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
* DuckDB

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

### Array Support

Array types are supported on a per-database basis:

**PostgreSQL and DuckDB:**
- Full support for `List<T>` parameters and result mapping
- Supported element types: Int, Long, Short, Float, Double, Boolean, String, BigDecimal
- PostgreSQL also supports UUID element type
- Arrays can be null, contain null elements, or be empty (on result mapping)
- Example: `WHERE id = ANY(:ids)` with `"ids" to listOf(1, 2, 3)`

**MySQL, SQLite, Oracle, MSSQL:**
- Array types are not supported
- Passing a List/Array parameter will throw `KapperUnsupportedOperationException`

See [Array Types](./mapping.md#array-types) in the mapping guide for details and examples.

## Testing

For testing, consider using:
- **SQLite** for in-memory or file-based tests  
- **Testcontainers** for production-like integration tests

## Next Steps

- [Installation](./installation.md) - Getting started
- [Basic Usage](./basic-usage.md) - Your first queries
- [Performance](../performance/) - Optimization tips
# Kapper vs Ktorm

A detailed comparison between Kapper and Ktorm, both Kotlin-focused database libraries.

> **📁 Source Code**: View complete comparison examples in the [comparison](https://github.com/driessamyn/kapper/tree/main/examples/kotlin-example/src/test/kotlin/net/samyn/kapper/example/kotlin/comparison) directory.

## Overview

| Aspect             | Kapper                 | Ktorm                 |
|--------------------|------------------------|-----------------------|
| **Philosophy**     | SQL-first, lightweight | DSL-first, type-safe  |
| **API Style**      | Function-based         | Object-oriented + DSL |
| **SQL Control**    | Raw SQL preferred      | DSL generates SQL     |
| **Type Safety**    | Compile-time mapping   | Compile-time DSL      |
| **Learning Curve** | Minimal                | Moderate              |

## Basic Operations

### Kapper
```kotlin
data class User(val id: Long?, val name: String, val email: String)

// Direct SQL queries
val users = connection.query<User>("SELECT * FROM users WHERE active = true")
val user = connection.querySingle<User>("SELECT * FROM users WHERE id = ?", 1)

// Simple inserts
val userId = connection.execute(
    "INSERT INTO users (name, email) VALUES (?, ?) RETURNING id",
    "Alice", "alice@example.com"
)
```

### Ktorm
```kotlin
// Table definition required
object Users : Table<User>("users") {
    val id = long("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
    val email = varchar("email").bindTo { it.email }
    val active = boolean("active").bindTo { it.active }
}

data class User(val id: Long?, val name: String, val email: String, val active: Boolean)

// DSL queries
val users = database.from(Users).select().where { Users.active eq true }.map { Users.createEntity(it) }
val user = database.from(Users).select().where { Users.id eq 1 }.map { Users.createEntity(it) }.firstOrNull()

// DSL inserts
val userId = database.insert(Users) {
    set(it.name, "Alice")
    set(it.email, "alice@example.com")
    set(it.active, true)
}
```

## Complex Queries

### Kapper - Raw SQL
```kotlin
data class UserStats(
    val userId: Long,
    val userName: String,
    val postCount: Int,
    val avgRating: Double
)

val stats = connection.query<UserStats>("""
    SELECT 
        u.id as userId,
        u.name as userName,
        COUNT(p.id) as postCount,
        COALESCE(AVG(p.rating), 0) as avgRating
    FROM users u
    LEFT JOIN posts p ON u.id = p.user_id
    WHERE u.created_at > ?
    GROUP BY u.id, u.name
    HAVING COUNT(p.id) > 5
    ORDER BY avgRating DESC
    LIMIT ?
""", cutoffDate, limit)
```

### Ktorm - Type-safe DSL
```kotlin
// Requires additional table definitions
object Posts : Table<Post>("posts") {
    val id = long("id").primaryKey()
    val userId = long("user_id")
    val rating = double("rating")
    val createdAt = timestamp("created_at")
}

// Complex DSL query
val stats = database
    .from(Users)
    .leftJoin(Posts, on = Users.id eq Posts.userId)
    .select(
        Users.id,
        Users.name,
        count(Posts.id).aliased("postCount"),
        avg(Posts.rating).aliased("avgRating")
    )
    .where { Users.createdAt greater cutoffDate }
    .groupBy(Users.id, Users.name)
    .having { count(Posts.id) greater 5 }
    .orderBy(avg(Posts.rating).desc())
    .limit(limit)
    .map { row ->
        UserStats(
            userId = row[Users.id]!!,
            userName = row[Users.name]!!,
            postCount = row.getInt("postCount"),
            avgRating = row.getDouble("avgRating")
        )
    }
```

## Configuration and Setup

### Kapper
```kotlin
// Minimal setup with any DataSource
val dataSource = HikariDataSource().apply {
    jdbcUrl = "jdbc:postgresql://localhost/mydb"
    username = "user"
    password = "password"
}

// Ready to use immediately
val users = dataSource.connection.use { connection ->
    connection.query<User>("SELECT * FROM users")
}
```

### Ktorm
```kotlin
// Requires Database instance configuration
val database = Database.connect(
    url = "jdbc:postgresql://localhost/mydb",
    driver = "org.postgresql.Driver",
    user = "user",
    password = "password"
)

// Table definitions are required before use
object Users : Table<User>("users") {
    val id = long("id").primaryKey()
    val name = varchar("name")
    val email = varchar("email")
}

val users = database.from(Users).select().map { Users.createEntity(it) }
```

## Next Steps

- [Kapper vs Hibernate](./hibernate.md) - Comparison with traditional JVM ORMs
- [Basic CRUD Operations](../kotlin/basic-crud.md) - Get started with Kapper
- [Performance Benchmarks](../../performance/) - See how they compare in practice

## Source Examples

- **Kapper implementation**: [KapperExample.kt](https://github.com/driessamyn/kapper/blob/main/examples/kotlin-example/src/test/kotlin/net/samyn/kapper/example/kotlin/comparison/kapper/KapperExample.kt)
- **Ktorm implementation**: [KtormExample.kt](https://github.com/driessamyn/kapper/blob/main/examples/kotlin-example/src/test/kotlin/net/samyn/kapper/example/kotlin/comparison/ktorm/KtormExample.kt)
- **Ktorm repository**: [SuperHeroRepository.kt](https://github.com/driessamyn/kapper/blob/main/examples/kotlin-example/src/main/kotlin/net/samyn/kapper/example/kotlin/ktorm/SuperHeroRepository.kt)
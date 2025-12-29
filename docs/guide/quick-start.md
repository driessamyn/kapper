# Quick Start

This guide will get you up and running with Kapper in just a few minutes.

## Setup Database Connection

First, set up your database connection. We'll use [HikariCP](https://github.com/brettwooldridge/HikariCP) for connection pooling:

```kotlin
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

val dataSource = HikariDataSource(HikariConfig().apply {
    jdbcUrl = "jdbc:postgresql://localhost:5432/mydatabase"
    username = "myuser"
    password = "mypassword"
})
```

## Define Your Data Class

Create a simple data class* that matches your database table:

```kotlin
import java.util.UUID

data class SuperHero(
    val id: UUID, 
    val name: String, 
    val email: String? = null, 
    val age: Int? = null
)
```

*Java record classes are also [supported](mapping.md#java-records-support).

## Create Your Table

```sql
CREATE TABLE super_heroes (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    age INT
);
```

## Basic Operations

### Query Data

```kotlin
import net.samyn.kapper.query

// Get all heroes
val heroes = dataSource.connection.use {
    it.query<SuperHero>("SELECT * FROM super_heroes")
}

// Query with parameters
val olderHeroes = dataSource.connection.use {
    it.query<SuperHero>(
        "SELECT * FROM super_heroes WHERE age > :age",
        "age" to 30
    )
}

// Find a single hero
val batman = dataSource.connection.use {
    it.querySingle<SuperHero>(
        "SELECT * FROM super_heroes WHERE name = :name",
        "name" to "Batman"
    )
}
```

### Insert Data

```kotlin
import net.samyn.kapper.execute

dataSource.connection.use {
    it.execute(
        """
        INSERT INTO super_heroes(id, name, email, age) 
        VALUES (:id, :name, :email, :age)
        """,
        "id" to UUID.randomUUID(),
        "name" to "Wonder Woman",
        "email" to "wonder@dc.com",
        "age" to 3000
    )
}
```

### Update Data

```kotlin
dataSource.connection.use {
    it.execute(
        "UPDATE super_heroes SET age = :age WHERE name = :name",
        "age" to 86,
        "name" to "Batman"
    )
}
```

### Execute with DTOs

You can also pass DTOs directly to execute functions:

```kotlin
val hero = SuperHero(UUID.randomUUID(), "Flash", "flash@dc.com", 28)

dataSource.connection.use {
    it.execute(
        "INSERT INTO super_heroes(id, name, email, age) VALUES(:id, :name, :email, :age)",
        hero,
        "id" to SuperHero::id,
        "name" to SuperHero::name,
        "email" to SuperHero::email,
        "age" to SuperHero::age
    )
}
```

## Transactions

```kotlin
import net.samyn.kapper.withTransaction

dataSource.withTransaction { connection ->
    // All operations in this block are transactional
    connection.execute(
        "INSERT INTO super_heroes(id, name, email, age) VALUES(:id, :name, :email, :age)",
        "id" to UUID.randomUUID(),
        "name" to "Spider-Man",
        "email" to "spidey@marvel.com",
        "age" to 16
    )
    
    connection.execute(
        "UPDATE super_heroes SET email = :email WHERE name = :name",
        "email" to "peter.parker@marvel.com",
        "name" to "Spider-Man"
    )
}
```

## That's It!

You're now ready to use Kapper in your application. For more other examples and more advanced usage:

- Explore [Examples](/examples/) for real-world patterns
- Learn about [Coroutines Support](./coroutines.md) for async operations
- Check out [Custom Mappers](./custom-mappers.md) for complex mapping scenarios
- See [Performance Tuning](./performance-tuning.md) for optimization tips
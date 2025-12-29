# Basic CRUD Operations (Kotlin)

This example demonstrates the fundamental Create, Read, Update, and Delete operations using Kapper with Kotlin.

> **📁 Source Code**: View the complete runnable examples in the [kotlin-example](https://github.com/driessamyn/kapper/tree/main/examples/kotlin-example/src/test/kotlin/net/samyn/kapper/example/kotlin/simple) directory.

## Setup

The examples use a superhero-themed schema:

```kotlin
data class SuperHero(
    val id: UUID,
    val name: String,
    val email: String? = null,
    val age: Int? = null,
)

data class Villain(
    val id: UUID,
    val name: String,
)

data class SuperHeroBattle(
    val superhero: String,
    val villain: String,
    val date: LocalDateTime,
)
```

## Create (Insert)

```kotlin
// Insert a single superhero
connection.execute(
    """
    INSERT INTO super_heroes(id, name, email, age) 
    VALUES (:id, :name, :email, :age)
    """.trimIndent(),
    "id" to UUID.randomUUID(),
    "name" to "Batman",
    "email" to "batman@dc.com",
    "age" to 85,
)
```

## Read (Select)

```kotlin
// Query all superheroes
val heroes = connection.query<SuperHero>("SELECT * FROM super_heroes")

// Query with parameters
val olderHeroes = connection.query<SuperHero>(
    "SELECT * FROM super_heroes WHERE age > :age",
    "age" to 80,
)

// Query single superhero
val batman = connection.querySingle<SuperHero>(
    "SELECT * FROM super_heroes WHERE name = :name",
    "name" to "Batman",
)
```

## Update

```kotlin
// Update superhero age
connection.execute(
    """
    UPDATE super_heroes
    SET age = 86
    WHERE name = :name
    """.trimIndent(),
    "name" to "Batman",
)
```

## Delete

```kotlin
// Delete superhero
connection.execute(
    """
    DELETE FROM super_heroes
    WHERE name = :name
    """.trimIndent(),
    "name" to "Batman",
)
```

## Custom Mapping

You can use custom mappers for complex result processing:

```kotlin
// Custom mapping to Pair
val heroAges = connection.query<Pair<String, Int>>(
    "SELECT * FROM super_heroes WHERE age > :age",
    { resultSet, fields ->
        Pair(
            resultSet.getString(fields["name"]!!.columnIndex),
            resultSet.getInt(fields["age"]!!.columnIndex),
        )
    },
    "age" to 80,
)
```

## Complete Example

```kotlin
fun main() {
    val dataSource = HikariDataSource().apply {
        jdbcUrl = "jdbc:postgresql://localhost:5432/example"
        username = "user"
        password = "password"
    }
    
    dataSource.connection.use { connection ->
        // Insert superheroes
        connection.execute(
            """
            INSERT INTO super_heroes(id, name, email, age) 
            VALUES (:id, :name, :email, :age)
            """.trimIndent(),
            "id" to UUID.randomUUID(),
            "name" to "Superman",
            "email" to "superman@dc.com",
            "age" to 86,
        )
        
        // Query with parameters
        val olderHeroes = connection.query<SuperHero>(
            "SELECT * FROM super_heroes WHERE age > :age",
            "age" to 80,
        )
        println("Older heroes: $olderHeroes")
        
        // Update
        connection.execute(
            """
            UPDATE super_heroes
            SET age = 87
            WHERE name = :name
            """.trimIndent(),
            "name" to "Superman",
        )
        
        // Query single result
        val superman = connection.querySingle<SuperHero>(
            "SELECT * FROM super_heroes WHERE name = :name",
            "name" to "Superman",
        )
        println("Updated Superman: $superman")
    }
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

## Next Steps

- [Advanced Queries](./advanced-queries.md) - Complex SQL operations
- [Transaction Handling](./transactions.md) - Managing database transactions
- [Coroutines & Flow](./coroutines.md) - Async operations with coroutines

## Source Examples

- **Complete test examples**: [Execute.kt](https://github.com/driessamyn/kapper/blob/main/examples/kotlin-example/src/test/kotlin/net/samyn/kapper/example/kotlin/simple/Execute.kt)
- **DTO examples**: [ExecuteWithDto.kt](https://github.com/driessamyn/kapper/blob/main/examples/kotlin-example/src/test/kotlin/net/samyn/kapper/example/kotlin/simple/ExecuteWithDto.kt)
- **Query examples**: [Query.kt](https://github.com/driessamyn/kapper/blob/main/examples/kotlin-example/src/test/kotlin/net/samyn/kapper/example/kotlin/simple/Query.kt)
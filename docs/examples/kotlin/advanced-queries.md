# Advanced Queries (Kotlin)

Explore complex SQL operations and advanced querying techniques with Kapper.

> **📁 Source Code**: View complete examples in the [kotlin-example](https://github.com/driessamyn/kapper/tree/main/examples/kotlin-example/src/test/kotlin/net/samyn/kapper/example/kotlin) directory.

## Joins and Relations

### Finding Battles with Joins

```kotlin
data class SuperHeroBattle(
    val superhero: String,
    val villain: String,
    val date: LocalDateTime,
)

// Join across multiple tables - from SuperHeroRepository.kt
fun findBattles(superHeroName: String): List<SuperHeroBattle> {
    return dataSource.connection.use { connection ->
        connection.query<SuperHeroBattle>(
            """
            SELECT s.name as superhero, v.name as villain, b.battle_date as date
            FROM super_heroes as s
            INNER JOIN battles as b on s.id = b.super_hero_id
            INNER JOIN villains as v on v.id = b.villain_id
            WHERE s.name = :name 
            """.trimIndent(),
            "name" to superHeroName,
        )
    }
}
```

## Window Functions and Complex Analysis

### Movie Analysis with Window Functions

```kotlin
data class PopularMovie(
    val title: String,
    val grossed: Long,
    val comparedToAnnualAverage: Double,
    val allTimeRanking: Int,
)

// Complex query with window functions - from SuperHeroRepository.kt
fun findPopularMovies(): List<PopularMovie> {
    return dataSource.connection.use { connection ->
        var allTimeRank = 1
        connection.query(
            """
            SELECT
                title,
                release_date, 
                gross_worldwide, 
                AVG(gross_worldwide) OVER() AS total_average_gross,
                AVG(gross_worldwide) OVER(PARTITION BY EXTRACT(YEAR FROM release_date)) AS average_annual_gross
            FROM movies 
            ORDER BY gross_worldwide DESC
            LIMIT 3
            """.trimIndent(),
            { rs, _ ->
                val gross = rs.getLong("gross_worldwide")
                val annualAvgGross = rs.getInt("average_annual_gross")
                PopularMovie(
                    rs.getString("title"),
                    gross,
                    gross / annualAvgGross.toDouble(),
                    allTimeRank++,
                )
            },
        )
    }
}
```

## Custom Mappers

### Manual Result Mapping

```kotlin
// Custom mapping to Map - from KapperExample.kt
val villainAsMap = dataSource.connection.use { connection ->
    connection.querySingle<Map<String, *>>(
        "SELECT * FROM villains WHERE name = :name",
        { resultSet, fields ->
            mapOf(
                "id" to resultSet.getString("id"),
                "name" to resultSet.getString("name"),
            )
        },
        "name" to "Joker",
    )
}
```

### Custom Mapping with Pairs

```kotlin
// From Query.kt test
val heroAges = connection.query<Pair<String, *>>(
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

## Repository Pattern

### Complete Repository Implementation

```kotlin
// From SuperHeroRepository.kt
class SuperHeroRepository(private val dataSource: DataSource) {
    
    // List all superheroes
    fun list(): List<SuperHero> =
        dataSource.connection.use {
            it.query<SuperHero>("SELECT * FROM super_heroes")
        }

    // Find a superhero by ID
    fun findById(id: UUID): SuperHero? =
        dataSource.connection.use {
            it.querySingle<SuperHero>(
                "SELECT * FROM super_heroes WHERE id = :id",
                "id" to id,
            )
        }

    // Find a superhero by name
    fun findByName(name: String): SuperHero? =
        dataSource.connection.use {
            it.querySingle<SuperHero>(
                "SELECT * FROM super_heroes WHERE name = :name",
                "name" to name,
            )
        }

    // Insert a new superhero
    fun insertHero(superHero: SuperHero): Long =
        dataSource.connection.use {
            it.execute(
                """
                INSERT INTO super_heroes(id, name, email, age) 
                VALUES (:id, :name, :email, :age)
                """.trimIndent(),
                "id" to superHero.id,
                "name" to superHero.name,
                "email" to superHero.email,
                "age" to superHero.age,
            )
        }
}
```

## Transaction Patterns

### Complex Transactional Operations

```kotlin
// From SuperHeroRepository.kt - insertBattle method
fun insertBattle(
    superHero: SuperHero,
    villain: Villain,
    date: LocalDateTime,
) = dataSource.withTransaction { connection ->
    
    // Insert superhero if not exists
    connection.execute(
        """
        INSERT INTO super_heroes(id, name, email, age) 
        VALUES (:id, :name, :email, :age)
        ${ignoreConflict("id")}
        """.trimIndent(),
        "id" to superHero.id,
        "name" to superHero.name,
        "email" to superHero.email,
        "age" to superHero.age,
    )
    
    // Insert villain if not exists
    connection.execute(
        """
        INSERT INTO villains(id, name) 
        VALUES (:id, :name)
        ${ignoreConflict("id")}
        """.trimIndent(),
        "id" to villain.id,
        "name" to villain.name,
    )
    
    // Insert the battle record
    connection.execute(
        """
        INSERT INTO battles(super_hero_id, villain_id, battle_date, updated_ts)
        VALUES (:super_hero_id, :villain_id, :date, NOW())
        """.trimIndent(),
        "super_hero_id" to superHero.id,
        "villain_id" to villain.id,
        "date" to date,
    )
}
```

## Database-Specific Features

### Cross-Database Compatibility

```kotlin
// From SuperHeroRepository.kt
class SuperHeroRepository(private val dataSource: DataSource) {
    private val dbType: DbType

    init {
        dbType = dataSource.connection.use { connection ->
            when (connection.metaData.databaseProductName) {
                "MySQL" -> DbType.MYSQL
                else -> DbType.POSTGRESQL
            }
        }
    }

    enum class DbType {
        POSTGRESQL,
        MYSQL,
    }

    // Database-specific conflict resolution
    private fun ignoreConflict(updateCol: String) =
        if (DbType.MYSQL == dbType) {
            "ON DUPLICATE KEY UPDATE $updateCol=$updateCol"
        } else {
            "ON CONFLICT DO NOTHING"
        }
}
```

## Next Steps

- [Transaction Handling](./transactions.md) - Advanced transaction patterns
- [Coroutines & Flow](./coroutines.md) - Async operations
- [Performance Guide](../../guide/performance-tuning.md) - Optimization strategies

## Source Examples

- **Entity definitions**: [Entities.kt](https://github.com/driessamyn/kapper/blob/main/examples/kotlin-example/src/main/kotlin/net/samyn/kapper/example/kotlin/Entities.kt)
- **Repository patterns**: [SuperHeroRepository.kt](https://github.com/driessamyn/kapper/blob/main/examples/kotlin-example/src/main/kotlin/net/samyn/kapper/example/kotlin/kapper/SuperHeroRepository.kt)
- **Advanced query tests**: [KapperExample.kt](https://github.com/driessamyn/kapper/blob/main/examples/kotlin-example/src/test/kotlin/net/samyn/kapper/example/kotlin/comparison/kapper/KapperExample.kt)
- **Custom mapping examples**: [Query.kt](https://github.com/driessamyn/kapper/blob/main/examples/kotlin-example/src/test/kotlin/net/samyn/kapper/example/kotlin/simple/Query.kt)
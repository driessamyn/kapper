# kapper-examples

KapperExample usage of the [Kapper](https://github.com/driessamyn/kapper) library.



## Overview

This project demonstrates how to use the Kapper library with Kotlin and SQL databases. 
It includes examples of basic CRUD operations, complex queries, and transaction handling.

## Quick Start

1. Add Kapper dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("net.samyn:kapper:<version>")
}
```

2. Create a basic repository:

```kotlin
class SuperHeroRepository(private val dataSource: DataSource) {
    fun findHeroByName(name: String) = dataSource.connection.use {
        it.querySingle<SuperHero>(
            "SELECT * FROM super_heroes WHERE name = :name",
            "name" to name
        )
    }
}
```

3. Use the repository:

```kotlin
val repository = SuperHeroRepository(dataSource)
val hero = repository.findHeroByName("Superman")
println("Found hero: ${hero.name}, age: ${hero.age}")
```

## Database Schema

The examples use the following schema:

```sql
CREATE TABLE super_heroes (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    age INT
);

CREATE TABLE villains (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE battles (
    super_hero_id UUID REFERENCES super_heroes(id),
    villain_id UUID REFERENCES villains(id),
    battle_date TIMESTAMP NOT NULL,
    updated_ts TIMESTAMP NOT NULL,
    PRIMARY KEY (super_hero_id, villain_id, battle_date)
);
```

Separate scripts are provided for [PostgreSQL](./db/postgresql.sql) and [MySQL](./db/mysql.sql).

## Examples

### Kotlin

[kotlin-example](./kotlin-example) contains Kotlin examples using Kapper.

[SuperHeroRepository.kt](./src/main/kotlin/net/samyn/kapper/example/kotlin/SuperHeroRepository.kt) has an example repository class using the kapper API:

#### Simple SELECT query

```kotlin
fun list(): List<SuperHero> = dataSource.connection.use {
    it.query<SuperHero>("SELECT * FROM super_heroes")
}
```

Executes a simple SELECT query on the `super_heroes` table and maps the results to the `SuperHero` data class.

#### Find by ID

```kotlin
fun findById(id: UUID) = dataSource.connection.use {
    it.querySingle<SuperHero>(
        "SELECT * FROM super_heroes WHERE id = :id", "id" to id)
}

fun findByName(name: String) = dataSource.connection.use {
    it.querySingle<SuperHero>(
        "SELECT * FROM super_heroes WHERE name = :name", "name" to name)
}
```

The above examples select a single row by using the ID or name and also automatically maps to the `SuperHero` class.

#### Join query

```kotlin
fun findBattles(superHeroName: String) = dataSource.connection.use {
    it.query<SuperHeroBattle>(
        """
         SELECT s.name as superhero, v.name as villain, b.battle_date as date
         FROM super_heroes as s
         INNER JOIN battles as b on s.id = b.super_hero_id
         INNER JOIN villains as v on v.id = b.villain_id
         WHERE s.name = :name 
    """.trimIndent(), "name" to superHeroName
    )
}
```

This example uses a SQL query that has multiple joins and auto-maps the result to the `SuperHeroBattle` data class.

This is an example to illustrate how Kapper does not interfere with the SQL query language, and it does not keep a strict mapping between the relational and the object model.
Instead, it simply executes the query that is provided and maps to the object that is specified.
This avoids needing to learn another language or API to facilitate JOINs, or other complex queries, or wasteful multiple queries and code based joining of data that is often seen when people use ORMs "out the box".

You can also see here how data from the `super_heroes` table is used by multiple data classes.
Kapper is not opinionated about the mapping between the relational and object model.

#### Simple INSERT

```kotlin
fun insertHero(superHero: SuperHero) = dataSource.connection.use {
    it.execute(
        """
         INSERT INTO super_heroes(id, name, email, age) 
         VALUES (:id, :name, :email, :age)
    """.trimIndent(),
        "id" to superHero.id,
        "name" to superHero.name,
        "email" to superHero.email,
        "age" to superHero.age
    )
}
```

The above is a simple example of an insert.
Kapper currently doesn't support passing an object into teh execute function, instead it requires each parameter to be specified individually.
This makes it very easy to understand the expected behaviour (for example, when ID values are generated in code vs the DB).
However, if there is sufficient demand, an additional API will be introduced.

#### Transactional INSERT (IF NOT EXIST)

```kotlin
fun insertBattle(
    superHero: SuperHero,
    villain: Villain,
    date: LocalDateTime,
) = dataSource.connection.use {
    try {
        it.autoCommit = false
        it.execute(
            """
             INSERT INTO super_heroes(id, name, email, age) 
             VALUES (:id, :name, :email, :age)
             ${ignoreConflict("id")}
              """.trimIndent(),
            "id" to superHero.id,
            "name" to superHero.name,
            "email" to superHero.email,
            "age" to superHero.age
        )
        it.execute(
            """
             INSERT INTO villains(id, name) 
             VALUES (:id, :name)
             ${ignoreConflict("id")}
              """.trimIndent(),
            "id" to villain.id,
            "name" to villain.name,
        )
        it.execute(
            """
             INSERT INTO battles(super_hero_id, villain_id, battle_date, updated_ts)
             VALUES (:super_hero_id, :villain_id, :date, NOW())
              """.trimIndent(),
            "super_hero_id" to superHero.id,
            "villain_id" to villain.id,
            "date" to date,
        )
        it.commit()
    } catch (ex: SQLException) {
        it.rollback()
        throw ex
    }
}

// example of something non-trivial that impacts the Query
//  in this case, we want to support both mySQL and postgreSQL in their
//  own way of doing an insert and ignoring the duplicate.
private fun ignoreConflict(updateCol: String) =
    if(DbType.MYSQL == dbType) {
        "ON DUPLICATE KEY UPDATE $updateCol=$updateCol"
    } else {
        "ON CONFLICT DO NOTHING"
    }
```

In the above example, a DB transaction is used to ensure the inserts are atomic.
As you can see, Kapper doesn't interfere with the transaction handling, in fact it is completely unaware of the existence of the transaction.
This is because Kapper is simply an extension of the JDBC DB Connection rather than wrapping existing APIs.

Secondly, the example shows how it is possible to use DB native functionality, in this case to support an insert only if the data does not exist.
In PostgreSQL, this is supported by the `ON CONFLICT DO NOTHING` hint and in mySQL, we use `ON DUPLICATE KEY UPDATE`.

Because Kapper uses SQL queries are provided, it never needs to "catch up" with specific DB features.
Users or Kapper are in complete control.
This is further illustrated by the example below which uses a complex Window query and custom mapper.

#### A more complex example

[SuperHeroRepository](./kotlin-example/src/main/kotlin/net/samyn/kapper/example/kotlin/kapper/SuperHeroRepository.kt) contains an additional, more complex, query, which serves as an example of how complex SQL queries can be used with Kapper without the need for a complex or new API.

In this example, we use a Window function to calculate averages for a year, and map this to a new `PopularMovie` data class.

```kotlin
data class PopularMovie(
    val title: String,
    val grossed: Long,
    val comparedToAnnualAverage: Double)

val movies = ds.connection.use { conn ->
        conn.query("""
            SELECT 
             title,
             release_date, 
             gross_worldwide, 
             AVG(gross_worldwide) OVER() AS total_average_gross,
             AVG(gross_worldwide) OVER(PARTITION BY EXTRACT(YEAR FROM release_date)) AS average_annual_gross
            FROM movies 
            ORDER BY gross_worldwide DESC
        """.trimIndent(),
            { rs, _ ->
                val gross = rs.getLong("gross_worldwide")
                val annualAvgGross = rs.getInt("average_annual_gross")
                PopularMovie(
                    rs.getString("title"),
                    gross,
                    annualAvgGross.toDouble()/gross
                )
            })
    }
    
    movies
        .filter { it.comparedToAnnualAverage > 2.0 }
        .sortedByDescending { it.comparedToAnnualAverage }
        .forEach {
            println("${it.title} took ${it.grossed}, or more than double the annual average.")
            it.comparedToAnnualAverage.shouldBeGreaterThan(2.0)
        }
```

#### Coroutine support

Kapper supports coroutines with the inclusion of the `kapper-coroutines` module:

```kotlin
dependencies {
    implementation("net.samyn:kapper-coroutines:<version>")
}
```  

This module provides an extension function `withConnection` on the `DataSource` object, optionally allowing you to specify a `Dispatcher`.
If no `Dispatcher` is provided, the default `Dispatchers.IO` is used.

For example:

```kotlin
suspend fun listHeroes(): List<SuperHero> =
    dataSource.withConnection {
        // Kapper query runs on Dispatchers.IO
        it.query<SuperHero>("SELECT * FROM super_heroes")
    }
```  

Using the function above like so:

```kotlin
runBlocking {
    val selectJob =
        async {
            println("[${Thread.currentThread().name}] Starting to select heroes.")
            val heroes = it.query<SuperHero>("SELECT * FROM super_heroes")
            println("\n[${Thread.currentThread().name}] Finished selecting heroes")
            heroes
        }
    val logJob =
        launch {
            // print . until the insertJob has completed.
            println("[${Thread.currentThread().name}] ")
            while (selectJob.isActive) {
                delay(100)
                println(".")
            }
        }
    println("Selected ${selectJob.await().joinToString { it.name }}")
    logJob.join()
}
```

Would output, for example:

```text
[Test worker @coroutine#5] Starting to select heroes.
[Test worker @coroutine#6] ...........
[Test worker @coroutine#5] Finished selecting heroes
Selected Superman, Batman, Wonder Woman, Spider-Man, Iron Man, Captain America, Thor
.
```

See [NonBlockingExampleTest](./kotlin-example/src/main/kotlin/net/samyn/kapper/example/kotlin/kapper/NonBlockingSuperHeroService.kt) and [CoroutineExample](./kotlin-example/src/test/kotlin/CoroutineExample.kt) for more examples.

### Java Examples

Kapper also supports Java, including auto-mapping to Java records.

[java-example](./java-example) contains Java examples using Kapper.

See [SuperHeroRepository.java](./java-example/src/main/java/net/samyn/kapper/example/java/kapper/SuperHeroRecordRepository.java) for an example repository class using the Kapper API in Java.

Using the following record class:

```java
public record SuperHeroRecord(UUID id, String name, String email, int age) {}
```

#### Simple SELECT query

```java
public List<SuperHeroRecord> list() throws SQLException {
    try (Connection conn = dataSource.getConnection()) {
        return  kapper.query(SuperHeroRecord.class, conn, "SELECT * FROM super_heroes", Map.of());
    }
}
```

Executes a simple SELECT query on the `super_heroes` table and maps the results to the `SuperHeroRecord` class.

#### Find by ID

```java
public SuperHeroRecord findById(UUID id) throws SQLException {
    try (Connection conn = dataSource.getConnection()) {
        return kapper.querySingle(SuperHeroRecord.class, conn, "SELECT * FROM super_heroes WHERE id = :id", Map.of("id", id));
    }
}

public SuperHeroRecord findByName(String name) throws SQLException {
    try (Connection conn = dataSource.getConnection()) {
        return kapper.querySingle(SuperHeroRecord.class, conn, "SELECT * FROM super_heroes WHERE name = :name", Map.of("name", name));
    }
}
```

The above examples select a single row by using the ID or name and also automatically maps to the `SuperHeroRecord` class.

#### Join query

```java
public List<SuperHeroBattleRecord> findBattles(String superHeroName) throws SQLException {
    try (Connection conn = dataSource.getConnection()) {
        return kapper.query(SuperHeroBattleRecord.class,
                conn,
                """
                        SELECT s.name as superhero, v.name as villain, b.battle_date as date
                        FROM super_heroes as s
                        INNER JOIN battles as b on s.id = b.super_hero_id
                        INNER JOIN villains as v on v.id = b.villain_id
                        WHERE s.name = :name
                        """,
                Map.of("name", superHeroName)
        );
    }
}
```

This example uses a SQL query that has multiple joins and auto-maps the result to the `SuperHeroBattleRecord` class.

This is an example to illustrate how Kapper does not interfere with the SQL query language, and it does not keep a strict mapping between the relational and the object model.
Instead, it simply executes the query that is provided and maps to the object that is specified.
This avoids needing to learn another language or API to facilitate JOINs, or other complex queries, or wasteful multiple queries and code based joining of data that is often seen when people use ORMs "out the box".

You can also see here how data from the super_heroes table is used by multiple data classes. Kapper is not opinionated about the mapping between the relational and object model.

## Testing

The project uses [TestContainers](https://testcontainers.com/) for integration testing:

```kotlin
class SuperHeroRepositoryTest {
    private lateinit var postgres: PostgreSQLContainer<Nothing>
    private lateinit var repository: SuperHeroRepository

    @BeforeEach
    fun setup() {
        postgres = PostgreSQLContainer<Nothing>("postgres:14-alpine").apply {
            withDatabaseName("heroes")
            withUsername("test")
            withPassword("test")
            start()
        }
        
        val dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
        })
        
        repository = SuperHeroRepository(dataSource)
    }

    @Test
    fun `should find hero by name`() {
        val hero = repository.findByName("Superman")
        assertNotNull(hero)
        assertEquals("Superman", hero.name)
    }
}
```

## Comparison with ORMs

### Hibernate

The _Godfather_ of ORMs:

- [Example code](kotlin-example/src/main/kotlin/net/samyn/kapper/example/hibernate/HibernateExample.kt)
- [Example usage](kotlin-example/src/test/kotlin/net/samyn/kapper/example/hibernate/HibernateExampleTest.kt)

### Ktorm

The _new kid on the block:

- [Example code](kotlin-example/src/main/kotlin/net/samyn/kapper/example/ktorm/KtormExample.kt)
- [Example usage](kotlin-example/src/test/kotlin/net/samyn/kapper/example/ktorm/KtormExampleTest.kt)

## Performance Considerations

- Use connection pooling (e.g., HikariCP) for optimal performance
- Consider batch operations for bulk inserts/updates
- Use appropriate indexes on your database
- Monitor query execution plans
- Cache frequently accessed data when appropriate

## Contributing

1. Fork the repository
2. Create a feature branch
3. Submit a pull request

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details.

---

For more information, visit [Kapper](https://github.com/driessamyn/kapper).
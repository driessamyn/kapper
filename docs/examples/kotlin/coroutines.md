# Coroutines & Flow (Kotlin)

Learn how to use Kapper with Kotlin coroutines and Flow for reactive programming.

> **📁 Source Code**: View complete examples in the [kotlin-example coroutines](https://github.com/driessamyn/kapper/tree/main/examples/kotlin-example/src/test/kotlin/net/samyn/kapper/example/kotlin/coroutines) directory.

## Setup

Add the coroutines module:

```kotlin
dependencies {
    implementation("net.samyn:kapper-coroutines:latest")
}
```

## Basic Coroutine Operations

### NonBlockingSuperHeroService

```kotlin
// From NonBlockingSuperHeroService.kt
class NonBlockingSuperHeroService(private val dataSource: DataSource) {
    
    // Equivalent of the list function in SuperHeroRepository but using coroutines
    // A sleep is added to query to illustrate the non-blocking nature of the function
    suspend fun listSlowly(): List<SuperHero> =
        dataSource.withConnection {
            val heroes = it.query<SuperHero>("SELECT * FROM super_heroes")
            sleep(1000) // sleeping the IO dispatcher thread before returning ... very very bad!
            heroes
        }

    // Very inefficient (bad) way of inserting heroes, sleeping between each insert,
    // to illustrates the non-blocking nature of the function and the use of transactions.
    suspend fun insertSlowly(heroes: List<SuperHero>) {
        dataSource.withTransaction { conn ->
            heroes.forEach { hero ->
                conn.execute(
                    "INSERT INTO super_heroes (id, name, email) VALUES (:id, :name, :email)",
                    "id" to hero.id,
                    "name" to hero.name,
                    "email" to hero.email,
                )
                sleep(10) // sleeping the IO dispatcher thread ... very very bad!
            }
        }
    }
}
```

## Real Coroutine Examples

### Insert Heroes with Progress Tracking

```kotlin
// From CoroutineExample.kt
@Test
fun `insert heroes`() {
    val heroes = (0..100).map {
        SuperHero(UUID.randomUUID(), "Superman - $it", "super-$it@dc.com", 85)
    }
    val log = runBlocking {
        val sb = StringBuilder()
        val insertJob = launch {
            sb.appendLine("[${Thread.currentThread().name}] Starting to insert ${heroes.size} heroes.")
            service.insertSlowly(heroes)
            sb.appendLine("\n[${Thread.currentThread().name}] Finished inserting ${heroes.size} heroes")
        }
        val logJob = launch {
            // print . until the insertJob has completed.
            sb.append("[${Thread.currentThread().name}] ")
            while (insertJob.isActive) {
                delay(100)
                sb.append(".")
            }
        }
        logJob.join()
        sb.toString()
    }
    println(log)
}
```

### Concurrent Database Access

```kotlin
// From CoroutineExample.kt
@Test
fun `parallel connections`() {
    insertHeroes()
    runBlocking {
        val heroes = async {
            getDataSource(postgresql).withConnection {
                it.query<SuperHero>("SELECT * FROM super_heroes")
            }
        }
        // This creates a second connection!
        val villains = async {
            getDataSource(postgresql).withConnection {
                it.query<Villain>("SELECT * FROM villains")
            }
        }
        heroes.await().shouldNotBeEmpty()
        villains.await().shouldBeEmpty()
    }
}
```

### Async Query Operations

```kotlin
// From CoroutineExample.kt  
@Test
fun `select heroes`() {
    insertHeroes()
    val log = runBlocking {
        val sb = StringBuilder()
        val selectJob = async {
            sb.appendLine("[${Thread.currentThread().name}] Starting to select heroes.")
            val heroes = service.listSlowly()
            sb.appendLine("\n[${Thread.currentThread().name}] Finished selecting heroes")
            heroes
        }
        val logJob = launch {
            // print . until the insertJob has completed.
            sb.append("[${Thread.currentThread().name}] ")
            while (selectJob.isActive) {
                delay(100)
                sb.append(".")
            }
        }
        sb.appendLine("Selected ${selectJob.await().joinToString { it.name }}")
        logJob.join()
        sb.toString()
    }
    println(log)
}
```

## Flow Integration

### Flow Example

```kotlin
// From FlowExample.kt
class FlowExample : DbBase() {
    
    @Test
    fun `flow example`() {
        // Insert test data
        runBlocking {
            (0..10).map {
                SuperHero(UUID.randomUUID(), "Batman - $it", "bat-$it@dc.com", 83)
            }.let { heroes ->
                NonBlockingSuperHeroService(getDataSource(postgresql)).insertSlowly(heroes)
            }
        }

        // Process with Flow
        runBlocking {
            flowOf(*Array(10) { "Batman - $it" })
                .map { name ->
                    getDataSource(postgresql).withConnection { connection ->
                        connection.querySingle<SuperHero>(
                            "SELECT * FROM super_heroes WHERE name = :name",
                            "name" to name
                        )
                    }
                }
                .collect { hero ->
                    hero?.let { println("Found hero: ${it.name}") }
                }
        }
    }
}
```

## Next Steps

- [Transaction Handling](./transactions.md) - Advanced transaction patterns
- [Performance Guide](../../guide/performance-tuning.md) - Optimization for async operations
- [Coroutines Guide](../../guide/coroutines.md) - Detailed coroutines documentation

## Source Examples

- **Coroutine operations**: [CoroutineExample.kt](https://github.com/driessamyn/kapper/blob/main/examples/kotlin-example/src/test/kotlin/net/samyn/kapper/example/kotlin/coroutines/CoroutineExample.kt)
- **Flow examples**: [FlowExample.kt](https://github.com/driessamyn/kapper/blob/main/examples/kotlin-example/src/test/kotlin/net/samyn/kapper/example/kotlin/coroutines/FlowExample.kt)
- **Async service**: [NonBlockingSuperHeroService.kt](https://github.com/driessamyn/kapper/blob/main/examples/kotlin-example/src/main/kotlin/net/samyn/kapper/example/kotlin/kapper/NonBlockingSuperHeroService.kt)
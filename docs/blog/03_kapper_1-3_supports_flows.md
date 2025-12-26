# Kapper 1.3 supports flows - more Kotlin goodness

Kapper, [Kotlin's most lightweight and idiomatic ORM library](https://dev.to/driessamyn/kapper-a-fresh-look-at-orms-for-kotlin-and-the-jvm-1ln5) brings more Kotlin goodness with support for Flows.
Kapper 1.3 supports queries returning Kotlin _[Flows](https://kotlinlang.org/docs/flow.html#flows)_.

## What are Flows?

Flows are a Kotlin API for asynchronous streams of data.
They are similar to [Rx Observables](https://reactivex.io/), but are simpler and more idiomatic to Kotlin.
They are a great fit for asynchronous data processing, particularly for database operations where results may be large or processing needs to happen incrementally.
This makes them a perfect addition to [Kapper's existing coroutine support](https://dev.to/driessamyn/coroutine-support-in-kapper-11-45h9).

## Kapper 1.3: Simple Flow Integration

As always with Kapper, the new API is simple and idiomatic to Kotlin and is provided as an extension function rather than a leaky abstraction which you may find in other libraries.

To make use of the new API, simply call the `queryAsFlow` extension function on a JDBC `Connection` instance.
The `queryAsFlow` function takes the same arguments as the _regular_, blocking, `query` function, but instead returns a `Flow` of the query results.

### Example: Basic Flow Query

```kotlin
 val query =
    datasource.withConnection {
        async {
            // SuperHero is a plain Kotlin dataclass
            it.queryAsFlow<SuperHero>("SELECT * FROM super_heroes")
                .map { it.name }
                .toList()
        }
    }
println("Starting query")
val heroes = query.await()
heroes.forEach(::println)
```

## Cancelling a query

The `Flow` returned by `queryAsFlow` is a regular `Flow` and can be cancelled using the regular `cancel` function.
This can be leveraged to cancel a query if the caller is no longer interested in the results or to return early from a long-running query.

### Example: Early Cancellation with Running Total

The query below selects all movies from a database, ordered by their worldwide gross income.
The cumulative gross income is calculated as each result is processed, and the query is cancelled when the cumulative gross income reaches 10 billion.

```kotlin
val job =
    async {
        getDataSource(postgresql).withConnection { connection ->
            var acc = 0L
            delay(50)
            val movies =
                connection.queryAsFlow<PopularMovie>(
                    """
                    SELECT
                     title,
                     gross_worldwide as grossed
                    FROM movies 
                    ORDER BY gross_worldwide DESC
                    """.trimIndent(),
                ).onEach {
                    acc += it.grossed
                    println("Accumulated gross including (${it.title}): ${String.format("%,d", acc)}")
                    if (acc >= 10_000_000_000) {
                        cancel("Gross reached 10 billion")
                    }
                }.toList()
            movies to acc
        }
    }
println("Query started")
try {
    val popularMovies = job.await()
    println(
        "Popular movies are: ${popularMovies.first.map { it.title }}, " +
            "grossing a total of ${String.format("%,d", popularMovies.second)}",
    )
} catch (e: CancellationException) {
    println("Query cancelled: ${e.message}")
}
```

### Example: Using takeWhile for Cleaner Cancellation

The same code could be rewritten to use the takeWhile function for a more functional approach:

```kotlin
val job =
    async {
        getDataSource(postgresql).withConnection { connection ->
            connection.queryAsFlow<PopularMovie>(
                """
                SELECT
                 title,
                 gross_worldwide as grossed
                FROM movies 
                ORDER BY gross_worldwide DESC
                """.trimIndent(),
            )
                .runningFold(0L to emptyList<PopularMovie>()) { (totalGross, movieList), movie ->
                    val newTotal = totalGross + movie.grossed
                    newTotal to (movieList + movie)
                }.takeWhile { (totalGross, _) ->
                    // query will be cancelled here
                    totalGross <= 10_000_000_000
                }.last()
        }
    }
println("Query started")
val popularMovies = job.await()
println(
    "Popular movies are: ${popularMovies.second.map { it.title }}, " +
        "grossing a total of ${String.format("%,d", popularMovies.first)}",
)
```

> NOTE: This code could be re-written to use a window function to calculate the cumulative gross income.
> This may be more efficient, however this example is only for illustrative purposes.

### Technical Considerations

When a flow is cancelled, Kapper will attempt to cancel the underlying JDBC `Statement` and `ResultSet` objects.
For this reason, Kapper sets the `fetchSize` of the `Statement`.
This is defaulted to `1,000` but can be changed by setting the `fetchSize` argument on the `queryAsFlow` function.
This gives the JDBC driver the opportunity to cancel the query at each batch of rows fetched.
However, it should be noted that not all JDBC drivers support this feature.

## Conclusion

With Kapper 1.3, you can now use Kotlin Flows to process the results of database queries, making Kapper even more idiomatic to Kotlin.

The combination of Kotlin's coroutines and Flows with Kapper's simple API provides a powerful yet intuitive way to work with database operations asynchronously.

Kapper 1.3 is available on [Maven Central](https://central.sonatype.com/artifact/net.samyn/kapper) and on [GitHub](https://github.com/driessamyn/kapper).
Give it a test driver and let me know what you think!
What should be next for Kapper? Let me know in the comments below.

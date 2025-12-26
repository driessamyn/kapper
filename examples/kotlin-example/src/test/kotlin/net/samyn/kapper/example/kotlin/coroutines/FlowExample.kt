package net.samyn.kapper.example.kotlin.coroutines

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import net.samyn.kapper.coroutines.queryAsFlow
import net.samyn.kapper.coroutines.withConnection
import net.samyn.kapper.example.DbBase
import net.samyn.kapper.example.kotlin.SuperHero
import net.samyn.kapper.execute
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.UUID
import java.util.stream.IntStream.range

class FlowExample : DbBase() {
    data class PopularMovie(
        val title: String,
        val releaseDate: Date,
        val grossed: Long,
    )

    @Test
    fun `simple example`() {
        val datasource = getDataSource(postgresql)
        insertHeroes(10)
        runBlocking {
            val query =
                datasource.withConnection {
                    async {
                        it
                            .queryAsFlow<SuperHero>("SELECT * FROM super_heroes")
                            .map { it.name }
                            .toList()
                    }
                }
            println("Starting query")
            val heroes = query.await()
            heroes.size shouldBe 10
            heroes.forEach(::println)
        }
    }

    private fun insertHeroes(n: Int) {
        getDataSource(postgresql).connection.use {
            range(0, n).forEach { i ->
                it.execute(
                    "INSERT INTO super_heroes (id, name, email, age) VALUES (:id, :name, :email, :age)",
                    "id" to UUID.randomUUID(),
                    "name" to "Superman $i",
                    "email" to "superman$i@d.com",
                    "age" to (40..99).random(),
                )
            }
        }
    }

    @Test
    fun `cancel query`() {
        runBlocking {
            // Using the gross_worldwide table from the SuperHeroRepository example to query the cumulative gross
            // until is reached.
            // NOTE: this could be achieved using a window function. The example is not meant to be an example
            // of a good real-world use case.
            // Its purpose is only to demonstrate how a query can be cancelled by using the Flow interface.
            val job =
                async {
                    getDataSource(postgresql).withConnection { connection ->
                        var acc = 0L
                        delay(50)
                        val movies =
                            connection
                                .queryAsFlow<PopularMovie>(
                                    """
                                    SELECT
                                     title,
                                     release_date as releasedate, 
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
        }
    }

    @Test
    fun `cancel query automatically`() {
        runBlocking {
            // As above, but using takeWhile to automatically cancel.
            val job =
                async {
                    getDataSource(postgresql).withConnection { connection ->
                        connection
                            .queryAsFlow<PopularMovie>(
                                """
                                SELECT
                                 title,
                                 release_date as releasedate, 
                                 gross_worldwide as grossed
                                FROM movies 
                                ORDER BY gross_worldwide DESC
                                """.trimIndent(),
                            ).runningFold(0L to emptyList<PopularMovie>()) { (totalGross, movieList), movie ->
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
        }
    }
}

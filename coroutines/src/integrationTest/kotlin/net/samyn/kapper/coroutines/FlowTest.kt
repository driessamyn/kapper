package net.samyn.kapper.coroutines

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import net.samyn.kapper.withTransaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.random.Random

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FlowTest {
    @Container
    val postgresql = PostgreSQLContainer("postgres:16")

    data class SuperHero(val id: UUID, val name: String, val age: Int)

    @BeforeEach
    fun setup() {
        postgresql.start()
        createDataSource(postgresql).withTransaction { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE super_heroes (
                        id UUID PRIMARY KEY,
                        name VARCHAR(100),
                        age INT
                    );
                    """.trimIndent().also {
                        println(it)
                    },
                )
                repeat(10) { it ->
                    statement.execute(
                        """
                        INSERT INTO super_heroes (id, name, age) VALUES
                            ('${UUID.randomUUID()}', 'Hulk $it', $it);
                        """.trimIndent().also {
                            println(it)
                        },
                    )
                }
            }
        }
    }

    @Test
    fun `multiple flows in coroutines`() {
        runBlocking {
            // using a single connection for both queries
            //  this will process both in a sequence because the JDBC driver blocks while the queries complete.
            //  this may not be what is expected.
            // using two connections will allow the queries to run in parallel.
            val (evenAgeResult, oddAgeResult) =
                createDataSource(postgresql).withConnection { connection ->
                    val one =
                        async {
                            printGreen("Executing query - $connection, filtering even ages")
                            connection
                                .queryAsFlow<SuperHero>(
                                    "SELECT * FROM super_heroes",
                                )
                                .filter {
                                    it.age % 2 == 0
                                }
                                .map {
                                    it.age
                                }
                        }
                    val two =
                        async {
                            printRed("Executing query - $connection, filtering odd ages")
                            connection
                                .queryAsFlow<SuperHero>(
                                    "SELECT * FROM super_heroes",
                                )
                                .filter {
                                    it.age % 2 == 1
                                }
                                .map {
                                    it.age
                                }
                        }
                    println("[${Thread.currentThread().name}] Waiting for results")
                    awaitAll(one, two)
                }
            evenAgeResult.reduce { acc, i ->
                val a = acc + i
                printGreen("[${Thread.currentThread().name}] acc=$a")
                a
            } shouldBe
                20.also {
                    printGreen("[${Thread.currentThread().name}] Even accumulative age is $it")
                }
            oddAgeResult.reduce { acc, i ->
                val a = acc + i
                printRed("[${Thread.currentThread().name}] accumulative age=$a")
                a
            } shouldBe
                25.also {
                    printRed("[${Thread.currentThread().name}] odd accumulative age is $it")
                }
            println("Query finished")
        }
    }

    @Test
    fun `multiple flows in coroutines processing in parallel`() {
        runBlocking {
            // using a single connection for both queries, but unlike above, collecting all results before processing
            //  we simulate slow processing, which means the processing can happen in parallel once the queries
            //  have completed.
            val (evenAgeResult, oddAgeResult) =
                createDataSource(postgresql).withConnection { connection ->
                    val one =
                        async {
                            printGreen("Executing queryAsFlow - $connection, filtering even ages")
                            connection
                                .queryAsFlow<SuperHero>(
                                    "SELECT * FROM super_heroes",
                                )
                                .filter {
                                    it.age % 2 == 0
                                }
                                .map {
                                    it.age
                                }
                                .toList()
                                .reduce { acc, i ->
                                    // simulate delay
                                    delay(Random.nextLong(10, 50))
                                    val a = acc + i
                                    printGreen("[${Thread.currentThread().name}] acc=$a")
                                    a
                                }
                        }
                    val two =
                        async {
                            printRed("Executing query - $connection, filtering odd ages")
                            connection
                                .queryAsFlow<SuperHero>(
                                    "SELECT * FROM super_heroes",
                                )
                                .filter {
                                    it.age % 2 == 1
                                }
                                .map {
                                    it.age
                                }
                                .toList()
                                .reduce { acc, i ->
                                    // simulate delay
                                    delay(Random.nextLong(30, 70))
                                    val a = acc + i
                                    printRed("[${Thread.currentThread().name}] accumulative age=$a")
                                    a
                                }
                        }
                    println("[${Thread.currentThread().name}] Waiting for results")
                    awaitAll(one, two)
                }
            evenAgeResult shouldBe
                20.also {
                    printGreen("[${Thread.currentThread().name}] Even accumulative age is $it")
                }
            oddAgeResult shouldBe
                25.also {
                    printRed("[${Thread.currentThread().name}] odd accumulative age is $it")
                }
            println("Query finished")
        }
    }

    @Test
    fun `cancel query explicitely`() {
        runBlocking {
            // here we exit early (in this example when cumulative age has exceeded 20)
            // cancelling the flow will cancel the JDBC iterator early and close the ResultSet.
            createDataSource(postgresql).withConnection { connection ->
                val job =
                    async {
                        connection
                            .queryAsFlow<SuperHero>(
                                "SELECT * FROM super_heroes",
                            )
                            .map {
                                it.age
                            }
                            .reduce { acc, i ->
                                val a = acc + i
                                printGreen("[${Thread.currentThread().name}] acc=$a")
                                a.also {
                                    if (a > 20) {
                                        println("Cancelling query as cumulative age is $a")
                                        cancel("Cumulative age exceeded 20")
                                    }
                                }
                            }
                    }
                println("Executing query - $connection, exit when age 20 is exceeded")
                shouldThrow<CancellationException> {
                    job.await() shouldBeGreaterThan 20
                }
            }
            println("Query finished")
        }
    }

    @Test
    fun `cancel query by expression`() {
        runBlocking {
            // This query never completes, so we cancel it after a certain condition is met.
            data class Entry(val n: Int)
            val job =
                async {
                    createDataSource(postgresql).withConnection { connection ->
                        connection.queryAsFlow<Entry>(
                            """
                            SELECT generate_series(1, 1000) AS n;
                            """.trimIndent(),
                        ).takeWhile {
                            println("Processing $it")
                            it.n <= 10
                        }.toList()
                    }
                }
            job.await().size shouldBe 10
        }
    }

    private val green = "\u001b[32m"
    private val resetGreen = "\u001b[0m"
    private val red = "\u001b[31m"
    private val resetRed = "\u001b[0m"

    private fun printGreen(s: String) {
        println(green + s + resetGreen)
    }

    private fun printRed(s: String) {
        println(red + s + resetRed)
    }
}

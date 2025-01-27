package net.samyn.kapper.coroutines

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.samyn.kapper.execute
import net.samyn.kapper.query
import net.samyn.kapper.querySingle
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatasourceWithConnectionDbTest {
    @Container
    val postgresql = PostgreSQLContainer("postgres:16")

    @Test
    fun `slow query on IO dispatcher`() {
        // this is a bit of a naughty test as it includes a sleep,
        //  but wanted to validate this against a real JDBC driver and a real slow query.
        runBlocking {
            val dataSource = createDataSource(postgresql)
            var slept = false
            val queryJob =
                launch {
                    val int =
                        dataSource.withConnection { connection ->
                            println("Executing slow query - $connection")
                            connection.querySingle<Int>(
                                "SELECT 1, pg_sleep(1)",
                                { rs, _ -> rs.getInt(1) },
                            ).also {
                                slept = true
                            }!!
                        }
                    int shouldBe 1
                }
            slept shouldBe false
            queryJob.join()
            slept shouldBe true
        }
    }

    @Test
    fun `execute tx on IO dispatcher`() {
        // this is a bit of a naughty test as it includes a sleep,
        //  but wanted to validate this against a real JDBC driver and a real slow query.
        runBlocking {
            val dataSource = createDataSource(postgresql)
            val queryJob =
                launch {
                    dataSource.withConnection { connection ->
                        connection.autoCommit = false
                        connection.execute(
                            "CREATE TABLE IF NOT EXISTS some_table (id SERIAL PRIMARY KEY, value INT);",
                        )
                        // please don't do this in _real_ code
                        (0..1000).forEach {
                            connection.execute(
                                "INSERT INTO some_table(value) VALUES(:value);",
                                "value" to it,
                            )
                        }
                        connection.commit()
                    }
                }
            queryJob.join()
            val emptyResult =
                dataSource.connection.query<Int>(
                    "SELECT * FROM some_table",
                    { rs, _ -> rs.getInt(1) },
                )
            emptyResult.count() shouldBe 1001
        }
    }

    private fun createDataSource(postgresql: PostgreSQLContainer<*>) =
        PGSimpleDataSource().also {
            it.setUrl(postgresql.jdbcUrl)
            it.user = postgresql.username
            it.password = postgresql.password
        }
}

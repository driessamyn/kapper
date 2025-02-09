package net.samyn.kapper.coroutines

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.samyn.kapper.execute
import net.samyn.kapper.query
import net.samyn.kapper.querySingle
import net.samyn.kapper.withTransaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.SQLException

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
            val insertJob =
                launch {
                    dataSource.withTransaction { connection ->
                        connection.execute(
                            "CREATE TABLE IF NOT EXISTS some_table (id SERIAL PRIMARY KEY, value INT);",
                        )
                        // please don't do this in _real_ code
                        repeat(100) {
                            connection.execute(
                                "INSERT INTO some_table(value) VALUES(:value);",
                                "value" to it,
                            )
                        }
                    }
                }
            insertJob.join()
            val emptyResult =
                dataSource.connection.query<Int>(
                    "SELECT * FROM some_table",
                    { rs, _ -> rs.getInt(1) },
                )
            emptyResult.count() shouldBe 100
        }
    }

    @Test
    fun `execute failed tx on IO dispatcher`() {
        // this is a bit of a naughty test as it includes a sleep,
        //  but wanted to validate this against a real JDBC driver and a real slow query.
        runBlocking {
            val dataSource = createDataSource(postgresql)
            val queryJob =
                launch {
                    dataSource.withConnection { connection ->
                        connection.execute(
                            "CREATE TABLE IF NOT EXISTS some_table2 (id INT PRIMARY KEY, value INT);",
                        )
                    }
                    // insert with duplicate key
                    assertThrows<SQLException> {
                        dataSource.withTransaction { connection ->
                            repeat(2) {
                                connection.execute(
                                    "INSERT INTO some_table2(id, value) VALUES(:id, :value);",
                                    "id" to 1,
                                    "value" to it,
                                )
                            }
                        }
                    }
                }
            queryJob.join()
            val emptyResult =
                dataSource.connection.query<Int>(
                    "SELECT * FROM some_table2",
                    { rs, _ -> rs.getInt(1) },
                )
            emptyResult.count() shouldBe 0
        }
    }
}

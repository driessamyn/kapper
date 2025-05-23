package net.samyn.kapper

import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.DriverManager
import java.time.Instant

@Testcontainers
class TypeCastTest {
    @Container
    val postgresql = PostgreSQLContainer("postgres:16")

    private fun createConnection() = DriverManager.getConnection(postgresql.jdbcUrl, postgresql.username, postgresql.password)

    @Test
    fun `postgresql uuid can cast to string`() {
        postgresql.start()
        // replicate isssue https://github.com/driessamyn/kapper/issues/77

        // create table
        createConnection().use { connection ->
            connection.execute(
                """
                CREATE EXTENSION IF NOT EXISTS "uuid-ossp";  -- Ensures the UUID extension is available

                CREATE TABLE blogs (
                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),  -- UUID primary key with default value generated by uuid-ossp
                    title TEXT NOT NULL, 
                    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,  -- Timestamp when the blog is created, defaults to current time
                    content TEXT NOT NULL  -- Content of the blog (text field for longer content)
                );
                """.trimIndent(),
            )
        }

        // insert
        createConnection().use { connection ->
            connection.execute(
                """
                INSERT INTO blogs (title, content) VALUES ('First blog', 'This is the first blog');
                """.trimIndent(),
            )
        }

        // select
        data class Blog(val id: String, val title: String, val createdAt: Instant, val content: String)

        val result =
            createConnection().use { connection ->
                connection.query<Blog>("SELECT id::text, title, created_at as createdAt, content FROM blogs;")
            }

        println(result)
        result.size shouldBe 1
        result.first().should {
            it.id.shouldBeInstanceOf<String>()
            it.title shouldBe "First blog"
            it.content shouldBe "This is the first blog"
        }
    }
}

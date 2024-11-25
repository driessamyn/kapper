package net.samyn.kapper.api

import io.mockk.mockk
import net.samyn.kapper.query
import net.samyn.kapper.querySingle
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.sql.Connection

/**
 * This tests just the usability of the API from Kotlin code
 */
class UsabilityTest {
    val connection = mockk<Connection>(relaxed = true)

    data class SuperHero(val name: String, val age: Int)

    @Test
    fun `can query`() {
        assertDoesNotThrow {
            connection.query<SuperHero>(
                """
                SELECT * FROM super_hero 
                WHERE age > :age
                """.trimIndent(),
                "age" to 30,
            )
        }
    }

    @Test
    fun `can query - mapper`() {
        assertDoesNotThrow {
            connection.query<SuperHero>(
                """
                SELECT * FROM super_hero 
                WHERE age > :age
                """.trimIndent(),
                { rs, fields ->
                    SuperHero(rs.getString("name"), rs.getInt("age"))
                },
                "age" to 30,
            )
        }
    }

    @Test
    fun `can query single`() {
        assertDoesNotThrow {
            connection.querySingle<SuperHero>(
                """
                SELECT * FROM super_hero 
                WHERE name = :name and age = :age
                """.trimIndent(),
                "name" to "Batman",
                "age" to 30,
            )
        }
    }

    @Test
    fun `can query single - mapper`() {
        assertDoesNotThrow {
            connection.querySingle<SuperHero>(
                """
                SELECT * FROM super_hero 
                WHERE name = :name and age = :age
                """.trimIndent(),
                { rs, fields ->
                    SuperHero(rs.getString("name"), rs.getInt("age"))
                },
                "name" to "Batman",
                "age" to 30,
            )
        }
    }
}

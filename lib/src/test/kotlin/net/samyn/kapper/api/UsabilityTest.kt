package net.samyn.kapper.api

import io.mockk.mockk
import net.samyn.kapper.querySingle
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.sql.Connection

/**
 * This tests just the usability of the API from Kotlin code
 */
class UsabilityTest {
    val connection = mockk<Connection>()

    data class SuperHero(val name: String, val age: Int)

    @Test
    @Disabled("TODO")
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
}

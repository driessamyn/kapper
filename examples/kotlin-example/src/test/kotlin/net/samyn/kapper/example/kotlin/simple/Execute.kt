package net.samyn.kapper.example.kotlin.simple

import net.samyn.kapper.example.DbBase
import net.samyn.kapper.execute
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import java.util.UUID

class Execute : DbBase() {
    @Test
    @Order(1)
    fun `execute insert`() {
        getDataSource(postgresql).connection.use {
            it.execute(
                """
                INSERT INTO super_heroes(id, name, email, age) 
                VALUES (:id, :name, :email, :age)
                """.trimIndent(),
                "id" to UUID.randomUUID(),
                "name" to "Batman",
                "email" to "batman@dc.com",
                "age" to 85,
            )
        }
    }

    @Test
    @Order(2)
    fun `execute update`() {
        getDataSource(postgresql).connection.use {
            it.execute(
                """
                UPDATE super_heroes
                SET age = 86
                WHERE name = :name
                """.trimIndent(),
                "name" to "Batman",
            )
        }
    }

    @Test
    @Order(3)
    fun `execute delete`() {
        getDataSource(postgresql).connection.use {
            it.execute(
                """
                DELETE FROM super_heroes
                WHERE name = :name
                """.trimIndent(),
                "name" to "Batman",
            )
        }
    }
}

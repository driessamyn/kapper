package net.samyn.kapper.example.kotlin.simple

import net.samyn.kapper.example.DbBase
import net.samyn.kapper.example.kotlin.SuperHero
import net.samyn.kapper.execute
import net.samyn.kapper.executeAll
import org.junit.jupiter.api.Test
import java.util.UUID

class ExecuteWithDto : DbBase() {
    @Test
    fun `execute insert`() {
        val hero = SuperHero(UUID.randomUUID(), "Thor", "thor@heroes.com", 150)
        getDataSource(postgresql).connection.use {
            it.execute(
                """
                INSERT INTO super_heroes(id, name, email, age) 
                VALUES (:id, :name, :email, :age)
                """.trimIndent(),
                hero,
                "id" to SuperHero::id,
                "name" to SuperHero::name,
                "email" to SuperHero::email,
                "age" to SuperHero::age,
            )
        }
    }

    @Test
    fun `execute bulk insert`() {
        val heroes =
            listOf(
                SuperHero(UUID.randomUUID(), "Aquaman", "aquaman@heroes.com", 150),
                SuperHero(UUID.randomUUID(), "Deadpool", "deadpool@heroes.com", 30),
            )
        getDataSource(postgresql).connection.use {
            it.executeAll(
                """
                INSERT INTO super_heroes(id, name, email, age) 
                VALUES (:id, :name, :email, :age)
                """.trimIndent(),
                heroes,
                "id" to SuperHero::id,
                "name" to SuperHero::name,
                "email" to SuperHero::email,
                "age" to SuperHero::age,
            )
        }
    }
}

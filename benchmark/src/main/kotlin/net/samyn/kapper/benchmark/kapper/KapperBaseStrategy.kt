package net.samyn.kapper.benchmark.kapper

import net.samyn.kapper.benchmark.BenchmarkStrategy
import net.samyn.kapper.benchmark.ISuperHero
import net.samyn.kapper.execute
import net.samyn.kapper.executeAll
import java.sql.Connection
import java.time.LocalDateTime
import java.util.UUID

data class SuperHero(override val id: UUID, override val name: String, override val email: String?, override val age: Int?) : ISuperHero

data class SuperHeroBattle(val superhero: String, val villain: String, val date: LocalDateTime)

abstract class KapperBaseStrategy : BenchmarkStrategy {
    override fun insertNewHero(
        connection: Connection,
        id: UUID,
        name: String,
        email: String,
        age: Int,
    ) {
        connection.execute(
            "INSERT INTO super_heroes (id, name, email, age) VALUES (:id, :name, :email, :age)",
            "id" to id,
            "name" to name,
            "email" to email,
            "age" to age,
        )
    }

    override fun changeHero(
        connection: Connection,
        id: UUID,
        name: String,
        email: String,
        age: Int,
    ) {
        connection.execute(
            "UPDATE super_heroes SET name = :name, email = :email, age = :age WHERE id = :id",
            "id" to id,
            "name" to name,
            "email" to email,
            "age" to age,
        )
    }

    override fun insertManyHeroes(connection: Connection) {
        val superHeroes =
            (1..100).map {
                SuperHero(
                    UUID.randomUUID(),
                    "Hero$it",
                    "hero$it@example.com",
                    20 + (it % 30),
                )
            }
        connection.executeAll(
            "INSERT INTO super_heroes (id, name, email, age) VALUES (:id, :name, :email, :age)",
            superHeroes,
            "id" to SuperHero::id,
            "name" to SuperHero::name,
            "email" to SuperHero::email,
            "age" to SuperHero::age,
        )
    }
}

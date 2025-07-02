package net.samyn.kapper.benchmark.jdbc

import net.samyn.kapper.benchmark.BenchmarkStrategy
import net.samyn.kapper.benchmark.ISuperHero
import java.sql.Connection
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.UUID

class JDBCStrategy : BenchmarkStrategy {
    data class SuperHero(override val id: UUID, override val name: String, override val email: String?, override val age: Int?) : ISuperHero

    data class SuperHeroBattle(val superhero: String, val villain: String, val date: LocalDateTime)

    override fun findHeroById(
        connection: Connection,
        id: UUID,
    ): Any? {
        connection.prepareStatement(
            "SELECT id, name, email, age FROM super_heroes WHERE id = ?",
        ).use { stmt ->
            stmt.setObject(1, id)
            stmt.executeQuery().use { resultSet ->
                return if (resultSet.next()) {
                    resultSet.mapRow()
                } else {
                    return null
                }
            }
        }
    }

    override fun find100Heroes(connection: Connection): List<Any> {
        val results = mutableListOf<SuperHero>()
        connection.prepareStatement(
            "SELECT id, name, email, age FROM super_heroes LIMIT 100",
        ).use { stmt ->
            stmt.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    results.add(resultSet.mapRow())
                }
            }
        }
        return results
    }

    override fun insertNewHero(
        connection: Connection,
        id: UUID,
        name: String,
        email: String,
        age: Int,
    ) {
        connection.prepareStatement(
            "INSERT INTO super_heroes (id, name, email, age) VALUES (?, ?, ?, ?)",
        ).use { stmt ->
            stmt.setObject(1, id)
            stmt.setString(2, name)
            stmt.setString(3, email)
            stmt.setInt(4, age)
            stmt.executeUpdate()
        }
    }

    override fun changeHero(
        connection: Connection,
        id: UUID,
        name: String,
        email: String,
        age: Int,
    ) {
        connection.prepareStatement(
            "UPDATE super_heroes SET name = ?, email = ?, age = ? WHERE id = ?",
        ).use { stmt ->
            stmt.setString(1, name)
            stmt.setString(2, email)
            stmt.setInt(3, age)
            stmt.setObject(4, id)
            stmt.executeUpdate()
        }
    }

    override fun findHeroBattles(
        connection: Connection,
        heroId: UUID,
    ): List<Any> {
        val results = mutableListOf<SuperHeroBattle>()
        connection.prepareStatement(
            """
            SELECT s.name as superhero, v.name as villain, b.battle_date as date
            FROM super_heroes as s
            INNER JOIN battles as b on s.id = b.super_hero_id
            INNER JOIN villains as v on v.id = b.villain_id
            WHERE s.id = ?
            """.trimIndent(),
        ).use { stmt ->
            stmt.setObject(1, heroId)
            stmt.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    results.add(
                        SuperHeroBattle(
                            resultSet.getString("superhero"),
                            resultSet.getString("villain"),
                            resultSet.getObject("date", LocalDateTime::class.java),
                        ),
                    )
                }
            }
        }
        return results
    }

    override fun insertManyHeroes(connection: Connection) {
        val heroes =
            (1..100).map {
                SuperHero(
                    UUID.randomUUID(),
                    "Hero$it",
                    "hero$it@example.com",
                    20 + (it % 30),
                )
            }
        connection.prepareStatement(
            "INSERT INTO super_heroes (id, name, email, age) VALUES (?, ?, ?, ?)",
        ).use { stmt ->
            for (h in heroes) {
                stmt.setObject(1, h.id)
                stmt.setString(2, h.name)
                stmt.setString(3, h.email)
                stmt.setInt(4, h.age ?: 0)
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    private fun ResultSet.mapRow(): SuperHero =
        SuperHero(
            UUID.fromString(this.getString("id")),
            this.getString("name"),
            this.getString("email"),
            this.getInt("age"),
        )
}

package net.samyn.kapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.testcontainers.containers.JdbcDatabaseContainer
import java.sql.ResultSet

class QuerySingleTests : AbstractDbTests() {
    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `should query 1 heros`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            val hero = connection.querySingle<SuperHero>("SELECT * FROM super_heroes ORDER BY age DESC LIMIT 1")
            hero.shouldBe(superman)
        }
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `should query hero with condition`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            val hero = connection.querySingle<SuperHero>("SELECT * FROM super_heroes WHERE age > :age", "age" to 85)
            hero.shouldBe(superman)
        }
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `should query specific columns`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            val hero =
                connection.querySingle<SuperHero>(
                    "SELECT id, name FROM super_heroes WHERE name = :name",
                    "name" to superman.name,
                )
            hero.shouldBe(SuperHero(superman.id, superman.name))
        }
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `should handle empty result set`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            val hero =
                connection.querySingle<SuperHero>(
                    "SELECT * FROM super_heroes WHERE name = :name",
                    "name" to "joker",
                )
            hero.shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `should throw when larger than one result`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            val ex =
                shouldThrow<KapperResultException> {
                    connection.querySingle<SuperHero>(
                        "SELECT * FROM super_heroes",
                    )
                }
            ex.message.shouldContain("3")
        }
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `should query with multiple conditions`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            val hero =
                connection.querySingle<SuperHero>(
                    "SELECT * FROM super_heroes WHERE age BETWEEN :fromAge AND :toAge",
                    "fromAge" to 86,
                    "toAge" to 89,
                )
            hero.shouldBe(superman)
        }
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `can use custom mapper`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            val villain =
                connection.querySingle<Villain>(
                    "SELECT id, name FROM super_heroes WHERE name = :name",
                    ::createVillain,
                    "name" to superman.name,
                )
            villain.shouldNotBeNull()
            villain.id.shouldBe(superman.id.toString())
            villain.name.shouldBe(superman.name.toString())
        }
    }

    private fun createVillain(
        resultSet: ResultSet,
        fields: Map<String, Field>,
    ): Villain =
        Villain().also {
            it.id = resultSet.getString("id")
            it.name = resultSet.getString("name")
        }
}

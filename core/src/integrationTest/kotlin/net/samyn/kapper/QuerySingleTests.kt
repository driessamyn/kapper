package net.samyn.kapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.sql.Connection
import java.sql.ResultSet

class QuerySingleTests : AbstractDbTests() {
    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `should query 1 heros`(connection: Connection) {
        val hero = connection.querySingle<SuperHero>("SELECT * FROM super_heroes_$testId ORDER BY age DESC LIMIT 1")
        hero.shouldBe(superman)
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `should query hero with condition`(connection: Connection) {
        val hero = connection.querySingle<SuperHero>("SELECT * FROM super_heroes_$testId WHERE age > :age", "age" to 85)
        hero.shouldBe(superman)
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `should query specific columns`(connection: Connection) {
        val hero =
            connection.querySingle<SuperHero>(
                "SELECT id, name FROM super_heroes_$testId WHERE name = :name",
                "name" to superman.name,
            )
        hero.shouldBe(SuperHero(superman.id, superman.name))
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `should handle empty result set`(connection: Connection) {
        val hero =
            connection.querySingle<SuperHero>(
                "SELECT * FROM super_heroes_$testId WHERE name = :name",
                "name" to "joker",
            )
        hero.shouldBeNull()
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `should throw when larger than one result`(connection: Connection) {
        val ex =
            shouldThrow<KapperResultException> {
                connection.querySingle<SuperHero>(
                    "SELECT * FROM super_heroes_$testId",
                )
            }
        ex.message.shouldContain("3")
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `should query with multiple conditions`(connection: Connection) {
        val hero =
            connection.querySingle<SuperHero>(
                "SELECT * FROM super_heroes_$testId WHERE age BETWEEN :fromAge AND :toAge",
                "fromAge" to 86,
                "toAge" to 89,
            )
        hero.shouldBe(superman)
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `can use custom mapper`(connection: Connection) {
        val villain =
            connection.querySingle<Villain>(
                "SELECT id, name FROM super_heroes_$testId WHERE name = :name",
                ::createVillain,
                "name" to superman.name,
            )
        villain.shouldNotBeNull()
        villain.id.shouldBe(superman.id.toString())
        villain.name.shouldBe(superman.name)
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

package net.samyn.kapper

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.testcontainers.containers.JdbcDatabaseContainer
import java.sql.ResultSet

class QueryTests : AbstractDbTests() {
    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `should query all heros`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            val heroes = connection.query<SuperHero>("SELECT * FROM super_heroes")
            heroes.shouldContainExactlyInAnyOrder(
                superman,
                batman,
                spiderMan,
            )
        }
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `should query heros with condition`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            val heroes = connection.query<SuperHero>("SELECT * FROM super_heroes WHERE age > :age", "age" to 80)
            heroes.shouldContainExactlyInAnyOrder(
                superman,
                batman,
            )
        }
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `should query specific columns`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            val heroes =
                connection.query<SuperHero>(
                    "SELECT id, name FROM super_heroes WHERE name = :name",
                    "name" to superman.name,
                )
            heroes.shouldContainExactlyInAnyOrder(
                SuperHero(superman.id, superman.name),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `should handle empty result set`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            val heroes =
                connection.query<SuperHero>(
                    "SELECT * FROM super_heroes WHERE name = :name",
                    "name" to "joker",
                )
            heroes.shouldBeEmpty()
        }
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `should query with multiple conditions`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            val heroes =
                connection.query<SuperHero>(
                    "SELECT * FROM super_heroes WHERE age BETWEEN :fromAge AND :toAge",
                    "fromAge" to 80,
                    "toAge" to 89,
                )
            heroes.shouldContainExactlyInAnyOrder(
                superman,
                batman,
            )
        }
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `support field labels`(container: JdbcDatabaseContainer<*>) {
        data class SimpleClass(val superHeroName: String)
        createConnection(container).use { connection ->
            val hero =
                connection.query<SimpleClass>(
                    "SELECT name as superHeroName FROM super_heroes WHERE id = :id",
                    "id" to superman.id,
                )
            hero.shouldContainOnly(
                SimpleClass(superman.name),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `can use custom mapper`(container: JdbcDatabaseContainer<*>) {
        createConnection(container).use { connection ->
            val villain =
                connection.query<Villain>(
                    "SELECT id, name FROM super_heroes WHERE name = :name",
                    ::createVillain,
                    "name" to superman.name,
                )

            villain.size.shouldBe(1)
            villain.first().id.shouldBe(superman.id.toString())
            villain.first().name.shouldBe(superman.name.toString())
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

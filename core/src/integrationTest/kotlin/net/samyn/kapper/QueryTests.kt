package net.samyn.kapper

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.sql.Connection
import java.sql.ResultSet

class QueryTests : AbstractDbTests() {
    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `should query all heros`(connection: Connection) {
        val heroes = connection.query<SuperHero>("SELECT * FROM super_heroes_$testId")
        heroes.shouldContainExactlyInAnyOrder(
            superman,
            batman,
            spiderMan,
        )
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `should query heros with condition`(connection: Connection) {
        val heroes = connection.query<SuperHero>("SELECT * FROM super_heroes_$testId WHERE age > :age", "age" to 80)
        heroes.shouldContainExactlyInAnyOrder(
            superman,
            batman,
        )
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `should query specific columns`(connection: Connection) {
        val heroes =
            connection.query<SuperHero>(
                "SELECT id, name FROM super_heroes_$testId WHERE name = :name",
                "name" to superman.name,
            )
        heroes.shouldContainExactlyInAnyOrder(
            SuperHero(superman.id, superman.name),
        )
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `should handle empty result set`(connection: Connection) {
        val heroes =
            connection.query<SuperHero>(
                "SELECT * FROM super_heroes_$testId WHERE name = :name",
                "name" to "joker",
            )
        heroes.shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `should query with multiple conditions`(connection: Connection) {
        val heroes =
            connection.query<SuperHero>(
                "SELECT * FROM super_heroes_$testId WHERE age BETWEEN :fromAge AND :toAge",
                "fromAge" to 80,
                "toAge" to 89,
            )
        heroes.shouldContainExactlyInAnyOrder(
            superman,
            batman,
        )
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `support field labels`(connection: Connection) {
        data class SimpleClass(val superHeroName: String)
        val hero =
            connection.query<SimpleClass>(
                "SELECT name as superHeroName FROM super_heroes_$testId WHERE id = :id",
                "id" to superman.id,
            )
        hero.shouldContainOnly(
            SimpleClass(superman.name),
        )
    }

    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `can use custom registered mapper`(connection: Connection) {
        data class SuperHero2(val name: String, val email: String? = null, val age: Int? = null)

        class SuperHeroMapper : Mapper<SuperHero2> {
            override fun createInstance(
                resultSet: ResultSet,
                fields: Map<String, Field>,
            ) = SuperHero2(
                name = resultSet.getString("name"),
                email = resultSet.getString("email"),
                age = resultSet.getInt("age"),
            )
        }

        Kapper.mapperRegistry.registerIfAbsent<SuperHero2>(SuperHeroMapper())

        val heroes = connection.query<SuperHero2>("SELECT * FROM super_heroes_$testId")
        heroes.shouldContainExactlyInAnyOrder(
            SuperHero2(superman.name, superman.email, superman.age),
            SuperHero2(spiderMan.name, spiderMan.email, spiderMan.age),
            SuperHero2(batman.name, batman.email, batman.age),
        )
    }

    @ParameterizedTest
    @MethodSource("databaseContainers")
    fun `can use custom mapper`(connection: Connection) {
        val villain =
            connection.query<Villain>(
                "SELECT id, name FROM super_heroes_$testId WHERE name = :name",
                ::createVillain,
                "name" to superman.name,
            )

        villain.size.shouldBe(1)
        villain.first().id!!.shouldBe(superman.id.toString().lowercase().replace("-", ""))
        villain.first().name.shouldBe(superman.name)
    }

    private fun createVillain(
        resultSet: ResultSet,
        fields: Map<String, Field>,
    ): Villain =
        Villain().also {
            it.id = resultSet.getString("id").lowercase().replace("-", "")
            it.name = resultSet.getString("name")
        }
}

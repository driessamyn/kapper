package net.samyn.kapper

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.sql.ResultSet

class QueryTests : AbstractDbTests() {
    @Test
    fun `should query all heros`() {
        val heroes = connection.query<SuperHero>("SELECT * FROM super_heroes_$testId")
        heroes.shouldContainExactlyInAnyOrder(
            superman,
            batman,
            spiderMan,
        )
    }

    @Test
    fun `should query heros with condition`() {
        val heroes = connection.query<SuperHero>("SELECT * FROM super_heroes_$testId WHERE age > :age", "age" to 80)
        heroes.shouldContainExactlyInAnyOrder(
            superman,
            batman,
        )
    }

    @Test
    fun `should query specific columns`() {
        val heroes =
            connection.query<SuperHero>(
                "SELECT id, name FROM super_heroes_$testId WHERE name = :name",
                "name" to superman.name,
            )
        heroes.shouldContainExactlyInAnyOrder(
            SuperHero(superman.id, superman.name),
        )
    }

    @Test
    fun `should handle empty result set`() {
        val heroes =
            connection.query<SuperHero>(
                "SELECT * FROM super_heroes_$testId WHERE name = :name",
                "name" to "joker",
            )
        heroes.shouldBeEmpty()
    }

    @Test
    fun `should query with multiple conditions`() {
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

    @Test
    fun `support field labels`() {
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

    @Test
    fun `can use custom registered mapper`() {
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

    @Test
    fun `can use custom mapper`() {
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

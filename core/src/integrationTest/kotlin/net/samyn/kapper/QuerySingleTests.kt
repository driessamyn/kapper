package net.samyn.kapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import net.samyn.kapper.internal.getDbFlavour
import org.junit.jupiter.api.Test
import java.sql.ResultSet

class QuerySingleTests : AbstractDbTests() {
    @Test
    fun `should query 1 heros`() {
        val sql =
            if (DbFlavour.MSSQLSERVER == connection.getDbFlavour()) {
                "SELECT TOP 1 * FROM super_heroes_$testId ORDER BY name ASC"
            } else if (DbFlavour.ORACLE == connection.getDbFlavour()) {
                "SELECT * FROM super_heroes_$testId ORDER BY name ASC FETCH FIRST 1 ROWS ONLY"
            } else {
                "SELECT * FROM super_heroes_$testId ORDER BY name ASC LIMIT 1"
            }
        val hero = connection.querySingle<SuperHero>(sql)
        hero.shouldBe(batman)
    }

    @Test
    fun `should query hero with condition`() {
        val hero = connection.querySingle<SuperHero>("SELECT * FROM super_heroes_$testId WHERE age > :age", "age" to 85)
        hero.shouldBe(superman)
    }

    @Test
    fun `should query specific columns`() {
        val hero =
            connection.querySingle<SuperHero>(
                "SELECT id, name FROM super_heroes_$testId WHERE name = :name",
                "name" to superman.name,
            )
        hero.shouldBe(SuperHero(superman.id, superman.name))
    }

    @Test
    fun `should handle empty result set`() {
        val hero =
            connection.querySingle<SuperHero>(
                "SELECT * FROM super_heroes_$testId WHERE name = :name",
                "name" to "joker",
            )
        hero.shouldBeNull()
    }

    @Test
    fun `should throw when larger than one result`() {
        val ex =
            shouldThrow<KapperResultException> {
                connection.querySingle<SuperHero>(
                    "SELECT * FROM super_heroes_$testId",
                )
            }
        ex.message.shouldContain("4")
    }

    @Test
    fun `should query with multiple conditions`() {
        val hero =
            connection.querySingle<SuperHero>(
                "SELECT * FROM super_heroes_$testId WHERE age BETWEEN :fromAge AND :toAge",
                "fromAge" to 86,
                "toAge" to 89,
            )
        hero.shouldBe(superman)
    }

    @Test
    fun `can use custom mapper`() {
        val villain =
            connection.querySingle<Villain>(
                "SELECT id, name FROM super_heroes_$testId WHERE name = :name",
                ::createVillain,
                "name" to superman.name,
            )
        villain.shouldNotBeNull()
        villain.id!!.shouldBe(superman.id.toString().lowercase().replace("-", ""))
        villain.name.shouldBe(superman.name)
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

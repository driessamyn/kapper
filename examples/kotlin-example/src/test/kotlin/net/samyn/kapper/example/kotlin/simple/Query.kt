package net.samyn.kapper.example.kotlin.simple

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import net.samyn.kapper.example.DbBase
import net.samyn.kapper.example.kotlin.SuperHero
import net.samyn.kapper.query
import net.samyn.kapper.querySingle
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.UUID

class Query : DbBase() {
    @BeforeAll
    override fun setup() {
        super.setup()
        getDataSource(postgresql).connection.use { connection ->

            connection.createStatement().use { statement ->
                val sql =
                    """
                    INSERT INTO super_heroes (id, name, email, age) VALUES
                        ('${UUID.randomUUID()}', 'Superman', 'superman@dc.com', 86),
                        ('${UUID.randomUUID()}', 'Batman', 'batman@dc.com', 85),
                        ('${UUID.randomUUID()}', 'Spider-man', 'spider@marvel.com', 62);
                    """.trimIndent().also {
                        println(it)
                    }
                statement.execute(sql)
            }
        }
    }

    @Test
    fun `simple query`() {
        val heroes =
            getDataSource(postgresql).connection.use {
                it.query<SuperHero>("SELECT * FROM super_heroes")
            }
        println(heroes)
        heroes.shouldNotBeEmpty()
    }

    @Test
    fun `query with parameter`() {
        val olderHeroes =
            getDataSource(postgresql).connection.use {
                it.query<SuperHero>(
                    "SELECT * FROM super_heroes WHERE age > :age",
                    "age" to 80,
                )
            }
        println(olderHeroes)
        olderHeroes.shouldNotBeEmpty()
    }

    @Test
    fun `query with custom mapper`() {
        val heroAges =
            getDataSource(postgresql).connection.use {
                it.query<Pair<String, *>>(
                    "SELECT * FROM super_heroes WHERE age > :age",
                    { resultSet, fields ->
                        Pair(
                            resultSet.getString(fields["name"]!!.columnIndex),
                            resultSet.getInt(fields["age"]!!.columnIndex),
                        )
                    },
                    "age" to 80,
                )
            }
        println(heroAges)
        heroAges.shouldNotBeEmpty()
    }

    @Test
    fun `query returning single result`() {
        val batman =
            getDataSource(postgresql).connection.use {
                it.querySingle<SuperHero>(
                    "SELECT * FROM super_heroes WHERE name = :name",
                    "name" to "Batman",
                )
            }
        println(batman)
        batman.shouldNotBeNull()
    }
}

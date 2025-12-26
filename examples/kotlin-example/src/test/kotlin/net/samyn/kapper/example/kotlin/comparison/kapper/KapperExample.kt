package net.samyn.kapper.example.kotlin.comparison.kapper

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import net.samyn.kapper.example.DbBase
import net.samyn.kapper.example.kotlin.SuperHero
import net.samyn.kapper.example.kotlin.Villain
import net.samyn.kapper.example.kotlin.kapper.SuperHeroRepository
import net.samyn.kapper.querySingle
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.testcontainers.containers.JdbcDatabaseContainer
import java.time.LocalDateTime
import java.util.UUID

class KapperExample : DbBase() {
    val batman = SuperHero(UUID.randomUUID(), "Batman", "batman@dc.com", 85)
    val spiderMan = SuperHero(UUID.randomUUID(), "Spider-man", "spider@marvel.com", 62)

    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun example(container: JdbcDatabaseContainer<*>) {
        container.isRunning.shouldBeTrue()
        getDataSource(container).also { ds ->
            val repo = SuperHeroRepository(ds)
            repo.list().shouldBeEmpty()

            // insert heroes
            repo.insertHero(batman)
            repo.insertHero(spiderMan)

            // find all
            repo.list().shouldContainAll(batman, spiderMan)

            // find by ID
            repo.findById(batman.id).shouldBe(batman)

            // find by name
            repo.findByName(spiderMan.name).shouldBe(spiderMan)

            // insert some battles
            val catwoman = Villain(UUID.randomUUID(), "Catwoman")
            repo.insertBattle(
                batman,
                catwoman,
                LocalDateTime.of(2014, 7, 13, 23, 47),
            )
            val joker = Villain(UUID.randomUUID(), "Joker")
            repo.insertBattle(
                batman,
                joker,
                LocalDateTime.of(1940, 3, 5, 14, 30),
            )
            // another insert of battle with joker should not create a new Villain
            repo.insertBattle(
                batman,
                joker,
                LocalDateTime.of(1994, 1, 19, 22, 5),
            )

            // find battles - example of a join
            val battles = repo.findBattles(batman.name)
            battles.shouldHaveSize(3)
            battles.map { it.villain }.shouldContainAll("Catwoman", "Joker")

            // simple custom mapper
            val villainAsMap =
                ds.connection.use {
                    it.querySingle<Map<String, *>>(
                        "SELECT * FROM villains WHERE name = :name",
                        { resultSet, fields ->
                            mapOf(
                                "id" to resultSet.getString("id"),
                                "name" to resultSet.getString("name"),
                            )
                        },
                        "name" to "Joker",
                    )
                }
            villainAsMap.shouldNotBeNull()
            villainAsMap["id"].shouldBe(joker.id.toString())
            villainAsMap["name"].shouldBe(joker.name)

            // example of complex query and custom mapper
            val popularMovies = repo.findPopularMovies()
            popularMovies.shouldHaveSize(3)
            popularMovies.map { it.title }.shouldContainAll(
                "Avengers: Endgame",
                "Avengers: Infinity War",
                "Spider-Man: No Way Home",
            )
            popularMovies.forEach {
                println("${it.title} took ${it.grossed}, or ${it.comparedToAnnualAverage} compared to the annual average.")
                it.comparedToAnnualAverage.shouldBeGreaterThan(1.0)
            }
        }
    }
}

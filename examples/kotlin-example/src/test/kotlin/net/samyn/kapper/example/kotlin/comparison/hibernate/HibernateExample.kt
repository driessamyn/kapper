package net.samyn.kapper.example.kotlin.comparison.hibernate

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import net.samyn.kapper.example.DbBase
import net.samyn.kapper.example.kotlin.hibernate.SuperHeroEntity
import net.samyn.kapper.example.kotlin.hibernate.SuperHeroHibernateQueries
import net.samyn.kapper.example.kotlin.hibernate.VillainEntity
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.testcontainers.containers.JdbcDatabaseContainer
import java.time.LocalDateTime
import java.util.UUID

class HibernateExample : DbBase() {
    val batman = SuperHeroEntity(UUID.randomUUID(), "Batman", "batman@dc.com", 85)
    val spiderMan = SuperHeroEntity(UUID.randomUUID(), "Spider-man", "spider@marvel.com", 62)

    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun example(container: JdbcDatabaseContainer<*>) {
        container.isRunning.shouldBeTrue()
        val queries = SuperHeroHibernateQueries(container.username, container.password, container.jdbcUrl)
        queries.list().shouldBeEmpty()

        // insert heroes
        queries.insertHero(batman)
        queries.insertHero(spiderMan)

        // find all
        queries.list().shouldContainAll(batman, spiderMan)

        // find by ID
        queries.findById(batman.id).shouldBe(batman)

        // find by name
        queries.findByName(spiderMan.name).shouldBe(spiderMan)

        // insert some battles
        val catwoman = VillainEntity(UUID.randomUUID(), "Catwoman")
        queries.insertBattle(
            batman,
            catwoman,
            LocalDateTime.of(2014, 7, 13, 23, 47),
        )
        val joker = VillainEntity(UUID.randomUUID(), "Joker")
        queries.insertBattle(
            batman,
            joker,
            LocalDateTime.of(1940, 3, 5, 14, 30),
        )
        // another insert of battle with joker should not create a new Villain
        queries.insertBattle(
            batman,
            joker,
            LocalDateTime.of(1994, 1, 19, 22, 5),
        )

        // find battles - example of a join
        val battles = queries.findBattles(batman.name)
        battles.shouldHaveSize(3)
        battles.map { it.villain.name }.shouldContainAll("Catwoman", "Joker")

        // example of complex query and custom mapper
        val popularMovies = queries.findPopularMovies()
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

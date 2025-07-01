package net.samyn.kapper.benchmark

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import net.samyn.kapper.benchmark.hibernate.HibernateStrategy
import net.samyn.kapper.benchmark.jdbc.JDBCStrategy
import net.samyn.kapper.benchmark.kapper.KapperNoAutomapStrategy
import net.samyn.kapper.benchmark.kapper.KapperRecordStrategy
import net.samyn.kapper.benchmark.kapper.KapperStrategy
import net.samyn.kapper.benchmark.kapper.SuperHero
import net.samyn.kapper.benchmark.ktorm.KtormStrategy
import net.samyn.kapper.benchmark.setup.SQLiteConfig
import net.samyn.kapper.benchmark.setup.heroId
import net.samyn.kapper.query
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.measureTime

class SQLiteTest {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
        private val dbConfig = SQLiteConfig(100)

        @JvmStatic
        fun strategies() =
            mapOf(
                "Kapper" to KapperStrategy(),
                "Kapper-No-Automap" to KapperNoAutomapStrategy(),
                "Kapper-Record" to KapperRecordStrategy(),
                "JDBC" to JDBCStrategy(),
                "Hibernate" to HibernateStrategy(),
//                "Hibernate-Record" to HibernateRecordStrategy(),
                "Ktorm" to KtormStrategy(),
            ).map { arguments(named(it.key, it.value)) }
    }

    @Test
    fun `when config created setup data`() {
        SQLiteConfig(10).createConnection().use {
            it.query<SuperHero>("SELECT * FROM super_heroes").size shouldBeGreaterThan 1
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    fun `when find by id return`(strategy: BenchmarkStrategy) {
        measureAndLogTime {
            strategy.findHeroById(dbConfig.cachedConnection, heroId(1))
                .shouldNotBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    fun `when find 100 return`(strategy: BenchmarkStrategy) {
        measureAndLogTime {
            strategy.find100Heroes(dbConfig.cachedConnection)
                .size shouldBe 100
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    fun `when find batlles return`(strategy: BenchmarkStrategy) {
        measureAndLogTime {
            strategy.findHeroBattles(dbConfig.cachedConnection, heroId(5))
                .size.shouldBeGreaterThan(0)
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    fun `can insert new hero`(strategy: BenchmarkStrategy) {
        val newId = UUID.randomUUID()
        measureAndLogTime {
            strategy.insertNewHero(
                dbConfig.cachedConnection,
                newId,
                "New Hero",
                "$newId@heroes.com",
                30,
            )
        }

        strategy.findHeroById(dbConfig.cachedConnection, newId).shouldNotBeNull()
    }

    @ParameterizedTest
    @MethodSource("strategies")
    fun `can update hero`(strategy: BenchmarkStrategy) {
        val heroId = heroId(10)
        measureAndLogTime {
            strategy.changeHero(
                dbConfig.cachedConnection,
                heroId,
                "Changed Hero",
                "$heroId@heroes.com",
                35,
            )
        }

        strategy.findHeroById(dbConfig.cachedConnection, heroId) should {
            it as ISuperHero
            it.age shouldBe 35
            it.email shouldBe "$heroId@heroes.com"
            it.name shouldBe "Changed Hero"
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    fun `can bulk insert 100 heroes`(strategy: BenchmarkStrategy) {
        val initialCount = dbConfig.cachedConnection.query<SuperHero>("SELECT * FROM super_heroes").size
        measureAndLogTime {
            strategy.insertManyHeroes(dbConfig.cachedConnection)
        }
        val afterCount = dbConfig.cachedConnection.query<SuperHero>("SELECT * FROM super_heroes").size
        (afterCount - initialCount) shouldBe 100
    }

    private fun measureAndLogTime(block: () -> Unit) {
        measureTime(block).also {
            logger.info("Time taken: $it")
        }
    }
}

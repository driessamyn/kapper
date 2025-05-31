package net.samyn.kapper.benchmark

import net.samyn.kapper.benchmark.hibernate.HibernateStrategy
import net.samyn.kapper.benchmark.jdbc.JDBCStrategy
import net.samyn.kapper.benchmark.kapper.KapperNoAutomapStrategy
import net.samyn.kapper.benchmark.kapper.KapperRecordStrategy
import net.samyn.kapper.benchmark.kapper.KapperStrategy
import net.samyn.kapper.benchmark.ktorm.KtormStrategy
import net.samyn.kapper.benchmark.setup.DatabaseConfig
import net.samyn.kapper.benchmark.setup.DatabaseType
import net.samyn.kapper.benchmark.setup.createDatabaseConfig
import net.samyn.kapper.benchmark.setup.heroId
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.infra.Blackhole
import java.util.UUID
import java.util.concurrent.TimeUnit

@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class KapperBenchmark {
    @State(Scope.Thread)
    open class BenchmarkState {
        @Param("SQLITE", "POSTGRESQL")
        private lateinit var databaseType: String

        @Param("JDBC", "KAPPER", "KAPPER-RECORD", "KAPPER-NO-AUTOMAP", "HIBERNATE", "HIBERNATE-RECORD", "KTORM")
        private lateinit var library: String

        @Param("10_000")
        private lateinit var rows: String

        val heroId = heroId(10)
        lateinit var benchmarkStrategy: BenchmarkStrategy
        lateinit var databaseConfig: DatabaseConfig
        var iteration = 0

        @Setup
        fun setup() {
            databaseConfig =
                createDatabaseConfig(
                    DatabaseType.valueOf(databaseType),
                    rows.replace("_", "").toInt(),
                )
            benchmarkStrategy =
                when (library) {
                    "JDBC" -> JDBCStrategy()
                    "KAPPER" -> KapperStrategy()
                    "KAPPER-RECORD" -> KapperRecordStrategy()
                    "KAPPER-NO-AUTOMAP" -> KapperNoAutomapStrategy()
                    "HIBERNATE" -> HibernateStrategy()
//                    "HIBERNATE-RECORD" -> HibernateRecordStrategy()
                    "HIBERNATE-RECORD" -> throw NotImplementedError("Hibernate doesn't properly support records yet")
                    "KTORM" -> KtormStrategy()
                    else -> throw IllegalArgumentException("Unknown ORM type: $library")
                }
        }

        @TearDown
        fun tearDown() {
            databaseConfig.close()
        }
    }

    @Benchmark
    fun findById(
        state: BenchmarkState,
        blackhole: Blackhole,
    ) {
        val result =
            state.benchmarkStrategy.findHeroById(
                state.databaseConfig.cachedConnection,
                state.heroId,
            )
        blackhole.consume(result)
    }

    @Benchmark
    fun find100(
        state: BenchmarkState,
        blackhole: Blackhole,
    ) {
        val result =
            state.benchmarkStrategy.find100Heroes(
                state.databaseConfig.cachedConnection,
            )
        blackhole.consume(result)
    }

    @Benchmark
    fun simpleJoin(
        state: BenchmarkState,
        blackhole: Blackhole,
    ) {
        val result =
            state.benchmarkStrategy.findHeroBattles(
                state.databaseConfig.cachedConnection,
                state.heroId,
            )
        blackhole.consume(result)
    }

    @Benchmark
    fun insertSingleRow(state: BenchmarkState) {
        val id = UUID.randomUUID()
        state.benchmarkStrategy.insertNewHero(
            state.databaseConfig.cachedConnection,
            id,
            "Hero $id",
            "$id@heroes.com",
            80,
        )
    }

    @Benchmark
    fun updateSingleRow(state: BenchmarkState) {
        state.benchmarkStrategy.changeHero(
            state.databaseConfig.cachedConnection,
            state.heroId,
            "Hero ${state.heroId}",
            "${state.heroId}@heroes.com",
            80 + state.iteration,
        )
        state.iteration++
    }
}

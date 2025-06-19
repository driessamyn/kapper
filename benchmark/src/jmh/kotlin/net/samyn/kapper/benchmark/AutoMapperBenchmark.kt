package net.samyn.kapper.benchmark

import net.samyn.kapper.benchmark.mapper.ComplexDataClassMapperBenchmark
import net.samyn.kapper.benchmark.mapper.ComplexRecordMapperBenchmark
import net.samyn.kapper.benchmark.mapper.MapperBenchmark
import net.samyn.kapper.benchmark.mapper.SimpleDataClassMapperBenchmark
import net.samyn.kapper.benchmark.mapper.SimpleRecordMapperBenchmark
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 3, jvmArgsAppend = ["-XX:-Inline"])
open class AutoMapperBenchmark {
    @State(Scope.Thread)
    open class AutoMapperBenchmarkState {
        @Param("SIMPLE-DATACLASS", "SIMPLE-RECORD", "COMPLEX-DATACLASS", "COMPLEX-RECORD")
        private lateinit var scenario: String

        @Param("1", "100")
        private lateinit var numberOfResults: String

        lateinit var benchmarkStrategy: MapperBenchmark

        @Setup
        fun setup() {
            val resultsCount =
                numberOfResults.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid number of results: $numberOfResults")
            benchmarkStrategy =
                when (scenario) {
                    "SIMPLE-DATACLASS" -> SimpleDataClassMapperBenchmark(resultsCount)
                    "SIMPLE-RECORD" -> SimpleRecordMapperBenchmark(resultsCount)
                    "COMPLEX-DATACLASS" -> ComplexDataClassMapperBenchmark(resultsCount)
                    "COMPLEX-RECORD" -> ComplexRecordMapperBenchmark(resultsCount)
                    else -> throw IllegalArgumentException("Unknown scenario: $scenario")
                }
        }
    }

    @Benchmark
    fun autoMapperBenchmark(
        state: AutoMapperBenchmarkState,
        blackhole: Blackhole,
    ) {
        val allResults = state.benchmarkStrategy.map()
        assert(allResults.size == state.benchmarkStrategy.numberOfResults) {
            "Expected ${state.benchmarkStrategy.numberOfResults} results, but got ${allResults.size}"
        }
        allResults.forEach {
            blackhole.consume(it)
        }
        blackhole.consume(allResults.sumOf { it.hashCode() })
        blackhole.consume(allResults)
    }
}

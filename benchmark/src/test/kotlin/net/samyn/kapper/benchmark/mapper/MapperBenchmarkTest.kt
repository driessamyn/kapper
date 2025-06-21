package net.samyn.kapper.benchmark.mapper

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.time.measureTime

class MapperBenchmarkTest {
    @Test
    fun `simple data class mapper should return mapped results`() {
        val numberOfRows = 1000
        val benchmark = SimpleDataClassMapperBenchmark(numberOfRows)
        benchmark.map().size shouldBe numberOfRows

        measureTime {
            benchmark.map().size shouldBe numberOfRows
        }.also {
            println("Time taken: $it")
        }
    }

    @Test
    fun `simple record mapper should return mapped results`() {
        val numberOfRows = 1000
        val benchmark = SimpleRecordMapperBenchmark(numberOfRows)
        benchmark.map().size shouldBe numberOfRows

        measureTime {
            benchmark.map().size shouldBe numberOfRows
        }.also {
            println("Time taken: $it")
        }
    }

    @Test
    fun `complex data class mapper should return mapped results`() {
        val numberOfRows = 1000
        val benchmark = ComplexDataClassMapperBenchmark(numberOfRows)
        benchmark.map().size shouldBe numberOfRows

        measureTime {
            benchmark.map().size shouldBe numberOfRows
        }.also {
            println("Time taken: $it")
        }
    }

    @Test
    fun `complex record mapper should return mapped results`() {
        val numberOfRows = 1000
        val benchmark = ComplexRecordMapperBenchmark(numberOfRows)
        benchmark.map().size shouldBe numberOfRows

        measureTime {
            benchmark.map().size shouldBe numberOfRows
        }.also {
            println("Time taken: $it")
        }
    }
}

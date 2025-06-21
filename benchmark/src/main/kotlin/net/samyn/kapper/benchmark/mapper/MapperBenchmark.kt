package net.samyn.kapper.benchmark.mapper

interface MapperBenchmark {
    val numberOfResults: Int

    fun map(): List<Any>
}

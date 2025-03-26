# Kapper Benchmark Suite

This module contains a set of benchmarks to evaluate the relative performance of kapper against various other database access libraries.
It uses [Java Microbenchmark Harness (JMH)](https://github.com/openjdk/jmh).

## Overview

The benchmark suite includes tests for the following libraries:
- JDBC - no non-standard libraries used, using only the [java.sql](https://docs.oracle.com/javase/8/docs/api/java/sql/package-summary.html) api and "plain" JDBC drivers.
- Kapper
- Kapper without auto-mapping of data classes.
- [Hibernate](https://hibernate.org/)
- [Ktorm](https://www.ktorm.org/)

The benchmarks are designed to measure the performance of the following common database operations for the purpose of comparison between the libraries:

- finding a record by ID
- retrieving multiple (100) records
- retrieving a single record with simple inner join
- inserting a single record
- updating a single record

The benchmark does _not_ include the time taken to set up the database connection, as this is not the focus of the benchmark.
It does include the time taken to map the results to DTO type classes, including type conversions to handle UUID and date types.

Each of the benchmarks is tested against PostgreSQL and SQLite (in-memory) databases.

## Adding Additional Libraries

To add a new library to the benchmark suite, follow these steps:

1. Implement the [`BenchmarkStrategy`](./src/main/kotlin/net/samyn/kapper/benchmark/BenchmarkStrategy.kt) Interface 
2. Update the [`KapperBenchmark`](./src/jmh/kotlin/net/samyn/kapper/benchmark/KapperBenchmark.kt) Class:
Add a case for the new library in the `setup` method of the `KapperBenchmark.BenchmarkState` class.
```kotlin
@Setup
fun setup() {
    ...
    benchmarkStrategy = when (library) {
        "JDBC" -> JDBCStrategy()
        ...
        "NEWLIBRARY" -> NewLibraryStrategy() // Add this line
        else -> throw IllegalArgumentException("Unknown ORM type: $library")
    }
}
```
3. Update the new benchmark library to the [`run.sh`](run.sh) script and update [`run-usage.txt`](run-usage.txt).

## Running the Benchmarks

To run the benchmarks, use the `run.sh` script. The script builds the JMH jar and executes the benchmarks with the specified parameters.

Use `./run.sh -h` to see the available options.

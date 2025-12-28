# Performance History

Track Kapper's performance improvements and competitive positioning across tested versions.

## Version Performance Timeline

### Kapper 1.6.1 (Latest)
**Release Date**: Aug 2025

#### Major Performance Improvements:
- **Overall improvement**: 7.5% average performance gain across 50 benchmark comparisons

#### Top Performance Gains:
- **findById vs Ktorm**: 81.9% improvement (1181.6 → 213.9 μs/op)
- **find100 operations**: 47.9% improvement (401.8 → 209.5 μs/op)  
- **findById operations**: 15.9% improvement (71.4 → 60.1 μs/op)

#### Competitive Analysis:
```
Library Comparisons (1000-row operations):
┌──────────────────┬─────────────┬─────────────┬─────────────┐
│ Operation        │ vs JDBC     │ vs Hibernate│ vs Ktorm    │
├──────────────────┼─────────────┼─────────────┼─────────────┤
│ findById (SQLite)│ 69.7% slower│ 45% faster  │ 88.3% faster│
│ find100 (SQLite) │ 61.3% slower│ 23% faster  │ 26% faster  │
│ insert (SQLite)  │ 12% slower  │ 67% faster  │ 89.8% faster│
│ update (SQLite)  │ 15% slower  │ 74% faster  │ 94.1% faster│
│ simpleJoin       │ 40% slower  │ 34% faster  │ 88.6% faster│
└──────────────────┴─────────────┴─────────────┴─────────────┘
```

### Kapper 1.5.0
**Release Date**: June 2025

#### Benchmark Results:
```
Main Operations (μs/op):
┌──────────────────┬─────────────┬─────────────┬─────────────┬─────────────┐
│ Operation        │ JDBC        │ KAPPER      │ HIBERNATE   │ KTORM       │
├──────────────────┼─────────────┼─────────────┼─────────────┼─────────────┤
│ find100 (SQLite) │ 154.6 ± 2.1 │ 401.8 ± 8.2 │ 288.9 ± 5.1 │ 389.1 ± 7.8 │
│ findById (SQLite)│ 10.9 ± 0.3  │ 71.4 ± 1.2  │ 23.7 ± 0.8  │ 1181.6 ± 45 │
│ insert (SQLite)  │ 6.2 ± 0.2   │ 7.1 ± 0.3   │ 23.1 ± 1.1  │ 162.0 ± 3.2 │
│ update (SQLite)  │ 6.7 ± 0.2   │ 7.7 ± 0.3   │ 31.8 ± 1.4  │ 149.5 ± 2.9 │
│ simpleJoin       │ 18.6 ± 0.7  │ 29.2 ± 1.1  │ 39.1 ± 1.8  │ 257.1 ± 5.4 │
└──────────────────┴─────────────┴─────────────┴─────────────┴─────────────┘
```

### Kapper 1.3.0
**Release Date**: March 2025

Available legacy data shows comprehensive performance metrics with error bars and percentile data, used for visualization testing and trend analysis.

## Benchmark Methodology

### Environment
- **Platform**: Oracle Cloud ARM instances (4 OCPUs, 24GB RAM)
- **JVM**: GraalVM JDK 21 with optimized JIT compilation
- **Framework**: JMH (Java Microbenchmark Harness)
- **Databases**: SQLite 3.50, PostgreSQL 14 (Testcontainers)

### Measurements
- **Warmup**: 5 iterations, 2 seconds each
- **Execution**: 5 iterations, 5 seconds each  
- **Mode**: Average time per operation (AVGT)
- **Statistics**: Score ± error with percentile data

### Data Integrity
- All benchmarks run on identical infrastructure
- Fresh database state for each test
- Statistical validation with confidence intervals
- Version-controlled benchmark code

## Next Steps

- [Performance Comparisons](./comparisons.md) - Detailed competitive analysis
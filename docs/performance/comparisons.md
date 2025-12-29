# Performance Comparisons

Detailed performance analysis comparing Kapper with other popular JVM database libraries using real benchmark data.

## Benchmark Overview

Our benchmarks test realistic database operations using JMH (Java Microbenchmark Harness):

- **find100** - Retrieve 100 records
- **findById** - Single record lookup by primary key
- **insertSingleRow** - Single record insertion
- **updateSingleRow** - Single record update
- **simpleJoin** - Basic JOIN query with mapping

## Libraries Tested

| Library       | Version | Type       | Notes                     |
|---------------|---------|------------|---------------------------|
| **Kapper**    | 1.6.1   | Micro ORM  | SQL-first mapping library |
| **Hibernate** | Latest  | Full ORM   | Traditional Java ORM      |
| **Ktorm**     | Latest  | Kotlin DSL | Type-safe query DSL       |
| **JDBC**      | -       | Direct API | Raw database access       |

## Test Environment

- **Platform**: Oracle Cloud ARM instances (4 OCPUs, 24GB RAM)
- **JVM**: GraalVM JDK 21 with optimised JIT compilation
- **Framework**: JMH (Java Microbenchmark Harness)
- **Database**: SQLite 3.50 (embedded), PostgreSQL 14 (Testcontainers)
- **Dataset**: 1000-row tables with realistic data structures

## SQLite Performance (Embedded Database)

### Find Operations Performance

![SQLite Find Operations](/sqlite-find-operations.png)

*SQLite find operations comparison showing Kapper's competitive positioning*

**Key Findings:**
- **JDBC maintains raw performance leadership** as expected for direct SQL
- **Kapper with auto-mapping shows excellent ORM competitiveness** especially vs Ktorm  
- **Ktorm significantly slower** in single-record operations (10x slower findById)

### Modification Operations Performance

![SQLite Modification Operations](/sqlite-modification-operations.png)

*SQLite modification and JOIN operations showing Kapper's near-JDBC performance*

**Key Findings:**
- **Kapper nearly matches JDBC performance** for modifications (10-13% overhead)
- **Dramatic advantages over ORMs** - 2-20x faster than Hibernate/Ktorm
- **Consistent low-overhead pattern** across all modification operations

## Performance Categories Summary

### Performance Trade-offs

**JDBC vs Kapper**:
- **JDBC Advantages**: Raw performance, no mapping overhead
- **Kapper Advantages**: Type safety, automatic mapping, cleaner code
- **Trade-off**: 10-70% performance cost for significant development productivity

**Kapper vs Full ORMs (Hibernate)**:
- **Kapper Advantages**: 69-282% faster, predictable performance, no lazy loading
- **ORM Advantages**: Advanced features (caching, lazy loading, change tracking)
- **Sweet Spot**: High performance with essential mapping features

**Kapper vs Kotlin DSLs (Ktorm)**:
- **Kapper Advantages**: 36-1970% faster, direct SQL control
- **DSL Advantages**: Type-safe query building, fluent API
- **Trade-off**: Kapper's SQL-first approach wins on performance

## Kapper Mapping Strategy Comparison

Within Kapper itself, different mapping approaches offer performance trade-offs:

### Mapping Strategy Performance (SQLite)

![Kapper Mapping Comparison](/kapper-mapping-comparison.png)

*Detailed comparison of Kapper's different mapping strategies showing performance trade-offs*

### Performance Impact Analysis

**Most Significant Gains**:
- **find100 (Bulk Operations)**: Manual mapping 35.5% faster than auto-mapping (161.6μs vs 250.4μs for Kotlin data classes)

**Minimal Impact Operations**:
- **Single-row modifications**: All strategies perform similarly (~0-2% difference)
- **Simple lookups**: Small but consistent manual mapping advantage (~4%)

### Strategic Mapping Choices

```kotlin
// High-frequency, performance-critical paths
connection.query("SELECT id, name FROM users") { rs, _ ->
    User(rs.getLong("id"), rs.getString("name"))
}

// Balanced performance with convenience
data class User(val id: Long, val name: String)
connection.query<User>("SELECT id, name FROM users")
```

## Next Steps

- [Performance History](./history.md) - Track performance evolution across versions
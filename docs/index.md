---
layout: home

hero:
  name: Kapper
  text: A Fresh Look at ORMs for Kotlin and the JVM
  tagline: SQL is not a problem to be solved - it's a powerful tool to be embraced
  image:
    src: /kapper-logo.png
    alt: Kapper Logo
  actions:
    - theme: brand
      text: Get Started
      link: /guide/
    - theme: alt
      text: View Examples
      link: /examples/
    - theme: alt
      text: See Performance
      link: /performance/

features:
  - icon: ⚡
    title: Lightning Fast
    details: Performance comparable to raw JDBC with the convenience of object mapping. Auto-mapping overhead in the single-digit microsecond range.
  - icon: 🎯
    title: SQL-First Philosophy
    details: Embrace SQL as the best query language. No hidden query generation or magic - what you write is what gets executed.
  - icon: 🔧
    title: Minimal Abstraction
    details: Extension functions on java.sql.Connection. Works alongside existing JDBC code without intrusion or lock-in.
  - icon: 📦
    title: Zero Dependencies
    details: Lightweight with minimal external dependencies. No heavyweight framework overhead or complex configuration.
  - icon: 🚀
    title: Modern Kotlin
    details: Idiomatic Kotlin with coroutines support, data class mapping, and Java record compatibility.
  - icon: 🗄️
    title: Database Agnostic
    details: Support for PostgreSQL, MySQL, SQLite, Oracle, MS SQL Server and probably many others.
---

## Quick Example

```kotlin
data class SuperHero(val id: UUID, val name: String, val email: String?, val age: Int?)

// Simple query
val heroes = dataSource.connection.use {
    it.query<SuperHero>("SELECT * FROM super_heroes WHERE age > :age", "age" to 30)
}

// Single result
val batman = dataSource.connection.use {
    it.querySingle<SuperHero>(
        "SELECT * FROM super_heroes WHERE name = :name", 
        "name" to "Batman"
    )
}

// Execute statement
dataSource.connection.use {
    it.execute(
        "INSERT INTO super_heroes(id, name, email, age) VALUES(:id, :name, :email, :age)",
        "id" to UUID.randomUUID(),
        "name" to "Wonder Woman",
        "email" to "wonder@dc.com", 
        "age" to 3000
    )
}
```

## Performance That Speaks for Itself

<div class="performance-showcase">
  <img src="/kapper-benchmark.png" alt="Kapper Performance Benchmarks" />
</div>

Kapper consistently outperforms other ORMs while maintaining the simplicity and transparency of SQL.

<style>
.performance-showcase {
  margin: 2rem 0;
  text-align: center;
}

.performance-showcase img {
  max-width: 100%;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}
</style>
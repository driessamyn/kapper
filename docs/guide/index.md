# Introduction

Kapper is a lightweight, Dapper-inspired ORM (Object-Relational Mapping) library written in Kotlin, targeting the JVM ecosystem.
It embraces SQL rather than abstracting it away, providing a simple, intuitive API for executing queries and mapping results.

## The Kapper Philosophy

Instead of adding another abstraction layer, Kapper embraces three core principles:

1. **SQL is the Best Query Language**: SQL has evolved over decades to be expressive, powerful, and optimised for database operations. 
Instead of hiding it, we should leverage it directly.

2. **Minimal Abstraction**: Kapper provides just enough abstraction to make database operations comfortable in Kotlin, without trying to reinvent database interactions.
Kapper prefers extension of existing APIs than abstraction of them.

3. **Transparency**: What you write is what gets executed.
There's no magic query generation or hidden database calls.

## Why Kapper?

Kapper aims to go against the grain of the heavyweight database abstractions.
Instead, it makes JDBC support easy without taking away any flexibility.
In fact, it can happily live alongside an existing _vanilla_ JDBC integration and/or your existing DB layer.

It does _not_ generate code, it does not introduce another layer of abstraction, it is never intrusive.
It does _not_ hide SQL, instead it embraces the fact that SQL is the best language for DB interaction.

## Key Features

- **Simple API**: Kapper provides a familiar set of methods for executing SQL queries, mapping results to Kotlin data classes, and updating/inserting data.
- **Extensibility**: The Kapper API is implemented as extension functions on the `java.sql.Connection` interface, allowing seamless integration with existing JDBC code.
- **Lightweight**: Kapper has minimal external dependencies, focusing on providing core functionality without bloat.
- **Fast**: Kapper's extension design means that it is possible to match "raw" JDBC performance, while its auto-mapper equals or outperforms other ORMs.
- **Supported DBs**: PostgreSQL, MySQL, SQLite, Oracle, MS SQL Server, and because it is _just_ an extension, probably many others.

## Next Steps

- [Install Kapper](./installation.md) in your project
- Follow the [Quick Start](./quick-start.md) guide
- Explore [Examples](/examples/) to see Kapper in action
- Check out [Performance benchmarks](/performance/) to see how fast Kapper is
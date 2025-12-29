# Coroutines Support

Kapper provides excellent support for Kotlin coroutines through the `kapper-coroutines` module.

## Quick Start

Add the coroutines dependency:

```kotlin
dependencies {
    implementation("net.samyn:kapper-coroutines:latest")
}
```

Use the `withConnection` extension function:

```kotlin
dataSource.withConnection { connection ->
    val heroes = connection.query<SuperHero>("SELECT * FROM super_heroes")
    // Process heroes...
}
```

## Detailed Guide

For a comprehensive guide with examples and best practices, see our detailed blog post:

**[📖 Coroutine Support in Kapper 1.1](../blog/02_coroutine_support.md)**

This covers:
- Installation and setup
- Using `withConnection`
- Sequential vs concurrent execution
- Transaction handling with coroutines
- Best practices and performance considerations

## API Reference

For complete API documentation, see:
- [**kapper-coroutines API Reference**](../api/kapper-coroutines/)

## Next Steps

- See [Transactions](./transactions.md) for coroutine transaction handling
- Explore [Performance Tuning](./performance-tuning.md) for async operation optimisation
- Learn about [Examples](../examples/) for real-world coroutine patterns
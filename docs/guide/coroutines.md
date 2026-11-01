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

## Threading and Resource Ownership

The coroutine API follows the same ownership rules as the core API, with one addition: `withConnection` manages the connection for you.

### `withConnection` owns the connection

`DataSource.withConnection { }` opens a connection, runs your block on `Dispatchers.IO` (or a context you pass), and closes the connection when the block returns — even if the block throws or is cancelled:

```kotlin
dataSource.withConnection { connection ->
    val heroes = connection.query<SuperHero>("SELECT * FROM super_heroes")
    // the connection is closed for you when this block returns
}
```

Do not let the connection escape the block. Returning it, capturing it in a longer-lived object, or launching a coroutine that outlives the block all risk using a closed connection.

### `queryAsFlow` and the result set

`Connection.queryAsFlow()` returns a `Flow` and closes the underlying result set (and statement) when the flow completes or is cancelled. It does **not** close the connection — you own that, so collect the flow while the connection is still open:

```kotlin
dataSource.withConnection { connection ->
    connection.queryAsFlow<SuperHero>("SELECT * FROM super_heroes")
        .collect { hero -> println(hero) }
}
```

### Threading

A JDBC `Connection` is not thread-safe. Do not share one connection across coroutines, and do not run concurrent queries on the same connection. `withConnection` gives each call its own connection, so the safe pattern is one connection per coroutine.

### Cancellation

When the coroutine collecting a `queryAsFlow` flow is cancelled, Kapper closes the result set and cancels the underlying statement. `queryAsFlow` sets a fetch size (1000 rows by default) so the database driver can abandon an in-flight query rather than draining the whole result set first.

## API Reference

For complete API documentation, see:
- [**kapper-coroutines API Reference**](../api/kapper-coroutines/)

## Next Steps

- See [Transactions](./transactions.md) for coroutine transaction handling
- Explore [Performance Tuning](./performance-tuning.md) for async operation optimisation
- Learn about [Examples](../examples/) for real-world coroutine patterns
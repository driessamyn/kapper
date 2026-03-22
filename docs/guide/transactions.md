# Transaction Management

Kapper provides simple transaction handling using the `withTransaction` extension function.

## Basic Transactions

```kotlin
dataSource.withTransaction { connection ->
    // All operations in this block are part of a single transaction
    connection.execute("INSERT INTO users (name) VALUES (:name)", "name" to "Alice")
    connection.execute(
        "INSERT INTO posts (user_id, title) VALUES (:userId, :title)",
        "userId" to 1, "title" to "My Post"
    )
    // Transaction is automatically committed if no exceptions are thrown
}
```

## Manual Transaction Control

The above is simply _syntax sugar_ for the code below.
Kapper cuts down on boilerplate code, but doesn't stand in the way when full control is desired.

```kotlin
connection.use { conn ->
    try {
        conn.autoCommit = false

        conn.execute("INSERT INTO users (name) VALUES (:name)", "name" to "Bob")
        conn.execute(
            "INSERT INTO posts (user_id, title) VALUES (:userId, :title)",
            "userId" to 2, "title" to "Another Post"
        )

        conn.commit()
    } catch (e: SQLException) {
        conn.rollback()
        throw e
    }
}
```

## With Coroutines

When using [coroutines](coroutines.md), transactions work seamlessly:

```kotlin
dataSource.withConnection { connection ->
    connection.withTransaction {
        // Suspendable operations within a transaction
        val user = connection.querySingle<User>("SELECT * FROM users WHERE id = :id", "id" to 1)
        connection.execute("UPDATE users SET last_seen = NOW() WHERE id = :id", "id" to user.id)
    }
}
```

## Nested Transactions

Kapper follows standard JDBC behaviour for nested transactions.
Be aware that most databases don't support true nested transactions.

## Error Handling

Transactions are automatically rolled back when exceptions are thrown:

```kotlin
dataSource.withTransaction { connection ->
    connection.execute("INSERT INTO users (name) VALUES (:name)", "name" to "Charlie")

    if (someCondition) {
        throw IllegalStateException("Rolling back transaction")
    }

    // This line won't be reached, transaction will be rolled back
    connection.execute(
        "INSERT INTO posts (user_id, title) VALUES (:userId, :title)",
        "userId" to 3, "title" to "Post"
    )
}
```

## Next Steps

- [Coroutines](./coroutines.md) - Using transactions with coroutines
- [API Reference](../api/kapper/) - Complete API documentation

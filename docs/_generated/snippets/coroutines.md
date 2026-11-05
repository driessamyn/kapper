### Coroutines

Run a query on a coroutine dispatcher.

```kotlin
suspend fun listUsers(dataSource: DataSource): List<User> =
    dataSource.withConnection { connection ->
        connection.query<User>("SELECT id, name, email, status, created_at FROM users")
    }
```

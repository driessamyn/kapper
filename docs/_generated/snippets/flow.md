### Flow

Stream results as a cold Flow.

```kotlin
suspend fun streamUsers(
    dataSource: DataSource,
    onUser: suspend (User) -> Unit,
) = dataSource.withConnection { connection ->
    // queryAsFlow is cold: the query runs when the flow is collected,
    // and the connection stays open for the duration of the collection.
    connection.queryAsFlow<User>("SELECT id, name, email, status, created_at FROM users")
        .collect { onUser(it) }
}
```

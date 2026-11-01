### Error handling

Handle a mapping failure.

```kotlin
fun safeQuery(dataSource: DataSource): List<User> =
    try {
        dataSource.connection.use {
            it.query<User>("SELECT id, name, email, status, created_at FROM users")
        }
    } catch (e: KapperMappingException) {
        // The message names the target class, the columns returned, and the mismatch.
        emptyList()
    }
```

### Error handling

Handle a mapping failure.

```kotlin
fun loadUsers(dataSource: DataSource): List<User> =
    try {
        dataSource.connection.use {
            it.query<User>("SELECT id, name, email, status, created_at FROM users")
        }
    } catch (e: KapperMappingException) {
        // The message names the target class, the columns returned and the mismatch.
        // Add context of your own and let it propagate rather than swallowing it.
        throw KapperMappingException("Could not load users: ${e.message}", e)
    }
```

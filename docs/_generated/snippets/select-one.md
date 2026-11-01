### Select one row

Return a single row, or null if none.

```kotlin
fun selectOne(connection: Connection, id: UUID): User? =
    connection.querySingle<User>(
        "SELECT id, name, email, status, created_at FROM users WHERE id = :id",
        "id" to id,
    )
```

### IN list

Expand a collection into placeholders for an IN clause.

```kotlin
fun selectByIds(connection: Connection, ids: List<UUID>): List<User> =
    connection.query<User>(
        "SELECT id, name, email, status, created_at FROM users WHERE id IN (:ids)",
        "ids" to ids,
    )
```

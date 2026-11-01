### Update

Update a row.

```kotlin
fun rename(connection: Connection, id: UUID, name: String): Int =
    connection.execute("UPDATE users SET name = :name WHERE id = :id", "name" to name, "id" to id)
```

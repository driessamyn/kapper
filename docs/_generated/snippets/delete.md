### Delete

Delete a row.

```kotlin
fun delete(
    connection: Connection,
    id: UUID,
): Int = connection.execute("DELETE FROM users WHERE id = :id", "id" to id)
```

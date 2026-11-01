### Insert and return the id

Insert a row and read back the generated id.

```kotlin
fun insertReturningId(connection: Connection, name: String): Long =
    connection.executeReturning<Long>(
        "INSERT INTO users(name) VALUES(:name) RETURNING id",
        "name" to name,
    ).single()
```

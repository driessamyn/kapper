### Insert and return the id

Insert a row and read back the generated id.

```kotlin
fun insertReturningId(
    connection: Connection,
    name: String,
): UUID =
    connection.executeReturning(
        "INSERT INTO users(name) VALUES(:name) RETURNING id",
        mapper = { rs, _ -> rs.getObject("id", UUID::class.java) },
        "name" to name,
    ).single()
```

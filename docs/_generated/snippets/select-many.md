### Select many rows

Map a result set to a list of data classes.

```kotlin
fun selectMany(
    connection: Connection,
    minAge: Int,
): List<User> =
    connection.query<User>(
        "SELECT id, name, email, status, created_at FROM users WHERE age > :age",
        "age" to minAge,
    )
```

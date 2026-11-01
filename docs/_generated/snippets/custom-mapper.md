### Custom mapper

Map a row by hand with the Row abstraction.

```kotlin
fun selectNames(connection: Connection): List<String> =
    connection.query("SELECT name FROM users") { row, _ -> row.string("name") }
```

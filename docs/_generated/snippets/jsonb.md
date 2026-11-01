### jsonb column

Map a PostgreSQL jsonb column to String.

```kotlin
// A jsonb column maps to String out of the box on PostgreSQL.
fun selectMetadata(connection: Connection): List<String> =
    connection.query("SELECT metadata FROM users") { row, _ -> row.string("metadata") }
```

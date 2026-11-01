### Select a scalar

Map a single-column result to a scalar type.

```kotlin
fun countActive(connection: Connection): Long =
    connection.queryScalar<Long>("SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'")
```

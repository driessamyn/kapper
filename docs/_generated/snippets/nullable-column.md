### Nullable column

Map a NULL column to a nullable property.

```kotlin
// email is nullable; a NULL column maps to null.
fun selectNullable(connection: Connection): List<User> =
    connection.query<User>("SELECT id, name, email, status, created_at FROM users")
```

### Enum property

Map a VARCHAR column to an enum property.

```kotlin
// Status maps from a VARCHAR column by name, case-insensitively.
fun selectWithEnum(connection: Connection): List<User> =
    connection.query<User>("SELECT id, name, email, status, created_at FROM users")
```

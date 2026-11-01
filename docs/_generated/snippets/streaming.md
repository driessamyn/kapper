### Streaming

Stream a large result set as a Sequence.

```kotlin
fun streamUsers(connection: Connection): Sequence<User> =
    connection.queryAsSequence<User>("SELECT id, name, email, status, created_at FROM users")
```

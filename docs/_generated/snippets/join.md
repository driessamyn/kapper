### Join

Map a joined result set to a flat DTO.

```kotlin
fun selectUsersWithOrderCount(connection: Connection): List<UserWithOrders> =
    connection.query<UserWithOrders>(
        """
        SELECT u.id, u.name, COUNT(o.id) AS order_count
        FROM users u
        LEFT JOIN orders o ON o.user_id = u.id
        GROUP BY u.id, u.name
        """.trimIndent(),
    )
```

### Nested transaction

A nested transaction joins the enclosing one.

```kotlin
fun nested(dataSource: DataSource, user: User) =
    dataSource.withTransaction { connection ->
        connection.execute(
            "INSERT INTO users(id, name) VALUES(:id, :name)",
            "id" to user.id,
            "name" to user.name,
        )
        // A nested withTransaction joins the enclosing transaction instead of committing it.
        connection.withTransaction {
            connection.execute(
                "INSERT INTO audit(user_id, action) VALUES(:id, 'CREATE')",
                "id" to user.id,
            )
        }
    }
```

### Transaction with a return value

Run a transaction and return a value from it.

```kotlin
fun createWithAudit(dataSource: DataSource, user: User): UUID =
    dataSource.withTransaction { connection ->
        connection.execute(
            "INSERT INTO users(id, name) VALUES(:id, :name)",
            "id" to user.id,
            "name" to user.name,
        )
        connection.execute(
            "INSERT INTO audit(user_id, action) VALUES(:id, 'CREATE')",
            "id" to user.id,
        )
        user.id
    }
```

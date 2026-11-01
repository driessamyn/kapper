### Batch insert

Insert many rows in one JDBC batch.

```kotlin
fun batchInsert(connection: Connection, users: List<User>): IntArray =
    connection.executeAll(
        "INSERT INTO users(id, name, email, status, created_at) VALUES(:id, :name, :email, :status, :createdAt)",
        users,
        "id" to User::id,
        "name" to User::name,
        "email" to User::email,
        "status" to User::status,
        "createdAt" to User::createdAt,
    )
```

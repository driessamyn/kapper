### Insert

Insert a row and return the affected count.

```kotlin
fun insert(
    connection: Connection,
    user: User,
): Int =
    connection.execute(
        "INSERT INTO users(id, name, email, status, created_at) VALUES(:id, :name, :email, :status, :createdAt)",
        "id" to user.id,
        "name" to user.name,
        "email" to user.email,
        "status" to user.status,
        "createdAt" to user.createdAt,
    )
```

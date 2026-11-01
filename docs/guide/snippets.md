# Snippets

One blessed way to do each common task. Generated from compiled code; do not edit by hand.

## Select many rows

Map a result set to a list of data classes.

```kotlin
fun selectMany(connection: Connection, minAge: Int): List<User> =
    connection.query<User>(
        "SELECT id, name, email, status, created_at FROM users WHERE age > :age",
        "age" to minAge,
    )
```

## Select one row

Return a single row, or null if none.

```kotlin
fun selectOne(connection: Connection, id: UUID): User? =
    connection.querySingle<User>(
        "SELECT id, name, email, status, created_at FROM users WHERE id = :id",
        "id" to id,
    )
```

## Select a scalar

Map a single-column result to a scalar type.

```kotlin
fun countActive(connection: Connection): Long =
    connection.queryScalar<Long>("SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'")
```

## IN list

Expand a collection into placeholders for an IN clause.

```kotlin
fun selectByIds(connection: Connection, ids: List<UUID>): List<User> =
    connection.query<User>(
        "SELECT id, name, email, status, created_at FROM users WHERE id IN (:ids)",
        "ids" to ids,
    )
```

## Join

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

## Streaming

Stream a large result set as a Sequence.

```kotlin
fun streamUsers(connection: Connection): Sequence<User> =
    connection.queryAsSequence<User>("SELECT id, name, email, status, created_at FROM users")
```

## Insert

Insert a row and return the affected count.

```kotlin
fun insert(connection: Connection, user: User): Int =
    connection.execute(
        "INSERT INTO users(id, name, email, status, created_at) VALUES(:id, :name, :email, :status, :createdAt)",
        "id" to user.id,
        "name" to user.name,
        "email" to user.email,
        "status" to user.status,
        "createdAt" to user.createdAt,
    )
```

## Insert and return the id

Insert a row and read back the generated id.

```kotlin
fun insertReturningId(connection: Connection, name: String): Long =
    connection.executeReturning<Long>(
        "INSERT INTO users(name) VALUES(:name) RETURNING id",
        "name" to name,
    ).single()
```

## Update

Update a row.

```kotlin
fun rename(connection: Connection, id: UUID, name: String): Int =
    connection.execute("UPDATE users SET name = :name WHERE id = :id", "name" to name, "id" to id)
```

## Delete

Delete a row.

```kotlin
fun delete(connection: Connection, id: UUID): Int =
    connection.execute("DELETE FROM users WHERE id = :id", "id" to id)
```

## Batch insert

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

## Transaction with a return value

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

## Nested transaction

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

## Enum property

Map a VARCHAR column to an enum property.

```kotlin
// Status maps from a VARCHAR column by name, case-insensitively.
fun selectWithEnum(connection: Connection): List<User> =
    connection.query<User>("SELECT id, name, email, status, created_at FROM users")
```

## Nullable column

Map a NULL column to a nullable property.

```kotlin
// email is nullable; a NULL column maps to null.
fun selectNullable(connection: Connection): List<User> =
    connection.query<User>("SELECT id, name, email, status, created_at FROM users")
```

## Custom mapper

Map a row by hand with the Row abstraction.

```kotlin
fun selectNames(connection: Connection): List<String> =
    connection.query("SELECT name FROM users") { row, _ -> row.string("name") }
```

## jsonb column

Map a PostgreSQL jsonb column to String.

```kotlin
// A jsonb column maps to String out of the box on PostgreSQL.
fun selectMetadata(connection: Connection): List<String> =
    connection.query("SELECT metadata FROM users") { row, _ -> row.string("metadata") }
```

## Coroutines

Run a query on a coroutine dispatcher.

```kotlin
suspend fun listUsers(dataSource: DataSource): List<User> =
    dataSource.withConnection { connection ->
        connection.query<User>("SELECT id, name, email, status, created_at FROM users")
    }
```

## Flow

Stream results as a cold Flow.

```kotlin
suspend fun streamUsers(dataSource: DataSource, onUser: suspend (User) -> Unit) =
    dataSource.withConnection { connection ->
        // queryAsFlow is cold: the query runs when the flow is collected,
        // and the connection stays open for the duration of the collection.
        connection.queryAsFlow<User>("SELECT id, name, email, status, created_at FROM users")
            .collect { onUser(it) }
    }
```

## Connection setup

Create a pooled DataSource.

```kotlin
fun dataSource(url: String, user: String, secret: String): DataSource =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = url
            username = user
            password = secret
        },
    )
```

## Error handling

Handle a mapping failure.

```kotlin
fun safeQuery(dataSource: DataSource): List<User> =
    try {
        dataSource.connection.use {
            it.query<User>("SELECT id, name, email, status, created_at FROM users")
        }
    } catch (e: KapperMappingException) {
        // The message names the target class, the columns returned, and the mismatch.
        emptyList()
    }
```

## Testing

Use the in-memory test harness.

```kotlin
// kapper-test provides an in-memory harness so tests need no Docker.
fun testHarness(): DataSource =
    KapperTest.dataSource(
        ddl = "CREATE TABLE users(id UUID PRIMARY KEY, name VARCHAR(100) NOT NULL)",
    )
```

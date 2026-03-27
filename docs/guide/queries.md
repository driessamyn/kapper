# Queries

Kapper provides a simple yet powerful API for executing SQL queries and mapping results to Kotlin objects.
This guide covers all aspects of querying with Kapper.

## Basic Query Operations

### The `query()` Function

The `query()` function executes a SELECT statement and returns a list of mapped objects:

```kotlin
data class User(val id: Long, val name: String, val email: String)

// Basic query
val users = connection.query<User>("SELECT * FROM users")

// Query with parameters
val activeUsers = connection.query<User>(
    "SELECT * FROM users WHERE active = :active",
    "active" to true
)
```

### The `querySingle()` Function

Use `querySingle()` when you expect at most one result:

```kotlin
// Returns User or null if not found
val user = connection.querySingle<User>(
    "SELECT * FROM users WHERE id = :id",
    "id" to 123
)

// Count query
val userCount = connection.querySingle<Int>(
    "SELECT COUNT(*) FROM users WHERE active = :active",
    "active" to true
) ?: 0
```

## Parameter Binding

Kapper supports parameter binding through named parameters:

```kotlin
val users = connection.query<User>(
    "SELECT * FROM users WHERE age > :min_age AND city = :city",
    "min_age" to 18,
    "city" to "New York"
)
```

### Array Parameters

Pass arrays (as `List<T>`) to queries on PostgreSQL and DuckDB:

```kotlin
val userIds = listOf(1, 2, 3, 4, 5)

val users = connection.query<User>(
    "SELECT * FROM users WHERE id = ANY(:ids)",
    "ids" to userIds
)

val tags = listOf("featured", "sale", "new")
val products = connection.query<Product>(
    "SELECT * FROM products WHERE tags && :tags",
    "tags" to tags
)
```

**Important:**
- Arrays are only supported on PostgreSQL and DuckDB
- Cannot pass empty lists (element type cannot be inferred)
- Use `List<Int>`, `List<String>`, etc., not primitive arrays like `IntArray`
- See [Array Types](./mapping.md#array-types) for supported element types

## Complex Queries

Kapper doesn't limit your SQL capabilities.
For example:

### Joins and Aggregations

```kotlin
data class UserWithStats(
    val id: Long,
    val name: String,
    val email: String,
    val orderCount: Int,
    val totalSpent: Double
)

val userStats = connection.query<UserWithStats>("""
    SELECT 
        u.id,
        u.name,
        u.email,
        COUNT(o.id) as order_count,
        COALESCE(SUM(o.total), 0) as total_spent
    FROM users u
    LEFT JOIN orders o ON u.id = o.user_id
    WHERE u.created_at > :since
    GROUP BY u.id, u.name, u.email
    HAVING COUNT(o.id) > :min_orders
    ORDER BY total_spent DESC
    LIMIT :limit
""", 
    "since" to LocalDate.now().minusMonths(6),
    "min_orders" to 5,
    "limit" to 100
)
```

### Window Functions

```kotlin
data class UserRanking(
    val id: Long,
    val name: String,
    val score: Int,
    val rank: Int
)

val rankings = connection.query<UserRanking>("""
    SELECT 
        id,
        name,
        score,
        RANK() OVER (ORDER BY score DESC) as rank
    FROM users
    WHERE score > 0
    ORDER BY rank
""")
```

### Common Table Expressions (CTEs)

```kotlin
data class CategorySales(
    val category: String,
    val totalSales: Double,
    val avgOrderValue: Double
)

val categorySales = connection.query<CategorySales>("""
    WITH order_totals AS (
        SELECT 
            p.category,
            o.total
        FROM orders o
        JOIN order_items oi ON o.id = oi.order_id
        JOIN products p ON oi.product_id = p.id
        WHERE o.created_at >= :start_date
    )
    SELECT 
        category,
        SUM(total) as total_sales,
        AVG(total) as avg_order_value
    FROM order_totals
    GROUP BY category
    ORDER BY total_sales DESC
""", "start_date" to startDate)
```

## Custom Result Mapping

### Manual Row Mapping

For maximum performance or complex mapping logic:

```kotlin
val users = connection.query(
    "SELECT id, name, email, created_at FROM users"
) { rs, _ ->
    User(
        id = rs.getLong("id"),
        name = rs.getString("name"),
        email = rs.getString("email"),
        createdAt = rs.getTimestamp("created_at").toInstant()
    )
}
```

### Scalar Results

For simple scalar queries:

```kotlin
val userEmails = connection.query<String>("SELECT email FROM users WHERE active = true")

val orderTotals = connection.query<BigDecimal>(
    "SELECT total FROM orders WHERE user_id = :userId",
    "userId" to 123
)
```

See [mapping](mapping.md) for more detail and more advanced mapping support.

## DML Statements that Return Records

Some databases support DML statements (INSERT, UPDATE, DELETE) that return the affected records.
Kapper provides `executeReturning()` for this pattern, which executes the statement and maps the returned rows — without the fragility of routing DML through a query function.

> **Note:** Support for this feature varies by database. Check your database's documentation to confirm support and syntax.

### Basic Usage

```kotlin
data class User(val id: Long, val name: String, val email: String)

val inserted: List<User> = connection.executeReturning<User>(
    "INSERT INTO users(id, name, email) VALUES(:id, :name, :email) RETURNING id, name, email",
    "id" to 1L,
    "name" to "Alice",
    "email" to "alice@example.com"
)
```

### Object-Based Parameters

Pass an object along with argument mappers, the same way as `execute()`:

```kotlin
data class User(val id: Long, val name: String, val email: String)

val user = User(id = 1L, name = "Alice", email = "alice@example.com")
val updated: List<User> = connection.executeReturning(
    "UPDATE users SET email = :email WHERE id = :id RETURNING id, name, email",
    obj = user,
    "id" to User::id,
    "email" to User::email
)
```

### Different Argument and Return Types

The argument object type and returned row type can differ — for example passing a request object and receiving a domain model result:

```kotlin
data class CreateUserRequest(val name: String, val email: String)
data class User(val id: Long, val name: String, val email: String)

val request = CreateUserRequest(name = "Alice", email = "alice@example.com")
val created: List<User> = connection.executeReturning<User, CreateUserRequest>(
    "INSERT INTO users(name, email) VALUES(:name, :email) RETURNING id, name, email",
    obj = request,
    "name" to CreateUserRequest::name,
    "email" to CreateUserRequest::email
)
```

### Custom Mapper

Use a custom mapper for full control over result mapping:

```kotlin
val names: List<String> = connection.executeReturning(
    "INSERT INTO users(id, name, email) VALUES(:id, :name, :email) RETURNING name",
    mapper = { rs, _ -> rs.getString("name") },
    "id" to 1L,
    "name" to "Alice",
    "email" to "alice@example.com"
)
```

### Java API

```java
List<User> result = Kapper.getInstance().executeReturning(
    User.class,
    connection,
    "INSERT INTO users(name, email) VALUES(:name, :email) RETURNING id, name, email",
    Map.of("name", "Alice", "email", "alice@example.com")
);
```

## Error Handling

```kotlin
try {
    val users = connection.query<User>(
        "SELECT * FROM users WHERE department_id = :deptId",
        "deptId" to departmentId
    )
    
    if (users.isEmpty()) {
        logger.info("No users found for department $departmentId")
    }
    
} catch (e: SQLException) {
    logger.error("Database error while querying users", e)
    // Handle database errors
} catch (e: KapperMappingException) {
    logger.error("Mapping error while processing user results", e)
    // Handle mapping errors
}
```

## Next Steps

- Learn about [Object Mapping](./mapping.md) for advanced mapping scenarios
- Explore [Transactions](./transactions.md) for consistent multi-query operations
- Check out [Performance Tuning](./performance-tuning.md) for optimization strategies
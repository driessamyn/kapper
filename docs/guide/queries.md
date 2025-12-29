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
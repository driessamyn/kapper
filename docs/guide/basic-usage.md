# Basic Usage

This section covers the fundamental operations you'll use with Kapper.

## Core API

Kapper's API is implemented as extension functions on `java.sql.Connection`, providing two main categories of functions:

- **Query functions**: `query()` and `querySingle()` for SELECT operations
- **Execute functions**: `execute()` and `executeAll()` for DML operations (INSERT, UPDATE, DELETE)

## Query Operations

### Basic Queries

```kotlin
data class User(val id: Int, val name: String, val email: String)

// Select all users
val users = connection.query<User>("SELECT * FROM users")

// Query with parameters
val activeUsers = connection.query<User>(
    "SELECT * FROM users WHERE status = :status",
    "status" to "active"
)
```

### Single Result Queries

```kotlin
// Find by ID
val user = connection.querySingle<User>(
    "SELECT * FROM users WHERE id = :id",
    "id" to 123
)

// Returns null if not found
val maybeUser = connection.querySingle<User>(
    "SELECT * FROM users WHERE email = :email",
    "email" to "nonexistent@example.com"
) // Returns null
```

### Complex Queries

Kapper doesn't limit your SQL - write any query you need:

```kotlin
data class UserStats(val name: String, val orderCount: Int, val totalSpent: Double)

val stats = connection.query<UserStats>(
    """
    SELECT u.name, 
           COUNT(o.id) as order_count,
           COALESCE(SUM(o.total), 0) as total_spent
    FROM users u
    LEFT JOIN orders o ON u.id = o.user_id
    WHERE u.created_at > :since
    GROUP BY u.id, u.name
    HAVING COUNT(o.id) > :min_orders
    ORDER BY total_spent DESC
    """,
    "since" to LocalDate.now().minusMonths(6),
    "min_orders" to 5
)
```

## Execute Operations

### Basic DML

```kotlin
// Insert
val rowsAffected = connection.execute(
    "INSERT INTO users(name, email) VALUES(:name, :email)",
    "name" to "John Doe",
    "email" to "john@example.com"
)

// Update
connection.execute(
    "UPDATE users SET email = :email WHERE id = :id",
    "email" to "newemail@example.com",
    "id" to 123
)

// Delete
connection.execute(
    "DELETE FROM users WHERE id = :id",
    "id" to 123
)
```

### Execute with DTOs

Pass entire objects to execute functions:

```kotlin
data class User(val id: Int?, val name: String, val email: String, val age: Int)

val user = User(null, "Jane Smith", "jane@example.com", 25)

connection.execute(
    "INSERT INTO users(name, email, age) VALUES(:name, :email, :age)",
    user,
    "name" to User::name,
    "email" to User::email,
    "age" to User::age
)
```

### Bulk Operations

For bulk inserts or updates, use `executeAll()`:

```kotlin
val users = listOf(
    User(null, "User 1", "user1@example.com", 25),
    User(null, "User 2", "user2@example.com", 30),
    User(null, "User 3", "user3@example.com", 35)
)

val results = connection.executeAll(
    "INSERT INTO users(name, email, age) VALUES(:name, :email, :age)",
    users,
    "name" to User::name,
    "email" to User::email,
    "age" to User::age
)
// results is IntArray with affected rows for each execution
```

## Auto-Mapping

Kapper automatically maps result sets to your data classes by matching column names to constructor parameters:

```kotlin
data class Product(
    val id: Long,
    val name: String,
    val price: BigDecimal,
    val categoryId: Long
)

// Automatically maps columns: id, name, price, category_id
val products = connection.query<Product>(
    "SELECT id, name, price, category_id FROM products"
)
```

### Handling Mismatched Names

Use column aliases when database columns don't match your property names:

```kotlin
data class ProductView(val id: Long, val productName: String, val cost: BigDecimal)

val products = connection.query<ProductView>(
    "SELECT id, name as product_name, price as cost FROM products"
)
```

### Default Values and Nullables

Kapper handles default values and nullable fields gracefully:

```kotlin
data class User(
    val id: Long,
    val name: String,
    val email: String? = null,    // Nullable - will be null if column is missing/null
    val status: String = "active" // Default value used if column is missing
)
```

## Error Handling

```kotlin
try {
    val user = connection.querySingle<User>(
        "SELECT * FROM users WHERE id = :id",
        "id" to 999
    )
    // Handle case where user is null
    if (user == null) {
        println("User not found")
    }
} catch (e: SQLException) {
    println("Database error: ${e.message}")
} catch (e: KapperMappingException) {
    println("Mapping error: ${e.message}")
}
```

## Next Steps

- Explore [Transactions](./transactions.md) for multi-operation consistency
- See [Database Support](./database-support.md) for database-specific features
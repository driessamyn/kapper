# Philosophy

Kapper is built on a set of core principles that guide its design and development.
Understanding these principles will help you use Kapper effectively and appreciate why certain design decisions were made.

## Core Principles

### 1. SQL is the Best Query Language

**SQL has evolved for decades to be the optimal language for database operations.**

Rather than hiding SQL behind abstractions, Kapper embraces it:

```kotlin
// ✅ Kapper approach: Direct SQL
val users = connection.query<User>("""
    SELECT u.id, u.name, u.email,
           COUNT(o.id) as order_count
    FROM users u
    LEFT JOIN orders o ON u.id = o.user_id  
    WHERE u.active = true
    GROUP BY u.id, u.name, u.email
    HAVING COUNT(o.id) > 5
    ORDER BY order_count DESC
""")

// ❌ Other ORMs: Hidden, complex abstractions
val users = userRepository
    .findActiveUsers()
    .withOrders()
    .havingOrderCountGreaterThan(5)
    .orderByOrderCountDesc()
```

**Benefits of the SQL-first approach:**

- **Predictable performance** - You see exactly what queries execute
- **Full database feature access** - Use window functions, CTEs, database-specific features
- **Easy optimization** - Direct control over query plans and indexes
- **No learning curve** - If you know SQL, you know how to use Kapper
- **Easy debugging** - just paste the SQL query into your favourite DB UI or CLI, no special extensions needed

### 2. Minimal Abstraction

**Kapper provides just enough abstraction to make JDBC comfortable, without hiding the underlying mechanisms.**

```kotlin
// Kapper extends existing JDBC APIs
fun Connection.query<T>(...): List<T>
fun Connection.querySingle<T>(...): T?
fun Connection.execute(...): Long
```

This approach means:

- **Small learning curve** - Familiar JDBC concepts with convenience
- **Easy debugging** - Stack traces point to your code, not framework internals  
- **Interoperability** - Works alongside existing JDBC code
- **Flexibility** - Drop down to raw JDBC when needed, or complement existing libraries

### 3. Transparency

**What you write is what gets executed. No magic, no hidden behaviour.**

```kotlin
// This code...
val user = connection.querySingle<User>(
    "SELECT * FROM users WHERE id = :id",
    "id" to 123
)

// ...does exactly what it says:
// 1. Prepares the SQL statement
// 2. Binds parameter 123 to :id
// 3. Executes the query  
// 4. Maps the single result to User
// 5. Returns User or null
```

**No hidden costs:**

- No lazy loading that triggers unexpected queries
- No automatic query generation that might be inefficient  
- No caching layers that can become stale
- No connection management magic that can cause leaks

### 4. Performance by Design

**Kapper is designed to be as fast as hand-written JDBC while providing convenience.**

#### Efficient Auto-Mapping

```kotlin
// Kapper's auto-mapping is optimised:
// - Reflection is cached after first use
// - Column index lookups are optimised
// - Minimal object allocation during mapping

val users = connection.query<User>("SELECT * FROM users")
```

#### Connection Management

```kotlin
// Kapper extends Connection directly - no connection wrapping
dataSource.connection.use { connection ->
    val users = connection.query<User>("SELECT * FROM users")
    // Uses the raw Connection, no proxy overhead or new APIs to learn
}
```

#### Predictable Resource Usage

```kotlin
// Memory usage is predictable and minimal:
// - No object caching by default
// - No proxy objects or lazy loading
// - Explicit result set processing
```

### 5. Kotlin-First Design

**Kapper leverages Kotlin's strengths while remaining Java-compatible.**

#### Idiomatic Kotlin

```kotlin
// Nullable types for proper null handling
data class User(
    val id: Long,
    val name: String,
    val email: String? = null  // Handles NULL columns gracefully
)

// Extension functions for natural APIs
fun DataSource.withTransaction<T>(block: (Connection) -> T): T

// Coroutines support (with kapper-coroutines)
suspend fun getUsers(): List<User> {
    return dataSource.withConnection { connection ->
        connection.query<User>("SELECT * FROM users")
    }
}
```

#### Java Compatibility

```java
// Works seamlessly with Java
public List<User> getUsers() {
    try (Connection conn = dataSource.getConnection()) {
        return KapperJava.query(conn, "SELECT * FROM users", User.class);
    }
}

// Java Records support
public record User(Long id, String name, String email) {}
```

## Design Decisions

### Why Not a Query Builder?

Query builders add complexity and often generate suboptimal SQL:

```kotlin
// Query builder approach (not Kapper)
query.select("users.*")
     .leftJoin("orders", "users.id = orders.user_id")
     .where("users.active = ?", true)
     .groupBy("users.id")
     .having("COUNT(orders.id) > ?", 5)
     .orderBy("COUNT(orders.id) DESC")

// Problems:
// - Verbose for complex queries
// - Type-unsafe (strings everywhere)  
// - Generated SQL might not be optimal
// - Hard to use database-specific features
// - Another API to learn
```

Kapper's approach is more direct:

```kotlin
// Kapper approach: Direct SQL
val users = connection.query<User>("""
    SELECT u.* FROM users u
    LEFT JOIN orders o ON u.id = o.user_id
    WHERE u.active = true
    GROUP BY u.id
    HAVING COUNT(o.id) > 5
    ORDER BY COUNT(o.id) DESC
""")

// Benefits:
// - Clear and concise
// - Full SQL feature access
// - Optimal query plans
// - Easy to optimise
// - Stick to the one DB query language which you will need to learn anyway: SQL
```

### Why Not Active Record?

Active Record patterns mix data and behaviour, leading to complex object hierarchies:

```kotlin
// Active Record approach (not Kapper)
class User : ActiveRecord {
    fun orders(): List<Order> = find("orders").where("user_id", id)
    fun save(): Boolean = persist()
    fun delete(): Boolean = destroy()
}

// Problems:
// - Objects become heavy with database logic
// - Hard to test (database coupling)
// - Performance issues (hidden queries)
// - Complex inheritance hierarchies
```

Kapper separates concerns:

```kotlin
// Kapper approach: Simple data classes or DTOs
data class User(val id: Long, val name: String, val email: String)

// Separate repository for data access
class UserRepository(private val dataSource: DataSource) {
    fun findById(id: Long): User? = dataSource.connection.use { connection ->
        connection.querySingle<User>("SELECT * FROM users WHERE id = :userId", "userId" to id)
    }
    
    fun save(user: User): Long = dataSource.connection.use { connection ->
        connection.execute("INSERT INTO users (name, email) VALUES (:name, :email)", 
            "name" to user.name, 
            "email" to user.email)
    }
}

// Benefits:
// - Simple, testable objects
// - Clear separation of concerns
// - Explicit database operations
// - Easy to reason about
```

### Why No Automatic Relationships?

Automatic relationship loading leads to unpredictable performance:

```kotlin
// ORM approach (not Kapper)
@Entity
class User {
    @OneToMany(fetch = FetchType.LAZY)
    List<Order> orders; // Triggers query when accessed
}

user.getOrders(); // Hidden database query!

// Problems:
// - N+1 query problems
// - Unpredictable performance  
// - Hard to optimise
// - Hidden complexity
```

Kapper makes relationships explicit:

```kotlin
// Kapper approach: Explicit queries
data class User(val id: Long, val name: String)
data class Order(val id: Long, val userId: Long, val total: BigDecimal)

// Explicit relationship loading
fun getUserWithOrders(userId: Long): UserWithOrders? {
    return dataSource.connection.use { connection ->
        val user = connection.querySingle<User>(
            "SELECT * FROM users WHERE id = :userId", 
            "userId" to userId
        ) ?: return null

        val orders = connection.query<Order>(
            "SELECT * FROM orders WHERE user_id = :userId", 
            "userId" to userId
        )

        UserWithOrders(user, orders)
    }
}

// or more efficiently
fun getUserOrders(userId: Long): List<UserOrder> {
    return dataSource.connection.use { connection ->
        connection.query<UserOrder>(
            """
            SELECT *
            FROM orders o
            INNER JOIN users u on o.user_id = u.id
            WHERE u.id = :userId
            """,
            "userId" to userId
        )
    }
}

// Benefits:
// - Explicit performance characteristics
// - No hidden queries
// - Easy to optimise (combine queries, add indexes)
// - Clear data loading strategy
```

## Philosophy in Practice

These principles guide every aspect of Kapper's design:

### API Design
- Extension functions over new classes
- Familiar JDBC concepts
- Minimal method overloading

### Error Handling
- Clear, actionable error messages
- Preserve original SQL exceptions
- Fail fast with helpful context

### Documentation
- SQL examples for every feature
- Performance characteristics clearly stated
- Real-world usage patterns

### Community
- Open source with permissive license
- Clear contribution guidelines
- Responsive to user feedback

## Conclusion

Kapper's philosophy prioritizes **simplicity, transparency, and performance** over feature richness. This makes it ideal for teams that value:

- **Control** over their data access layer
- **Performance** that scales with their application
- **Simplicity** that reduces maintenance burden
- **SQL skills** as a core competency

By embracing these principles, Kapper helps you build robust, performant applications while keeping your database layer simple and maintainable.

## Next Steps

- [Quick Start](./quick-start.md) - See the philosophy in action
- [Basic Usage](./basic-usage.md) - Learn the core APIs
- [Performance](../performance/) - Understand Kapper's performance characteristics
# Migration Guide

This guide helps you migrate to Kapper from other database libraries and frameworks.
It provides practical examples and migration strategies for common scenarios.

## Migrating from Raw JDBC

If you're currently using raw JDBC, Kapper provides a smooth migration path with minimal changes.

### Before: Raw JDBC

```java
// Raw JDBC code
public List<User> getActiveUsers() {
    List<User> users = new ArrayList<>();
    String sql = "SELECT id, name, email FROM users WHERE active = true";
    
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
        
        while (rs.next()) {
            User user = new User(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("email")
            );
            users.add(user);
        }
    } catch (SQLException e) {
        throw new RuntimeException("Failed to fetch users", e);
    }
    
    return users;
}
```

### After: Kapper

```kotlin
// Kapper equivalent
fun getActiveUsers(): List<User> {
    return dataSource.connection.use { connection ->
        connection.query<User>("SELECT id, name, email FROM users WHERE active = true")
    }
}
```

### Migration Strategy for JDBC

1. **Keep your existing SQL** - Kapper doesn't require changing your queries
2. **Add Kapper dependency** gradually to new features
3. **Migrate method by method** rather than all at once
4. **Leverage existing connection pooling** - Kapper works with your current DataSource

```kotlin
// Gradual migration: Mix JDBC and Kapper in the same class
class UserService(private val dataSource: DataSource) {
    
    // Legacy JDBC method (keep as-is during migration)
    fun getUserByIdLegacy(id: Long): User? {
        // ... existing JDBC code
    }
    
    // New Kapper method
    fun getUserById(id: Long): User? {
        return dataSource.connection.use { connection ->
            connection.querySingle<User>(
                "SELECT * FROM users WHERE id = :id",
                "id" to id
            )
        }
    }
}
```

## Migrating from Hibernate/JPA

Hibernate migration requires more significant changes since you'll be moving from an ORM to a SQL-first approach.

### Before: Hibernate/JPA

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "email")
    private String email;
    
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();
    
    // constructors, getters, setters...
}

@Repository
public class UserRepository {
    @PersistenceContext
    private EntityManager entityManager;
    
    public List<User> findActiveUsers() {
        return entityManager
            .createQuery("SELECT u FROM User u WHERE u.active = true", User.class)
            .getResultList();
    }
    
    public User findByEmail(String email) {
        return entityManager
            .createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
            .setParameter("email", email)
            .getSingleResult();
    }
}
```

### After: Kapper

```kotlin
// Simple data class (no annotations needed)
data class User(
    val id: Long?,
    val name: String,
    val email: String,
    val active: Boolean = true
)

class UserRepository(private val dataSource: DataSource) {
    
    fun findActiveUsers(): List<User> {
        return dataSource.connection.use { connection ->
            connection.query<User>("SELECT id, name, email, active FROM users WHERE active = true")
        }
    }
    
    fun findByEmail(email: String): User? {
        return dataSource.connection.use { connection ->
            connection.querySingle<User>(
                "SELECT id, name, email, active FROM users WHERE email = :email",
                "email" to email
            )
        }
    }
}
```

## Migrating from Spring Data JPA

Spring Data JPA repositories can be replaced with simpler Kapper repositories:

### Before: Spring Data JPA

```java
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByActiveTrue();
    
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);
    
    @Query("SELECT u FROM User u WHERE u.createdAt > :date")
    List<User> findRecentUsers(@Param("date") LocalDateTime date);
}

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    public List<User> getActiveUsers() {
        return userRepository.findByActiveTrue();
    }
}
```

### After: Kapper

```kotlin
class UserRepository(private val dataSource: DataSource) {
    
    fun findByActiveTrue(): List<User> {
        return dataSource.connection.use { connection ->
            connection.query<User>("SELECT * FROM users WHERE active = true")
        }
    }
    
    fun findByEmail(email: String): User? {
        return dataSource.connection.use { connection ->
            connection.querySingle<User>(
                "SELECT * FROM users WHERE email = :email",
                "email" to email
            )
        }
    }
    
    fun findRecentUsers(date: LocalDateTime): List<User> {
        return dataSource.connection.use { connection ->
            connection.query<User>(
                "SELECT * FROM users WHERE created_at > :date",
                "date" to date
            )
        }
    }
}

class UserService(private val userRepository: UserRepository) {
    
    fun getActiveUsers(): List<User> {
        return userRepository.findByActiveTrue()
    }
}
```

## Migrating from jOOQ

jOOQ users will find Kapper familiar but simpler and with clear visibility of the SQL that is executed:

### Before: jOOQ

```java
// Generated classes and complex DSL
public List<User> getActiveUsers() {
    return create
        .select(USERS.ID, USERS.NAME, USERS.EMAIL)
        .from(USERS)
        .where(USERS.ACTIVE.eq(true))
        .fetchInto(User.class);
}

public UserStats getUserStats(long userId) {
    return create
        .select(
            USERS.ID,
            USERS.NAME,
            count(ORDERS.ID).as("order_count"),
            sum(ORDERS.TOTAL).as("total_spent")
        )
        .from(USERS)
        .leftJoin(ORDERS).on(USERS.ID.eq(ORDERS.USER_ID))
        .where(USERS.ID.eq(userId))
        .groupBy(USERS.ID, USERS.NAME)
        .fetchOneInto(UserStats.class);
}
```

### After: Kapper

```kotlin
fun getActiveUsers(): List<User> {
    return dataSource.connection.use { connection ->
        connection.query<User>("SELECT id, name, email FROM users WHERE active = true")
    }
}

fun getUserStats(userId: Long): UserStats? {
    return dataSource.connection.use { connection ->
        connection.querySingle<UserStats>("""
            SELECT 
                u.id,
                u.name,
                COUNT(o.id) as order_count,
                COALESCE(SUM(o.total), 0) as total_spent
            FROM users u
            LEFT JOIN orders o ON u.id = o.user_id
            WHERE u.id = :userId
            GROUP BY u.id, u.name
        """, "userId" to userId)
    }
}
```

### Benefits of Moving from jOOQ

- **No code generation** required
- **Simpler dependency management**
- **Direct SQL** - what you write is what executes
- **Better IDE support** for SQL strings
- **Smaller runtime footprint**

## Migrating from Ktorm

Ktorm and Kapper are both Kotlin-focused, but with different philosophies:

### Before: Ktorm

```kotlin
object Users : Table<User>("users") {
    val id = long("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
    val email = varchar("email").bindTo { it.email }
    val active = boolean("active").bindTo { it.active }
}

fun getActiveUsers(): List<User> {
    return database
        .from(Users)
        .select()
        .where { Users.active eq true }
        .map { Users.createEntity(it) }
}
```

### After: Kapper

```kotlin
// No table definitions needed
data class User(val id: Long, val name: String, val email: String, val active: Boolean)

fun getActiveUsers(): List<User> {
    return dataSource.connection.use { connection ->
        connection.query<User>("SELECT id, name, email, active FROM users WHERE active = true")
    }
}
```

## Common Migration Patterns

### 1. Repository Pattern

Create a base repository to reduce boilerplate:

```kotlin
abstract class BaseRepository<T>(
    protected val dataSource: DataSource,
    private val tableName: String
) {
    inline fun <reified T> findAll(): List<T> {
        return dataSource.connection.use { connection ->
            connection.query<T>("SELECT * FROM $tableName")
        }
    }
    
    inline fun <reified T> findById(id: Long): T? {
        return dataSource.connection.use { connection ->
            connection.querySingle<T>(
                "SELECT * FROM $tableName WHERE id = :id",
                "id" to id
            )
        }
    }
}

class UserRepository(dataSource: DataSource) : BaseRepository<User>(dataSource, "users") {
    
    fun findByEmail(email: String): User? {
        return dataSource.connection.use { connection ->
            connection.querySingle<User>(
                "SELECT * FROM users WHERE email = :email",
                "email" to email
            )
        }
    }
}
```

### 2. Service Layer with Transactions

```kotlin
class UserService(private val dataSource: DataSource) {
    
    fun createUserWithProfile(userData: CreateUserRequest): User {
        return dataSource.withTransaction { connection ->
            val userId = connection.execute(
                "INSERT INTO users (name, email) VALUES (:name, :email) RETURNING id",
                "name" to userData.name,
                "email" to userData.email
            )
            
            connection.execute(
                "INSERT INTO user_profiles (user_id, bio) VALUES (:userId, :bio)",
                "userId" to userId,
                "bio" to userData.bio
            )
            
            connection.querySingle<User>(
                "SELECT * FROM users WHERE id = :id",
                "id" to userId
            )!!
        }
    }
}
```

## Getting Help

- Check the [Kapper documentation](/guide/) for detailed API information
- See [examples](/examples/) for common patterns
- Join the community discussions on GitHub
- Compare with similar libraries in [performance benchmarks](/performance/)

Ready to start your migration? Begin with the [Quick Start Guide](../guide/quick-start.md) and [Basic Usage](../guide/basic-usage.md) documentation.
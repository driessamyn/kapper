# Kapper vs Hibernate

A comparison between Kapper and Hibernate for different use cases.

> **📁 Source Code**: View complete comparison examples in the [comparison](https://github.com/driessamyn/kapper/tree/main/examples/kotlin-example/src/test/kotlin/net/samyn/kapper/example/kotlin/comparison) directory.

## Philosophy

| Aspect             | Kapper                 | Hibernate                 |
|--------------------|------------------------|---------------------------|
| **Approach**       | Lightweight, SQL-first | Full ORM with abstraction |
| **Learning Curve** | Minimal                | Steep                     |
| **SQL Control**    | Full control           | Hidden/generated/magic    |
| **Performance**    | Predictable            | Complex caching           |
| **Code Style**     | Functional             | Object-oriented           |

## Basic Entity Mapping

### Kapper
```kotlin
data class User(
    val id: Long? = null,
    val name: String,
    val email: String
)

// Direct SQL queries
val users = connection.query<User>("SELECT * FROM users WHERE active = true")
val user = connection.querySingle<User>("SELECT * FROM users WHERE id = ?", 1)
```

### Hibernate
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(unique = true)
    private String email;
    
    // Getters, setters, constructors...
}

// Repository with generated queries
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByActiveTrue();
    Optional<User> findById(Long id);
}
```

## Relationships

### Kapper - Explicit Joins
```kotlin
data class UserWithPosts(
    val userId: Long,
    val userName: String,
    val postId: Long?,
    val postTitle: String?
)

val results = connection.query<UserWithPosts>("""
    SELECT 
        u.id as userId, u.name as userName,
        p.id as postId, p.title as postTitle
    FROM users u 
    LEFT JOIN posts p ON u.id = p.user_id
    WHERE u.id = ?
""", userId)
```

### Hibernate - Automatic Loading
```java
@Entity
public class User {
    // ...
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Post> posts = new ArrayList<>();
}

@Entity
public class Post {
    // ...
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}

// Usage - potential N+1 queries
User user = userRepository.findById(1L).get();
List<Post> posts = user.getPosts(); // Lazy loading
```

## Complex Queries

### Kapper - Raw SQL
```kotlin
val stats = connection.query<UserStats>("""
    SELECT 
        u.id,
        u.name,
        COUNT(p.id) as postCount,
        AVG(p.rating) as avgRating,
        MAX(p.created_at) as lastPostDate
    FROM users u
    LEFT JOIN posts p ON u.id = p.user_id
    WHERE u.created_at > ?
    GROUP BY u.id, u.name
    HAVING COUNT(p.id) > 5
    ORDER BY avgRating DESC, postCount DESC
    LIMIT 10
""", cutoffDate)
```

### Hibernate - JPQL/Criteria API
```java
// JPQL
@Query("""
    SELECT new com.example.UserStats(
        u.id, u.name, COUNT(p.id), AVG(p.rating), MAX(p.createdAt)
    )
    FROM User u LEFT JOIN u.posts p 
    WHERE u.createdAt > :cutoffDate
    GROUP BY u.id, u.name
    HAVING COUNT(p.id) > 5
    ORDER BY AVG(p.rating) DESC, COUNT(p.id) DESC
    """)
List<UserStats> findActiveUserStats(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);

// Or Criteria API (much more verbose)
CriteriaBuilder cb = entityManager.getCriteriaBuilder();
CriteriaQuery<UserStats> query = cb.createQuery(UserStats.class);
Root<User> user = query.from(User.class);
Join<User, Post> posts = user.join("posts", JoinType.LEFT);
// ... many more lines
```

## Performance Characteristics

### Kapper
```kotlin
// Predictable performance
fun getActiveUsers(): List<User> {
    return connection.query<User>("SELECT * FROM users WHERE active = true")
    // Always executes exactly one SQL query
}

// Manual optimization
fun getUsersWithPostCounts(): List<UserWithCount> {
    return connection.query<UserWithCount>("""
        SELECT u.*, COUNT(p.id) as post_count
        FROM users u
        LEFT JOIN posts p ON u.id = p.user_id
        GROUP BY u.id
    """)
    // Single optimized query
}
```

### Hibernate
```java
// Potential performance surprises
public List<User> getActiveUsers() {
    return userRepository.findByActiveTrue();
    // Could trigger additional queries if associations are accessed
}

public void printUserPosts() {
    List<User> users = userRepository.findAll();
    for (User user : users) {
        System.out.println(user.getPosts().size()); // N+1 query problem
    }
}

// Need explicit optimization
@Query("SELECT u FROM User u JOIN FETCH u.posts WHERE u.active = true")
List<User> findActiveUsersWithPosts(); // Fetch join to avoid N+1
```

## Transactions

### Kapper
```kotlin
dataSource.withTransaction { connection ->
    val userId = connection.execute("INSERT INTO users (name) VALUES (?) RETURNING id", name)
    connection.execute("INSERT INTO profiles (user_id, bio) VALUES (?, ?)", userId, bio)
    // Explicit, clear transaction boundaries
}
```

### Hibernate
```java
@Transactional
public User createUserWithProfile(String name, String bio) {
    User user = new User(name);
    user = userRepository.save(user);
    
    Profile profile = new Profile(user, bio);
    profileRepository.save(profile);
    
    return user;
    // Transaction boundaries managed by Spring
}
```

## Migration Strategies

### From Hibernate to Kapper
1. Start with read-only queries
2. Replace simple CRUD operations
3. Optimize complex queries with raw SQL
4. Migrate transactions last

### Sample Migration
```kotlin
// Before (Hibernate)
@Repository
class UserRepository {
    @Query("SELECT u FROM User u WHERE u.email = :email")
    fun findByEmail(@Param("email") email: String): User?
}

// After (Kapper)
class UserRepository(private val dataSource: DataSource) {
    fun findByEmail(email: String): User? {
        return dataSource.connection.use { connection ->
            connection.querySingle<User>("SELECT * FROM users WHERE email = ?", email)
        }
    }
}
```

## Next Steps

- [Kapper vs Ktorm](./ktorm.md) - Comparison with another Kotlin ORM
- [Performance Benchmarks](../../performance/) - Detailed performance comparisons
- [Migration Guide](../../guide/) - Tips for migrating between ORMs

## Source Examples

- **Kapper implementation**: [KapperExample.kt](https://github.com/driessamyn/kapper/blob/main/examples/kotlin-example/src/test/kotlin/net/samyn/kapper/example/kotlin/comparison/kapper/KapperExample.kt)
- **Hibernate implementation**: [HibernateExample.kt](https://github.com/driessamyn/kapper/blob/main/examples/kotlin-example/src/test/kotlin/net/samyn/kapper/example/kotlin/comparison/hibernate/HibernateExample.kt)
- **Hibernate entities**: [HibernateUniverse.kt](https://github.com/driessamyn/kapper/blob/main/examples/kotlin-example/src/main/kotlin/net/samyn/kapper/example/kotlin/hibernate/HibernateUniverse.kt)
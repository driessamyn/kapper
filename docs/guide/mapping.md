# Object Mapping

Kapper provides automatic object mapping from SQL result sets to Kotlin data classes and Java records.
This guide covers how mapping works and how to customize it for complex scenarios.

## How Auto-Mapping Works

Kapper uses reflection to map database columns to constructor parameters by matching names:

```kotlin
data class User(
    val id: Long,           // Maps to "id" column
    val name: String,       // Maps to "name" column  
    val email: String?,     // Maps to "email" column (nullable)
    val createdAt: Instant  // Maps to "created_at" column (snake_case conversion)
)

// Auto-mapping in action
val users = connection.query<User>("SELECT id, name, email, created_at FROM users")
```

### Column Name Matching

Kapper automatically handles different naming conventions:

```kotlin
data class Product(
    val id: Long,           // Matches: "id"
    val productName: String, // Matches: "product_name", "productname", "PRODUCT_NAME"
    val categoryId: Long    // Matches: "category_id", "categoryid", "CATEGORY_ID"
)
```

## Type Mappings

### Primitive Types

Kapper supports all standard JDBC types:

```kotlin
data class DataTypes(
    // Numeric types
    val smallInt: Short,
    val integer: Int,
    val bigInt: Long,
    val decimal: BigDecimal,
    val real: Float,
    val double: Double,
    
    // String types
    val varchar: String,
    val char: String,
    val text: String,
    
    // Boolean type
    val active: Boolean,
    
    // Date/time types
    val date: LocalDate,
    val time: LocalTime,
    val timestamp: LocalDateTime,
    val timestampWithZone: Instant,
    val utcTimestamp: Instant,
    
    // Binary data
    val data: ByteArray
)
```

### Nullable Types

Mark properties as nullable to handle NULL database values:

```kotlin
data class User(
    val id: Long,                    // Required field
    val name: String,                // Required field
    val email: String? = null,       // Optional with default
    val age: Int? = null,            // Optional
    val lastLogin: Instant? = null   // Optional timestamp
)
```

### Enum Mappings

Enums are automatically mapped from string values:

```kotlin
enum class UserStatus { ACTIVE, INACTIVE, SUSPENDED }

data class User(
    val id: Long,
    val name: String,
    val status: UserStatus  // Maps from VARCHAR storing "ACTIVE", "INACTIVE", etc.
)

// Custom enum mapping
enum class Priority(val value: Int) {
    LOW(1), MEDIUM(2), HIGH(3);
    
    companion object {
        fun fromValue(value: Int) = values().find { it.value == value }
            ?: throw IllegalArgumentException("Unknown priority: $value")
    }
}
```

### UUID Support

```kotlin
import java.util.UUID

data class Entity(
    val id: UUID,            // Automatically mapped from UUID columns
    val parentId: UUID?,     // Nullable UUID
    val externalId: String   // String representation of UUID
)
```

## Advanced Mapping Techniques

### Using Column Aliases

When column names don't match property names, use SQL aliases:

```kotlin
data class UserSummary(
    val id: Long,
    val fullName: String,    // Use alias to match this property
    val emailAddress: String // Use alias to match this property
)

val summaries = connection.query<UserSummary>("""
    SELECT 
        id,
        CONCAT(first_name, ' ', last_name) as full_name,
        email as email_address
    FROM users
""")
```

### Default Values

Properties with default values are used when columns are missing:

```kotlin
data class User(
    val id: Long,
    val name: String,
    val status: String = "ACTIVE",      // Default when column missing
    val createdAt: Instant = Instant.now(), // Default timestamp
    val settings: Map<String, String> = emptyMap() // Default collection
)
```

### Data Classes with Multiple Constructors

Primary constructor is used for mapping:

```kotlin
data class User(
    val id: Long,
    val name: String,
    val email: String
) {
    // Secondary constructors won't be used for mapping
    constructor(name: String, email: String) : this(0, name, email)
}
```

## Custom Mappers

For complex mapping scenarios, implement custom mappers:

### Simple Custom Mapper

```kotlin
// Custom type that needs special handling
data class Money(val amount: BigDecimal, val currency: String) {
    override fun toString() = "$amount $currency"
    
    companion object {
        fun parse(value: String): Money {
            val parts = value.split(" ")
            return Money(BigDecimal(parts[0]), parts[1])
        }
    }
}

// Register custom mapper
Kapper.mapperRegistry.registerIfAbsent<Money> { rs, index ->
    val value = rs.getString(index)
    if (value != null) Money.parse(value) else null
}

data class Order(
    val id: Long,
    val amount: Money,  // Now uses custom mapper
    val currency: String
)
```

Mappers can be defined as lambda functions as per above example, or, if preferred, using a self-contained, easily unit-testable, function:

```kotlin
class MoneyMapper : Mapper<Money> {
    override fun createInstance(
        resultSet: ResultSet,
        fields: Map<String, Field>,
    ) : Money? {
        val value = resultSet.getString("amount")
        return if (value != null) Money.parse(value) else null
    }
}

Kapper.mapperRegistry.registerIfAbsent<Money>(MoneyMapper())
```

### Complex Object Mapper

Mappers can be used to map complex objects

```kotlin
data class Address(
    val street: String,
    val city: String,
    val zipCode: String,
    val country: String
)

data class User(
    val id: Long,
    val name: String,
    val address: Address  // Embedded object
)

// Custom mapper for embedded address
Kapper.mapperRegistry.registerIfAbsent<User> { rs, startIndex ->
    val address = Address(
        street = rs.getString("street"),
        city = rs.getString("city"),
        zipCode = rs.getString("zip_code"),
        country = rs.getString("country")
    )
    User(
        rs.getLong("id"),
        rs.getString("name"),
        address
    )
}
```

## Java Records Support

Kapper has excellent support for Java Records:

```java
public record User(
    Long id,
    String name, 
    String email,
    @Nullable Instant createdAt  // Nullable annotation supported
) {}

public record Product(
    Long id,
    String name,
    BigDecimal price
) {
    // Compact constructor for validation
    public Product {
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
    }
}
```

## Mapping Performance

### Mapper Caching

Kapper automatically caches mapper instances:

```kotlin
// First call creates and caches mapper
val users1 = connection.query<User>("SELECT * FROM users")

// Subsequent calls reuse cached mapper (faster)
val users2 = connection.query<User>("SELECT * FROM users WHERE active = true")
```

### Manual Mapping for Hot Paths

Kapper's auto-mapping is fast, but for maximum performance in critical paths, use manual mappers:

```kotlin
// Fastest: Manual row mapping, avoiding reflection
val users = connection.query("SELECT id, name, email FROM users") { rs, _ ->
    User(
        id = rs.getLong(1),      // By index is fastest
        name = rs.getString(2),
        email = rs.getString(3)
    )
}
```

## Next Steps

- Explore [Queries](./queries.md) for advanced query patterns  
- See [Transactions](./transactions.md) for managing database transactions
- Check [Performance Tuning](./performance-tuning.md) for optimization tips
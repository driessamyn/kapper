# Basic CRUD with Records (Java)

Learn how to use Kapper with Java Records for clean, immutable data models.

> **📁 Source Code**: View complete examples in the [java-example](https://github.com/driessamyn/kapper/tree/main/examples/java-example/src/test/java/net/samyn/kapper/example/java) directory.

## Setup

```java
// SuperHero record
public record SuperHeroRecord(UUID id, String name, String email, int age) {}

// Villain record
public record VillainRecord(UUID id, String name) {}

// Battle record
public record SuperHeroBattleRecord(
    UUID superHeroId, 
    UUID villainId, 
    LocalDateTime battleDate,
    LocalDateTime updatedTs
) {}
```

## Create (Insert)

```java
// Insert and get result
try (var connection = dataSource.getConnection()) {
    var hero = new SuperHeroRecord(
        UUID.randomUUID(), "Batman", "batman@dc.com", 85
    );
    
    var rows = kapper.execute(
        connection,
        """
        INSERT INTO super_heroes(id, name, email, age) 
        VALUES (:id, :name, :email, :age)
        """,
        Map.of(
            "id", hero.id(),
            "name", hero.name(),
            "email", hero.email(),
            "age", hero.age()
        )
    );
    
    System.out.println("Inserted " + rows + " hero(s)");
}
```

## Read (Select)

```java
// Query single superhero
Optional<SuperHeroRecord> hero = kapper.querySingle(
    connection,
    SuperHeroRecord.class,
    "SELECT * FROM super_heroes WHERE name = :name", 
    Map.of("name", "Batman")
);

if (hero.isPresent()) {
    System.out.println("Found hero: " + hero.get().name());
}

// Query multiple superheroes
List<SuperHeroRecord> heroes = kapper.query(
    connection,
    SuperHeroRecord.class,
    "SELECT * FROM super_heroes WHERE age > :age",
    Map.of("age", 80)
);

heroes.forEach(h -> System.out.println("Hero: " + h.name() + ", Age: " + h.age()));
```

## Update

```java
// Update superhero age
int rowsUpdated = kapper.execute(
    connection,
    """
    UPDATE super_heroes
    SET age = :newAge
    WHERE name = :name
    """,
    Map.of(
        "newAge", 86,
        "name", "Batman"
    )
);

System.out.println("Updated " + rowsUpdated + " rows");
```

## Delete

```java
// Delete superhero
kapper.execute(
    connection,
    """
    DELETE FROM super_heroes
    WHERE name = :name
    """,
    Map.of("name", "Batman")
);
```

## Complete Example

```java
public class SuperHeroCrudExample {
    public static void main(String[] args) {
        var kapper = Kapper.getInstance();
        var dataSource = createDataSource();
        
        try (var connection = dataSource.getConnection()) {
            // Create superhero
            var batman = new SuperHeroRecord(
                UUID.randomUUID(), "Batman", "batman@dc.com", 85
            );
            
            var insertedRows = kapper.execute(
                connection,
                """
                INSERT INTO super_heroes(id, name, email, age) 
                VALUES (:id, :name, :email, :age)
                """,
                Map.of(
                    "id", batman.id(),
                    "name", batman.name(),
                    "email", batman.email(),
                    "age", batman.age()
                )
            );
            
            System.out.println("Inserted " + insertedRows + " hero(s)");
            
            // Read hero back
            Optional<SuperHeroRecord> retrievedHero = kapper.querySingle(
                connection,
                SuperHeroRecord.class,
                "SELECT * FROM super_heroes WHERE id = :id", 
                Map.of("id", batman.id())
            );
            
            retrievedHero.ifPresent(hero -> 
                System.out.println("Retrieved: " + hero.name() + " (" + hero.age() + " years old)")
            );
            
            // Update hero
            kapper.execute(
                connection,
                "UPDATE super_heroes SET age = :age WHERE id = :id",
                Map.of("age", 86, "id", batman.id())
            );
            
            // Verify update
            Optional<SuperHeroRecord> updatedHero = kapper.querySingle(
                connection,
                SuperHeroRecord.class,
                "SELECT * FROM super_heroes WHERE id = :id", 
                Map.of("id", batman.id())
            );
            
            updatedHero.ifPresent(hero -> 
                System.out.println("Updated: " + hero.name() + " is now " + hero.age() + " years old")
            );
            
            // Clean up
            kapper.execute(
                connection, 
                "DELETE FROM super_heroes WHERE id = :id", 
                Map.of("id", batman.id())
            );
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private static HikariDataSource createDataSource() {
        var dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/example");
        dataSource.setUsername("user");
        dataSource.setPassword("password");
        return dataSource;
    }
}
```

## Working with Repository Pattern

```java
public class SuperHeroRepository {
    private final Kapper kapper;
    private final DataSource dataSource;

    public SuperHeroRepository(DataSource dataSource) {
        this.kapper = Kapper.getInstance();
        this.dataSource = dataSource;
    }

    public int insertHero(SuperHeroRecord hero) throws SQLException {
        try (var connection = dataSource.getConnection()) {
            return kapper.execute(
                connection,
                """
                INSERT INTO super_heroes(id, name, email, age) 
                VALUES (:id, :name, :email, :age)
                """,
                Map.of(
                    "id", hero.id(),
                    "name", hero.name(),
                    "email", hero.email(),
                    "age", hero.age()
                )
            );
        }
    }

    public Optional<SuperHeroRecord> findById(UUID id) throws SQLException {
        try (var connection = dataSource.getConnection()) {
            return kapper.querySingle(
                connection,
                SuperHeroRecord.class,
                "SELECT * FROM super_heroes WHERE id = :id",
                Map.of("id", id)
            );
        }
    }

    public List<SuperHeroRecord> findByAgeRange(int minAge, int maxAge) throws SQLException {
        try (var connection = dataSource.getConnection()) {
            return kapper.query(
                connection,
                SuperHeroRecord.class,
                "SELECT * FROM super_heroes WHERE age BETWEEN :minAge AND :maxAge",
                Map.of("minAge", minAge, "maxAge", maxAge)
            );
        }
    }
}
```

## Error Handling

```java
public Optional<SuperHeroRecord> findHeroSafely(String name) {
    try (var connection = dataSource.getConnection()) {
        return kapper.querySingle(
            connection,
            SuperHeroRecord.class, 
            "SELECT * FROM super_heroes WHERE name = :name",
            Map.of("name", name)
        );
        
    } catch (SQLException e) {
        System.err.println("Database error: " + e.getMessage());
        return Optional.empty();
    }
}

public SuperHeroRecord createHeroWithValidation(String name, String email, int age) {
    if (name == null || name.trim().isEmpty()) {
        throw new IllegalArgumentException("Name cannot be empty");
    }
    
    if (email == null || !email.contains("@")) {
        throw new IllegalArgumentException("Invalid email format");
    }
    
    if (age < 0 || age > 10000) {
        throw new IllegalArgumentException("Age must be realistic");
    }
    
    try (var connection = dataSource.getConnection()) {
        var hero = new SuperHeroRecord(UUID.randomUUID(), name, email, age);
        
        kapper.execute(
            connection,
            """
            INSERT INTO super_heroes(id, name, email, age) 
            VALUES (:id, :name, :email, :age)
            """,
            Map.of(
                "id", hero.id(),
                "name", hero.name(),
                "email", hero.email(),
                "age", hero.age()
            )
        );
        
        return hero;
        
    } catch (SQLException e) {
        if (e.getSQLState().equals("23505")) { // Unique violation
            throw new IllegalArgumentException("Hero with this name already exists: " + name);
        }
        throw new RuntimeException("Database error", e);
    }
}
```

## Next Steps

- [Java Records Support](./records.md) - Advanced record patterns
- [Advanced Queries](../kotlin/advanced-queries.md) - Complex SQL operations
- [Transaction Handling](../kotlin/transactions.md) - Managing transactions

## Source Examples

- **Java Records**: [SuperHeroRecord.java](https://github.com/driessamyn/kapper/blob/main/examples/java-example/src/main/java/net/samyn/kapper/example/java/kapper/SuperHeroRecord.java)
- **Execute tests**: [ExecuteJavaTest.java](https://github.com/driessamyn/kapper/blob/main/examples/java-example/src/test/java/net/samyn/kapper/example/java/ExecuteJavaTest.java)
- **Query tests**: [QueryJavaTest.java](https://github.com/driessamyn/kapper/blob/main/examples/java-example/src/test/java/net/samyn/kapper/example/java/QueryJavaTest.java)
- **DTO examples**: [ExecuteWithDtoJavaTest.java](https://github.com/driessamyn/kapper/blob/main/examples/java-example/src/test/java/net/samyn/kapper/example/java/ExecuteWithDtoJavaTest.java)
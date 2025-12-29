# Java Records Support

Learn how to effectively use Java Records with Kapper for immutable, clean data models.

> **📁 Source Code**: View complete examples in the [java-example](https://github.com/driessamyn/kapper/tree/main/examples/java-example/src/main/java/net/samyn/kapper/example/java/kapper) directory.

## Basic Record Patterns

### Simple Entity Records

These are the actual records used in the Kapper examples:

```java
// Simple superhero record
public record SuperHeroRecord(UUID id, String name, String email, int age) {}

// Villain record  
public record VillainRecord(UUID id, String name) {}

// Battle result record
public record SuperHeroBattleRecord(
    String superhero,
    String villain,
    LocalDateTime date
) {}

// Movie analysis record
public record PopularMovieRecord(
    String title,
    long grossed,
    double comparedToAnnualAverage,
    int allTimeRanking
) {}
```

## Working with Records

### Basic CRUD Operations

```java
// Insert a superhero record
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
```

### Querying Records

```java
// Query single record
Optional<SuperHeroRecord> hero = kapper.querySingle(
    connection,
    SuperHeroRecord.class,
    "SELECT * FROM super_heroes WHERE name = :name", 
    Map.of("name", "Batman")
);

// Query multiple records
List<SuperHeroRecord> heroes = kapper.query(
    connection,
    SuperHeroRecord.class,
    "SELECT * FROM super_heroes WHERE age > :age",
    Map.of("age", 80)
);
```

### Repository Pattern with Records

```java
// From SuperHeroRecordRepository.java
public class SuperHeroRecordRepository {
    private final Kapper kapper;
    private final DataSource dataSource;

    public SuperHeroRecordRepository(DataSource dataSource) throws SQLException {
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
}
```

## Record Benefits with Kapper

### Immutability
Records are immutable by default, which works perfectly with Kapper's functional approach:

```java
// Records can't be modified after creation
var hero = new SuperHeroRecord(id, "Batman", "batman@dc.com", 85);
// hero.name = "Superman"; // Compilation error - no setters
```

### Automatic Components
Records provide automatic implementations:

```java
var hero1 = new SuperHeroRecord(uuid, "Batman", "batman@dc.com", 85);
var hero2 = new SuperHeroRecord(uuid, "Batman", "batman@dc.com", 85);

// Automatic equals/hashCode
System.out.println(hero1.equals(hero2)); // true

// Automatic toString
System.out.println(hero1); // SuperHeroRecord[id=..., name=Batman, ...]

// Component accessors
System.out.println(hero1.name()); // Batman
```

## Testing with Records

```java
@Test
void shouldCreateAndRetrieveHero() throws SQLException {
    var originalHero = new SuperHeroRecord(
        UUID.randomUUID(), "Test Hero", "test@example.com", 25
    );
    
    // Insert
    repository.insertHero(originalHero);
    
    // Retrieve
    var retrievedHero = kapper.querySingle(
        connection,
        SuperHeroRecord.class,
        "SELECT * FROM super_heroes WHERE id = :id",
        Map.of("id", originalHero.id())
    );
    
    assertTrue(retrievedHero.isPresent());
    assertEquals(originalHero.name(), retrievedHero.get().name());
    assertEquals(originalHero.email(), retrievedHero.get().email());
    assertEquals(originalHero.age(), retrievedHero.get().age());
}
```

## Next Steps

- [Basic CRUD Operations](./basic-crud.md) - Fundamental operations
- [Advanced Queries](../kotlin/advanced-queries.md) - Complex SQL patterns
- [Performance Guide](../../guide/performance-tuning.md) - Optimization strategies

## Source Examples

- **Record entities**: [SuperHeroRecord.java](https://github.com/driessamyn/kapper/blob/main/examples/java-example/src/main/java/net/samyn/kapper/example/java/kapper/SuperHeroRecord.java)
- **Battle record**: [SuperHeroBattleRecord.java](https://github.com/driessamyn/kapper/blob/main/examples/java-example/src/main/java/net/samyn/kapper/example/java/kapper/SuperHeroBattleRecord.java)
- **Repository with records**: [SuperHeroRecordRepository.java](https://github.com/driessamyn/kapper/blob/main/examples/java-example/src/main/java/net/samyn/kapper/example/java/kapper/SuperHeroRecordRepository.java)
- **Villain record**: [VillainRecord.java](https://github.com/driessamyn/kapper/blob/main/examples/java-example/src/main/java/net/samyn/kapper/example/java/kapper/VillainRecord.java)
- **Movie record**: [PopularMovieRecord.java](https://github.com/driessamyn/kapper/blob/main/examples/java-example/src/main/java/net/samyn/kapper/example/java/kapper/PopularMovieRecord.java)
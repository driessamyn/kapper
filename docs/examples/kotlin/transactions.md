# Transaction Handling (Kotlin)

Learn advanced transaction patterns and error handling strategies with Kapper.

> **📁 Source Code**: View complete examples in the [kotlin-example](https://github.com/driessamyn/kapper/tree/main/examples/kotlin-example/src/test/kotlin/net/samyn/kapper/example/kotlin) directory.

## Basic Transaction Patterns

### Simple Transaction with withTransaction

```kotlin
import net.samyn.kapper.withTransaction

// From SuperHeroRepository.kt - insertBattle method
fun insertBattle(
    superHero: SuperHero,
    villain: Villain,
    date: LocalDateTime,
) = dataSource.withTransaction { connection ->
    
    // Insert superhero if not exists  
    connection.execute(
        """
        INSERT INTO super_heroes(id, name, email, age) 
        VALUES (:id, :name, :email, :age)
        ${ignoreConflict("id")}
        """.trimIndent(),
        "id" to superHero.id,
        "name" to superHero.name,
        "email" to superHero.email,
        "age" to superHero.age,
    )
    
    // Insert villain if not exists
    connection.execute(
        """
        INSERT INTO villains(id, name) 
        VALUES (:id, :name)
        ${ignoreConflict("id")}
        """.trimIndent(),
        "id" to villain.id,
        "name" to villain.name,
    )
    
    // Insert the battle record
    connection.execute(
        """
        INSERT INTO battles(super_hero_id, villain_id, battle_date, updated_ts)
        VALUES (:super_hero_id, :villain_id, :date, NOW())
        """.trimIndent(),
        "super_hero_id" to superHero.id,
        "villain_id" to villain.id,
        "date" to date,
    )
}
```

## Manual Transaction Control

### Using Raw Connection Transactions

```kotlin
// Manual transaction control example
connection.use { conn ->
    try {
        conn.autoCommit = false
        
        // Multiple operations
        val heroId = conn.execute(
            "INSERT INTO super_heroes (id, name, email, age) VALUES (:id, :name, :email, :age)",
            "id" to UUID.randomUUID(),
            "name" to "Batman",
            "email" to "batman@dc.com",
            "age" to 85
        )
        
        conn.execute(
            "INSERT INTO battles (super_hero_id, villain_id, battle_date, updated_ts) VALUES (:heroId, :villainId, :date, NOW())",
            "heroId" to heroId,
            "villainId" to villainId,
            "date" to LocalDateTime.now()
        )
        
        conn.commit()
        println("Battle created successfully")
        
    } catch (e: Exception) {
        conn.rollback()
        throw e
    }
}
```

## Error Handling

```kotlin
try {
    dataSource.withTransaction { connection ->
        // Your transactional operations
        connection.execute("INSERT INTO super_heroes ...")
        connection.execute("INSERT INTO battles ...")
    }
} catch (e: SQLException) {
    logger.error("Transaction failed", e)
    // Handle database errors
} catch (e: Exception) {
    logger.error("Unexpected error during transaction", e)
    throw e
}
```

## Next Steps

- [Coroutines & Flow](./coroutines.md) - Async transaction handling
- [Performance Guide](../../guide/performance-tuning.md) - Transaction optimization
- [Database Support](../../guide/database-support.md) - Database-specific transaction features

## Source Examples

- **Entity definitions**: [Entities.kt](https://github.com/driessamyn/kapper/blob/main/examples/kotlin-example/src/main/kotlin/net/samyn/kapper/example/kotlin/Entities.kt)
- **Repository patterns**: [SuperHeroRepository.kt](https://github.com/driessamyn/kapper/blob/main/examples/kotlin-example/src/main/kotlin/net/samyn/kapper/example/kotlin/kapper/SuperHeroRepository.kt)
- **Execute examples**: [Execute.kt](https://github.com/driessamyn/kapper/blob/main/examples/kotlin-example/src/test/kotlin/net/samyn/kapper/example/kotlin/simple/Execute.kt)
- **Comprehensive tests**: [KapperExample.kt](https://github.com/driessamyn/kapper/blob/main/examples/kotlin-example/src/test/kotlin/net/samyn/kapper/example/kotlin/comparison/kapper/KapperExample.kt)
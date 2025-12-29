# Kapper, a Fresh Look at ORMs for Kotlin and the JVM

> SQL is not a problem to be solved - it's a powerful tool to be embraced. This is the philosophy behind Kapper...

## Introduction

[Kapper](https://github.com/driessamyn/kapper) is a lightweight, [Dapper](https://github.com/DapperLib/Dapper)-inspired ORM (Object-Relational Mapping) library for the Kotlin programming language, targeting the JVM ecosystem. While most ORMs attempt to abstract away SQL, Kapper takes a different approach - it embraces SQL as the most effective language for database interactions while providing modern Kotlin conveniences for execution and mapping.

## Why Another ORM?

I created Kapper because I found existing solutions in the JVM ecosystem were either too heavy, too abstract, or too intrusive. While working with various ORMs, I consistently ran into what I call the "abstraction trap" —where the supposed convenience of database abstraction actually creates more complexity than it solves.

## The Abstraction Trap

Many ORM libraries fall into a common pattern: they try to shield developers from SQL by providing their own DSL or code generation tools. While this seems helpful on the surface, it creates several significant problems:

1. **Learning Curve Multiplication**: Instead of just needing to know SQL, developers must now learn both SQL *and* the ORM's abstraction layer. This is particularly evident when debugging performance issues or writing complex queries.

2. **Limited Expressiveness**: No matter how comprehensive an ORM's query API is, it will never capture the full expressiveness of SQL. Developers inevitably hit walls where they need to drop down to raw SQL anyway.

3. **Hidden Complexity**: ORMs often mask the actual database operations being performed. A simple-looking query might generate multiple database round trips or inefficient SQL, leading to performance problems that are hard to diagnose.

4. **False Sense of Database Independence**: While ORMs promise database independence, in practice, efficient database usage often requires leveraging database-specific features. The abstraction becomes leaky when you need to optimise performance or use advanced features.

## The Kapper Philosophy

Instead of adding another abstraction layer, Kapper embraces three core principles:

1. **SQL is the Best Query Language**: SQL has evolved over decades to be expressive, powerful, and optimised for database operations. Instead of hiding it, we should leverage it directly.

2. **Minimal Abstraction**: Kapper provides just enough abstraction to make database operations comfortable in Kotlin or Java, without trying to reinvent database interactions. Kapper prefers extension of existing APIs than abstraction of them.

3. **Transparency**: What you write is what gets executed. There's no magic query generation or hidden database calls.

## Real-World Comparisons

In this section, I want to explore the Kapper API and compare it to two popular ORM libraries, implementing the same examples in each.
The source code for all examples in this article, along with the used DB script, including the comparisons, can be found in the [Kapper Examples](https://github.com/driessamyn/kapper-examples/) repository.

### The Kapper API

Kapper's API is intentionally simple, implemented as an extension of the `java.sql.Connection` interface. This design choice ensures database connections in Kapper are the same as in JDBC:

```kotlin
// Create a DataSource object, for example using [HikariCP](https://github.com/brettwooldridge/HikariCP)
//  Kapper is un-opinionated about which pooler, if any you use.
val dataSource = HikariDataSource().apply {
    jdbcUrl = "jdbc:PostgreSQL://localhost:5432/mydatabase"
    username = "username"
    password = "password"
}

// The Kapper API is exposed as an extension of the java.sql.Connection interface:
dataSource.connection.use { connection ->
    // Do database stuff
}
```

The API consists of two main functions:

- `query`/`querySingle`: Execute `SELECT` queries.
- `execute`: Perform `INSERT`, `UPDATE`, `DELETE`, and other DML queries.

Example:

```kotlin
dataSource.connection.use {
    it.query<SuperHero>("SELECT * FROM super_heroes")
}
```

Or select a single superhero:

```kotlin
dataSource.connection.use {
    it.querySingle<SuperHero>(
        "SELECT * FROM super_heroes WHERE id = :id",
        "id" to id,
    )
}
```

Kapper supports auto-mapping based on column names, but you can also provide custom mapping if needed:

```kotlin
dataSource.connection.use {
    it.querySingle<Map<String, *>>(
        "SELECT * FROM villains WHERE name = :name",
        { resultSet, fields ->
            mapOf(
                "id" to resultSet.getString("id"),
                "name" to resultSet.getString("name"),
            )
        },
        "name" to "Joker",
    )
}
```

Kapper does not maintain a mapping between the database schema and classes. You are free to write any SQL query map the results in whichever way is necessary.

For example, the following query joins multiple tables and auto-maps the results to a DTO (Data Transfer Object):

```kotlin
dataSource.connection.use {
    it.query<SuperHeroBattle>(
        """
                SELECT s.name as superhero, v.name as villain, b.battle_date as date
                FROM super_heroes as s
                INNER JOIN battles as b on s.id = b.super_hero_id
                INNER JOIN villains as v on v.id = b.villain_id
                WHERE s.name = :name 
                """.trimIndent(),
        "name" to superHeroName,
    )
}
```

In the following, more complex, example, a lambda function is used to process the results of a window query:

```kotlin
dataSource.connection.use {
    var allTimeRank = 1
    it.query(
        // "complex query" -> it's "just" SQL    
        """
         SELECT
         title,
         release_date, 
         gross_worldwide, 
         AVG(gross_worldwide) OVER() AS total_average_gross,
         AVG(gross_worldwide) OVER(PARTITION BY EXTRACT(YEAR FROM release_date)) AS average_annual_gross
        FROM movies 
        ORDER BY gross_worldwide DESC
        LIMIT 3
        """.trimIndent(),
        { rs, _ ->
            // map each result -> it's "just" code
            val gross = rs.getLong("gross_worldwide")
            val annualAvgGross = rs.getInt("average_annual_gross")
            PopularMovie(
                rs.getString("title"),
                gross,
                gross / annualAvgGross.toDouble(),
                allTimeRank++,
            )
        },
    )
}
```

This approach feels intuitive and familiar to anyone who has used SQL before.

Following the same paradigm, the `execute` function can be used to insert, update, or delete data:

```kotlin
dataSource.connection.use {
    it.execute(
        """
        INSERT INTO super_heroes(id, name, email, age) 
        VALUES (:id, :name, :email, :age)
        """.trimIndent(),
        "id" to superHero.id,
        "name" to superHero.name,
        "email" to superHero.email,
        "age" to superHero.age,
    )
}
```

Database transactions are supported by the existing JDBC API:

```kotlin
dataSource.connection.use {
    try {
        it.autoCommit = false
        it.execute(
            """
            INSERT INTO villains(id, name) 
            VALUES (:id, :name)
            """.trimIndent(),
            "id" to villain.id,
            "name" to villain.name,
        )
        it.execute(
            """
            INSERT INTO battles(super_hero_id, villain_id, battle_date, updated_ts)
            VALUES (:super_hero_id, :villain_id, :date, NOW())
            """.trimIndent(),
            "super_hero_id" to superHero.id,
            "villain_id" to villain.id,
            "date" to date,
        )
        it.commit()
    } catch (ex: SQLException) {
        it.rollback()
        throw ex
    }
}
```

Kapper doesn't generate SQL, this means that it is easy to use DB-specific features. In this example, we use Postgres' `ON CONFLICT` clause to only insert a row if it doesn't exist yet:

```kotlin
dataSource.connection.use {
    it.execute(
        """
        INSERT INTO super_heroes(id, name, email, age) 
        VALUES (:id, :name, :email, :age)
        ON CONFLICT DO NOTHING 
        """.trimIndent(),
        "id" to superHero.id,
        "name" to superHero.name,
        "email" to superHero.email,
        "age" to superHero.age,
    )
}
```

SQL is the native language to interact with database, and I believe that it is therefore the best.

### How does Kapper compare to other ORMs?

Let's compare Kapper to two popular ORM libraries in the JVM ecosystem: Hibernate and Ktorm.

#### Hibernate

If you have worked with Java in the last 20 years or so, it is likely you will have used or come across Hibernate, the _Godfather_ of ORM libraries.

Source code for the examples below can be found in the [Kapper Examples](https://github.com/driessamyn/kapper-examples/blob/release-1.0-article/kotlin-example/src/main/kotlin/net/samyn/kapper/example/kotlin/hibernate/HibernateUniverse.kt) repository.

Hibernate is JPA compatible and uses a combination of JPA and Hibernate annotations to map _Entity classes_ to the DB schema.

For example:

```kotlin
@Entity
@Table(name = "super_heroes")
class SuperHeroEntity(
    @Id
    @Column(name = "id")
    var id: UUID,
    @Column(name = "name")
    var name: String,
    @Column(name = "email")
    var email: String?,
    @Column(name = "age")
    var age: Int?,
) {
    // equals method overridden in a non-intuitive way
    //  hibernate subscribes that the id is the only thing that matters
    //  which is not the way most people think about equality
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as SuperHeroEntity

        return id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
```

In the above example, we can see our `super_heroes` table is mapped to a `SuperHeroEntity` class. This class is then used to interact with the database.

This allows us to interact with the DB using a Hibernate Session:

```kotlin
// listing some superheroes
sessionFactory.openSession().use {
    it.createQuery("FROM SuperHeroEntity", SuperHeroEntity::class.java).list()
}

// finding a single superhero
sessionFactory.openSession().use {
    it.find(SuperHeroEntity::class.java, id) as SuperHeroEntity
}

// inserting a superhero
sessionFactory.openSession().use {
    it.transaction.begin()
    it.merge(superHero)
    it.transaction.commit()
}
```

This looks elegant, but there are a few potential problems with this approach.
You may notice, for example, that the entity class is not a Kotlin data class, it has non-standard equals and hashCode overrides, and it is mutable.
Explaining the reasons for this is out-of-scope of this article, but you will find [many](https://medium.com/@goncharov.valentin/a-practical-point-of-view-on-kotlin-data-classes-and-jpa-hibernate-c69370b975e1) [articles](https://blog.kotlin-academy.com/kotlin-and-jpa-a-good-match-or-a-recipe-for-failure-e52718d93b4f) and [discussions](https://discuss.kotlinlang.org/t/data-classes-equals-and-hashcode-for-use-with-jpa/4868/2) on the topic.

The important point is that this is opinionated and moves away from the _normal_ way of doing things, adding a layer of complexity and potential confusion.
To hide this complexity and reduce the risk of un-intended side effects, it is commonplace for projects to add another layer of abstraction on top of the Hibernate entities so that Hibernate entities don't leak into the rest of the codebase.

Looking further than the trivial example, we can see further evidence of the abstraction leaking.
For example, consider the above join query which we used in the kapper example to find battles between superheroes and villains. Hibernate's version looks like this:

```kotlin
sessionFactory.openSession().use {
    it.createSelectionQuery(
        """
    SELECT b FROM SuperHeroBattleEntity b 
    WHERE b.superHero.name = :name
""",
        SuperHeroBattleEntity::class.java,
    )
        .setParameter("name", superHeroName)
        .list()
}
```

On the surface, this looks brilliant. It's so simple and easy, much simpler than the Kapper example.
But, as a reader of this code, do you understand the SQL query that will be generated to support this code?
As somebody who has used Hibernate quite a lot, I still fall back to inspecting the generated query and find myself studying the documentation repeatedly to remind myself how to set up the mapping so that it would generate the query I wanted.

Let's have a look at the queries (!) Hibernate generates in this example.

```sql 
# join battles with super_heroes
   select
        shbe1_0.battle_date,
        shbe1_0.super_hero_id,
        shbe1_0.villain_id,
        shbe1_0.updated_ts 
    from
        battles shbe1_0 
    join
        super_heroes sh2_0 
            on sh2_0.id=shbe1_0.super_hero_id 
    where
        sh2_0.name=?
# oh, wait, we need villains too, so better query for those
    select
        she1_0.id,
        she1_0.age,
        she1_0.email,
        she1_0.name 
    from
        super_heroes she1_0 
    where
        she1_0.id=?
# oh, what, there's more than one of them?
    select
        ve1_0.id,
        ve1_0.name 
    from
        villains ve1_0 
    where
        ve1_0.id=?
# heh, another!
    select
        ve1_0.id,
        ve1_0.name 
    from
        villains ve1_0 
    where
        ve1_0.id=?
```

Of course, we cannot blame Hibernate for creating the query as it does, and there is no doubt that the mapping and/or query can be written such that the query is more efficient and matches that of our _hand-crafted_ SQL query, however, it is common to encounter these types of issues in applications, and they are often difficult to troubleshoot. A small mapping change can result in significant consequence.

See [the entity mapping source code](https://github.com/driessamyn/kapper-examples/blob/release-1.0-article/kotlin-example/src/main/kotlin/net/samyn/kapper/example/kotlin/hibernate/HibernateUniverse.kt) and the example of how it was tested [here](https://github.com/driessamyn/kapper-examples/blob/release-1.0-article/kotlin-example/src/test/kotlin/HibernateExample.kt).

As you can see from that relatively simple example, a lot of Hibernate specific knowledge is required to be able to use the abstraction that is meant to make things easier.

Lastly, let's have a look at the more complex window query example.
Hibernate supports the concept of _Native Queries_, so we can re-use the SQL query which we used in the Kapper example:

```kotlin
sessionFactory.openSession().use {
    var allTimeRank = 1
    return it.createNativeQuery(
        """
         SELECT
         title,
         release_date, 
         gross_worldwide, 
         AVG(gross_worldwide) OVER() AS total_average_gross,
         AVG(gross_worldwide) OVER(PARTITION BY EXTRACT(YEAR FROM release_date)) AS average_annual_gross
        FROM movies 
        ORDER BY gross_worldwide DESC
        LIMIT 3
        """.trimIndent(),
        Array<Any>::class.java,
    )
        .resultList.map { row ->
            val result = row as Array<*>
            PopularMovie(
                result[0] as String,
                result[2] as Long,
                (result[2] as Long).toDouble() / (result[3] as BigDecimal).toDouble(),
                allTimeRank++,
            )
        }
}
```

Using a Hibernate specific API we can extract the data from the result set and map it to our DTO. In this case Hibernate doesn't offer any advantage of native JDBC but still introduces a layer of abstraction.

### Ktorm

The _new kid on the block_, Ktorm describes itself as "a lightweight and efficient ORM Framework for Kotlin directly based on pure JDBC".
This sounds similar in spirit to Kapper, so I wanted to explore it using the same examples as we used for Kapper and Hibernate.

Ktorm provides a pleasant development experience and I will definitely take some inspiration from it. However, I still think it suffers from the same leaky abstraction issues as Hibernate.

First of all, like Kapper, Ktorm isn't opinionated about your domain objects, so I was able to use the same simple data classes as I was using with Kapper:

```kotlin
data class SuperHero(val id: UUID, val name: String, val email: String? = null, val age: Int? = null)
data class SuperHeroBattle(val superhero: String, val villain: String, val date: LocalDateTime)
data class Villain(val id: UUID, val name: String)
``` 

Ktorm does however require you to define a mapping to each DB table.
There are a number of ways this can be done. For example:

```kotlin
object SuperheroesTable : Table<Nothing>("super_heroes") {
    val id = uuid("id").primaryKey()
    val name = varchar("name")
    val email = varchar("email")
    val age = int("age")

    fun toDomain(row: QueryRowSet) =
        SuperHero(
            row[id] as UUID,
            row[name] as String,
            row[email],
            row[age],
        )
}  
```  

Once the mapping is set up, queries can use these. For example:

```kotlin  
// listing some superheroes
database
    .from(superHeroes)
    .select()
    .map(superHeroes::toDomain) 
    
// finding a single superhero
database
    .from(superHeroes)
    .select()
    .where { superHeroes.id eq id }
    .map(superHeroes::toDomain)
    .firstOrNull()   

// inserting a super hero
database.insert(superHeroes) {
    set(it.id, superHero.id)
    set(it.name, superHero.name)
    set(it.email, superHero.email)
    set(it.age, superHero.age)
}  
```

The join query, while using a custom DSL, was quite easy to write after consulting the documentation and validating the generated SQL:

```kotlin
database
    .from(superHeroes)
    .innerJoin(battles, on = superHeroes.id eq battles.superHeroId)
    .innerJoin(villains, on = battles.villainId eq villains.id)
    .select()
    .where { superHeroes.name eq superHeroName }
    .map {
        SuperHeroBattle(
            it[superHeroes.name] as String,
            it[villains.name] as String,
            it[battles.battleDate] as LocalDateTime,
        )
    }
```

Finally, looking at the more complex window query example, it took a while to go through the documentation, complemented by studying the source code and getting some direction from Claude AI, I was able to come up with this:

```kotlin
database.from(PopularMovies)
    .select(
        PopularMovies.title,
        PopularMovies.releaseDate,
        PopularMovies.grossWorldwide,
        avg(PopularMovies.grossWorldwide).over().aliased("total_average_gross"),
        avg(PopularMovies.grossWorldwide)
            .over {
                window().partitionBy(
                    // Cannot work this out, so abandoning the attempt
                    // ??("EXTRACT(YEAR FROM ${PopularMovies.releaseDate.name})")
                )
            }
            .aliased("average_annual_gross")
    ).map {
        PopularMovie(
            it[PopularMovies.title] as String,
            it[PopularMovies.grossWorldwide] as Long,
            it["total_average_gross"] as Double,
            it["average_annual_gross"] as Double
        )
    }
```

This looked promising, but failed at the last hurdle. As far as I could find out, an API to extract the year part from the datetime column is missing, meaning the window query API cannot be used.

This is a good example of one of the major problems with these type of abstractions. However rich they are, they are never complete.
Like the Hibernate example, I reverted to using native SQL to achieve the desired result.

### Support for multiple databases

We explored API differences between Kapper, Hibernate and Ktorm, but we haven't yet looked at how the different libraries handle support for different types of databases.

Even though Kapper is currently only tested against PostgreSQL and MySQL, as an extension of the JDBC API, it is database agnostic and should work with any database for which there is a JDBC driver.
Hibernate and Ktorm also support many databases but because they both maintain an abstraction of SQL, they require explicit handling of database dialects.
Hibernate abstract the DB type from the user (which is also leaky as we will see). Ktorm is more explicit and provides different extension libraries for different databases, with different APIs for each.

The tests against the examples in this article [were run against PostgreSQL as well as MySQL](https://github.com/driessamyn/kapper-examples/blob/release-1.0-article/kotlin-example/src/test/kotlin/DbBase.kt), which highlighted some issues with both abstractions.

#### UUID handling

Unlike PostgreSQL, MySQL does not support the UUID type. One approach in this case is to store the UUID as a string.
Kapper does handles this conversion when automapping to a class is used, but, importantly, users can easily use a custom mapping function to convert any DB type to any JVM type.

In hibernate, we needed to add an extra configuration key when building the `SessionFactory` to instruct Hibernate to use the VARCHAR column type _only_ for MySQL:

```kotlin
if (jdbcUrl.contains(DbType.MySQL.name, ignoreCase = true)) {
    // glad I had Claude AI to help me with this little bit of "magic" :(
    it.setProperty("hibernate.type.preferred_uuid_jdbc_type", "VARCHAR")
}
```

Handling this in Ktorm required creating a custom column type, which made the mapping code a bit more awkward:

```kotlin
class CustomUUIDSqlType(
    private val dbType: DbType,
) : SqlType<UUID>(Types.VARCHAR, "custom_uuid") {
    override fun doSetParameter(
        ps: PreparedStatement,
        index: Int,
        parameter: UUID,
    ) {
        if (dbType == DbType.MySQL) {
            ps.setString(index, parameter.toString())
        } else {
            ps.setObject(index, parameter)
        }
    }

    override fun doGetResult(
        rs: ResultSet,
        index: Int,
    ): UUID? {
        return if (dbType == DbType.MySQL) {
            UUID.fromString(rs.getString(index))
        } else {
            rs.getObject(index) as UUID?
        }
    }
} 

class SuperheroesTable(dbType: DbType) : Table<Nothing>("super_heroes") {
    val id = registerColumn("id", CustomUUIDSqlType(dbType)).primaryKey()
    val name = varchar("name")
    val email = varchar("email")
    val age = int("age")
}

private val superHeroes = SuperheroesTable(dbType)  
```

This shows how leaky abstractions can result in a lot more complexity than is necessary.

#### Ignore conflicts

As a second example, our example had a function to record a superhero battle, which would insert superheros and villains only when they didn't exist yet.
Commonly, a `ON CONFLICT` clause is used to only insert the row if one does not exist.
MySQL does not support this, instead we can use a `ON DUPLICATE KEY UPDATE` clause and set the primary key to its own value to avoid an update.
Because Kapper doesn't abstract the SQL, this is easy to achieve by simply varying the SQL query based on the DB type, for example:

```kotlin
it.execute(
    """
    INSERT INTO villains(id, name) 
    VALUES (:id, :name)
    ${ignoreConflict("id")}
    """.trimIndent(),
    "id" to villain.id,
    "name" to villain.name,
)
  
fun ignoreConflict(updateCol: String) =
    if (DbType.MySQL == dbType) {
        "ON DUPLICATE KEY UPDATE $updateCol=$updateCol"
    } else {
        "ON CONFLICT DO NOTHING"
    }
```

In Ktorm we were able to leverage the dedicated MySQL and PostgreSQL libraries to handle this:

```kotlin
when (dbType) {
    DbType.MYSQL -> {
        this.mySqlInsertOrUpdate(table) {
            block(this, table)
            onDuplicateKey {
                onDuplicateBlock(this, table)
            }
        }
    }
    DbType.POSTGRESQL -> {
        this.postgresqlInsertOrUpdate(table) {
            block(this, table)
            onConflict {
                doNothing()
            }
        }
    }
}
```

This case can probably be handled in Hibernate a well, but I fell back to using what most examples I have seen:

```kotlin
it.merge(superHero)
```

Simple, heh, but which query is generated?

```sql
// let's check if the villain exists
    select
        ve1_0.id,
        ve1_0.name 
    from
        villains ve1_0 
    where
        ve1_0.id=?
// oh, it doesn't, so let's insert it
    insert 
    into
        villains
        (name, id) 
    values
        (?, ?)
```

Maybe some people are happy with the extra query, or maybe they aren't aware.

## Conclusion

The "abstraction trap" in ORMs stems from a fundamental misconception: that SQL is a problem to be solved rather than a tool to be embraced. Kapper takes a different approach by providing a thin, elegant layer that makes SQL comfortable to use without hiding it.

Kapper empowers developers with direct, efficient database access while preserving modern development benefits. It’s an extension of JDBC, not an abstraction of it—and that makes all the difference.

If you want to give Kapper a try, visit [GitHub](https://github.com/driessamyn/kapper). I’d love to hear your feedback and suggestions for how we can make Kapper even better.

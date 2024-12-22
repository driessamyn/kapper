# Kapper ORM

![build](https://github.com/driessamyn/kapper/actions/workflows/build-and-test.yml/badge.svg)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Kapper is a lightweight, Dapper-inspired ORM (Object-Relational Mapping) library for the Kotlin programming language, targeting the JVM ecosystem.

The main goals of the Kapper ORM are:

1. **Simplicity**: Provide a simple, intuitive API for common database operations, following Kotlin idioms.
2. **Performance**: Minimize overhead and dependencies to ensure fast execution of queries and updates.
3. **Flexibility**: Allow integration with various database drivers and connection management strategies.

Kapper aims to go against the grain of the heavy weight database abstractions such as Hibernate and JOOQ.
Instead, it makes JDBC support easy without taking away any flexibility.
In fact, it can happily live alongside an existing _vanilla_ JDBC integration.
It does not generate code, it does not introduce another layer of abstraction, it is never intrusive.

Kapper also means hairdresser in Dutch, but I have yet to come up with a reason for why this is relevant.

## Features

- **Simple API**: Kapper provides a familiar set of methods for executing SQL queries, mapping results to Kotlin data classes, and updating/inserting data.
- **Extensibility**: The Kapper API is implemented as extension functions on the `java.sql.Connection` interface, allowing seamless integration with existing JDBC code.
- **Lightweight**: Kapper has minimal external dependencies, focusing on providing core functionality without bloat.

Kapper is in early development phase and while it is functional, breaking API changes may be made until its first stable release.

Snapshot releases are published from the main branch to [GitHub Packages](packages/2353016).
Stable releases will be published to Maven Central.

## Usage

Here's a simple example of how to use Kapper:

Example DB table
```sql
 CREATE TABLE super_heroes (
    id UUID PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    age INT
);
 ```

and an example DTO:
```kotlin
data class SuperHero(val id: UUID, val name: String, val email: String? = null, val age: Int? = null)
```

You can use Kapper like so:
```kotlin
// Assuming you have a java.sql.Connection instance, using your favourite connection pooler, for example:
ds.getConnection().use { connection ->
    // insert a row:
    connection.execute(
        "INSERT INTO super_heroes(id, name, email, age) VALUES(:id, :name, :email, :age);",
        "id" to UUID.randomUUID(),
        "name" to "Batman",
        "email" to "batman@dc.com",
        "age" to 85,
    )
    
    // Execute a SQL query and map the results to a list of SuperHero objects
    val heroes: List<SuperHero> = connection.query<SuperHero>("SELECT * FROM super_heroes")

    // or query by passing parameters
    val olderHeroes = connection.query<SuperHero>("SELECT * FROM super_heroes WHERE age > :age", "age" to 80)
    
    // or find a single
    val batman = connection.querySingle<SuperHero>(
        "SELECT id, name FROM super_heroes WHERE name = :name",
        "name" to "Batman",
    )
}
```

Other examples are available in the [integration tests](lib/src/integrationTest/kotlin/net/samyn/kapper/).

Kapper does not maintain a mapping between classes and DB tables or entities. 
Instead, it can either map to a given class if the constructor arguments match the DB fields, or a mapping lambda can be passed in.
This means Kapper provides a strongly typed mapping, but still allows rows-to-object mapping with lambdas for more flexibility or advanced used cases.

## Using Kapper in your project

Currently, only snapshot releases are published.
Stable versions will be published in due course, but if you want to try the snapshot releases, add the following dependency to your `build.gradle.kts`:

  ```kotlin
   dependencies {
       implementation("com.github.driessamyn.kapper:kapper:<version>")
   }
   ```
Or for Maven:
   ```xml
   <dependency>
       <groupId>com.github.driessamyn.kapper</groupId>
       <artifactId>kapper</artifactId>
       <version>{version}</version>
   </dependency>
   ```

## Database support

Kapper's integration tests currently cover Postgresql and MySQL.
Other DBs will be added.
If you need support for another DB, and/or find an issue with a particular DB, feel free to [open an issue](kapper/issues) or, even better, submit a pull request. 

## Roadmap

Kapper is in its early stages of development.
The following will be worked on in the next few releases:

- Example repo to enhance the documentation.
- Create a benchmark suite to validate performance.
- Cache query parsing.
- Custom SQL type conversion.
- Add MS SQL Server and Oracle integration tests.

## Contributing

We welcome contributions to the Kapper! If you find any issues or have ideas for improvements, please feel free to [open an issue](kapper/issues) or submit a pull request.

## License

_Kapper_ is released under the [Apache 2.0 License](./LICENSE).

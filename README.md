# Kapper ORM

![build](https://github.com/driessamyn/kapper/actions/workflows/build-and-test.yml/badge.svg)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

<img alt="logo" src="./img/kapper-logo-small.png" align="left" style="margin-right: 7px;"/>

<p style="font: italic bold 20px sans-serif;padding-top:7px;line-height: 1.6;">SQL is not a problem to be solved - it's a powerful tool to be embraced.<br />
This is the philosophy behind Kapper...</p>

<br clear="left" />

Kapper is a lightweight, Dapper-inspired ORM (Object-Relational Mapping) library written in Kotlin, targeting the JVM ecosystem.
It embraces SQL rathe than abstracting it away, providing a simple, intuitive API for executing queries and mapping results.

## The Kapper Philosophy

Instead of adding another abstraction layer, Kapper embraces three core principles:

1. **SQL is the Best Query Language**: SQL has evolved over decades to be expressive, powerful, and optimized for database operations.
Instead of hiding it, we should leverage it directly.
2. **Minimal Abstraction**: Kapper provides just enough abstraction to make database operations comfortable in Kotlin, without trying to reinvent database interactions. 
Kapper prefers extension of existing APIs than abstraction of them.
3. **Transparency**: What you write is what gets executed.
There's no magic query generation or hidden database calls.

Kapper aims to go against the grain of the heavyweight database abstractions.
Instead, it makes JDBC support easy without taking away any flexibility.
In fact, it can happily live alongside an existing _vanilla_ JDBC integration and/or your existing DB layer.

It does _not_ generate code, it does not introduce another layer of abstraction, it is never intrusive.
It does _not_ hide SQL, instead it embraces the fact that SQL is the best language for DB interaction.

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

Other examples are available in the [integration tests](lib/src/integrationTest/kotlin/net/samyn/kapper/) or check out the [Kapper-Example](https://github.com/driessamyn/kapper-examples) repo for more extended examples and documentation,
including [a comparison with Hibernate and Ktorm](https://github.com/driessamyn/kapper-examples/tree/release-1.0-article?tab=readme-ov-file#comparison-with-orms).

Kapper does not maintain a mapping between classes and DB tables or entities. 
Instead, it can either map to a given class if the constructor arguments match the DB fields, or a mapping lambda can be passed in.
This means Kapper provides a strongly typed mapping, but still allows rows-to-object mapping with lambdas for more flexibility or advanced used cases.

## Using Kapper in your project

Stable versions of Kapper are published to [Maven Central](https://central.sonatype.com/artifact/net.samyn/kapper/versions).
To use this in your project, simply add the following dependency to your `build.gradle.kts` (or the Groovy equivalent in `build.gradle`):

```kotlin
dependencies {
    implementation("net.samyn:kapper:1.0.0")
}
```

For Maven, use:

```xml
<dependency>
    <groupId>net.samyn</groupId>
    <artifactId>kapper</artifactId>
    <version>1.0.0</version>
</dependency>
```

Snapshot releases are published from the `main` branch to [GitHub packages](./packages).

In order to use these, add GitHub packages as a repository (and ensure `GH_USERNAME` and `GH_TOKEN` are set in your environment):
```kotlin
repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/driessamyn/kapper")
        credentials {
            username = project.findProperty("gh.user") as String? ?: System.getenv("GH_USERNAME")
            password = project.findProperty("gh.key") as String? ?: System.getenv("GH_TOKEN")
        }
    }
}
```

And add the following dependency to your `build.gradle.kts`:

```kotlin
dependencies {
   implementation("net.samyn:kapper:<VERSION>")
}
```

See [Kapper-Example](https://github.com/driessamyn/kapper-examples) for example usage.

## Database support

Kapper's integration tests currently cover Postgresql and MySQL.
Other DBs will be added.
If you need support for another DB, and/or find an issue with a particular DB, feel free to [open an issue](kapper/issues) or, even better, submit a pull request. 

# External content

- [Dev.to - Kapper, a Fresh Look at ORMs for Kotlin and the JVM ](https://dev.to/driessamyn/kapper-a-fresh-look-at-orms-for-kotlin-and-the-jvm-1ln5)

## Roadmap

Kapper is in its early stages of development.
The following will be worked on in the next few releases:

- [ ] Create a benchmark suite to validate performance.
- [ ] Add co-routine support.
- [ ] Add stream support.
- [ ] Improve and additional support for date/time conversion.
- [ ] Increase Java API compatibility tests & examples.
- [ ] Improve user documentation.
- [ ] Cache query parsing.
- [ ] Custom SQL type conversion.
- [ ] Bulk operations support
- [ ] Add MS SQL Server and Oracle integration tests.
- [ ] Tests & examples in other JVM languages.
- [ ] Create transaction syntax sugar.
- [ ] Support DTO argument for `execute`.
- [ ] Add support for non-blocking JDBC drivers.

Anything else you think is missing, or you want to be prioritised, please [open an issue](kapper/issues) or submit a pull request.

## Contributing

We welcome contributions to the Kapper! If you find any issues or have ideas for improvements, please feel free to [open an issue](kapper/issues) or submit a pull request.

## License

_Kapper_ is released under the [Apache 2.0 License](./LICENSE).

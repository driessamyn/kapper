# Kapper ORM

![build](https://github.com/driessamyn/kapper/actions/workflows/build-and-test.yml/badge.svg)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Kapper is a lightweight, Dapper-inspired ORM (Object-Relational Mapping) library for the Kotlin programming language, targeting the JVM ecosystem.

The main goals of the Kapper ORM are:

1. **Simplicity**: Provide a simple, intuitive API for common database operations, following Kotlin idioms.
2. **Performance**: Minimize overhead and dependencies to ensure fast execution of queries and updates.
3. **Flexibility**: Allow integration with various database drivers and connection management strategies.

## Features

- **Simple API**: Kapper provides a familiar set of methods for executing SQL queries, mapping results to Kotlin data classes, and updating/inserting data.
- **Dynamic Parameters**: The `DynamicParameters` class makes it easy to bind parameters to SQL statements.
- **Extensibility**: The Kapper API is implemented as extension functions on the `java.sql.Connection` interface, allowing seamless integration with existing JDBC code.
- **Lightweight**: Kapper has minimal external dependencies, focusing on providing core functionality without bloat.

> Work on Kapper has only recently started and it is not yet functional.
> Documentation below covers intended API, not final.

## Usage

Here's a simple example of how to use Kapper:

```kotlin
// Assuming you have a java.sql.Connection instance
val connection: Connection = ...

// Execute a SQL query and map the results to a list of User objects
val users: List<User> = connection.query<User>("SELECT * FROM users")

// Execute a SQL statement with parameters
val updatedRows = connection.execute(
    "UPDATE users SET name = :name WHERE id = :id",
    name" to "John Doe", "id" to 123
)

// Query a single result and map it to a User object
val user: User? = connection.querySingle<User>(
    "SELECT * FROM users WHERE id = :id",
    "id" to 1
)
```

## Contributing

We welcome contributions to the Kapper ORM project! If you find any issues or have ideas for improvements, please feel free to open an issue or submit a pull request.

## License

_Kapper_ is released under the [Apache 2.0 License](./LICENSE).

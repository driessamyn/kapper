### Connection setup

Create a pooled DataSource.

```kotlin
fun dataSource(url: String, user: String, secret: String): DataSource =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = url
            username = user
            password = secret
        },
    )
```

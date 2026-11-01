### Testing

Use the in-memory test harness.

```kotlin
// kapper-test provides an in-memory harness so tests need no Docker.
fun testHarness(): DataSource =
    KapperTest.dataSource(
        ddl = "CREATE TABLE users(id UUID PRIMARY KEY, name VARCHAR(100) NOT NULL)",
    )
```

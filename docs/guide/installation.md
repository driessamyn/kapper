# Installation

## Gradle (Kotlin DSL)

Add Kapper to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("net.samyn:kapper:1.6.1")
}
```

For coroutine support, use:

```kotlin
dependencies {
    implementation("net.samyn:kapper-coroutines:1.6.1")
}
```

## Gradle (Groovy)

Add Kapper to your `build.gradle`:

```groovy
dependencies {
    implementation 'net.samyn:kapper:1.6.1'
}
```

## Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>net.samyn</groupId>
    <artifactId>kapper</artifactId>
    <version>1.6.1</version>
</dependency>
```

## Snapshot Versions

Snapshot releases are published from the `main` branch to [GitHub Packages](https://github.com/driessamyn/kapper/packages).

To use snapshots, add GitHub packages as a repository:

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

## Database Drivers

You'll also need to include the JDBC driver for your database, for example:

### PostgreSQL
```kotlin
implementation("org.postgresql:postgresql:42.7.8")
```

### MySQL
```kotlin
implementation("com.mysql:mysql-connector-j:9.4.0")
```

### SQLite
```kotlin
implementation("org.xerial:sqlite-jdbc:3.50.3.0")
```

### Oracle
```kotlin
implementation("com.oracle.database.jdbc:ojdbc11:23.9.0.25.07")
```

### SQL Server
```kotlin
implementation("com.microsoft.sqlserver:mssql-jdbc:13.2.1.jre11")
```

## Connection Pooling (Recommended)

For production use, consider using a connection pool like [HikariCP](https://github.com/brettwooldridge/HikariCP):

```kotlin
implementation("com.zaxxer:HikariCP:5.0.1")
```

## Next Steps

Now that Kapper is installed, continue with the [Quick Start Guide](./quick-start.md) to see it in action.
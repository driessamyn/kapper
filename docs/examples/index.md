# Examples

Explore real-world examples of using Kapper in various scenarios.
All examples include complete, runnable code that you can use as a starting point for your own projects.

## Quick Navigation

### [Kotlin Examples](./kotlin/basic-crud.md)
- [Basic CRUD Operations](./kotlin/basic-crud.md)
- [Advanced Queries](./kotlin/advanced-queries.md) 
- [Transaction Handling](./kotlin/transactions.md)
- [Coroutines & Flow](./kotlin/coroutines.md)

### [Java Examples](./java/basic-crud.md)
- [Basic CRUD with Records](./java/basic-crud.md)
- [Java Records Support](./java/records.md)

### [Framework Comparisons](./comparisons/hibernate.md)
- [Kapper vs Hibernate](./comparisons/hibernate.md)
- [Kapper vs Ktorm](./comparisons/ktorm.md)

## Database Schema

All examples use a superhero-themed database schema:

```sql
CREATE TABLE super_heroes (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    age INT
);

CREATE TABLE villains (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE battles (
    super_hero_id UUID REFERENCES super_heroes(id),
    villain_id UUID REFERENCES villains(id),
    battle_date TIMESTAMP NOT NULL,
    updated_ts TIMESTAMP NOT NULL,
    PRIMARY KEY (super_hero_id, villain_id, battle_date)
);
```

Database setup scripts are available for:
- [PostgreSQL](https://github.com/driessamyn/kapper/blob/main/examples/db/postgresql.sql)
- [MySQL](https://github.com/driessamyn/kapper/blob/main/examples/db/mysql.sql)

## Project Structure

The complete examples are available in the [examples directory](https://github.com/driessamyn/kapper/tree/main/examples) of the main repository:

```
examples/
├── kotlin-example/          # Kotlin examples and tests
├── java-example/           # Java examples and tests
├── db/                     # Database setup scripts
└── docs/                   # Blog posts and additional documentation
```

## Running the Examples

1. Clone the repository:
   ```bash
   git clone https://github.com/driessamyn/kapper.git
   cd kapper/examples
   ```

2. Set up your database using the scripts in `/db`

3. Run the examples:
   ```bash
   # Kotlin examples
   ./gradlew :kotlin-example:test
   
   # Java examples  
   ./gradlew :java-example:test
   ```

## Key Concepts Demonstrated

- **Auto-mapping**: How Kapper automatically maps SQL results to Kotlin data classes and Java records
- **Parameter binding**: Different ways to pass parameters to SQL queries
- **Transaction handling**: Managing database transactions effectively
- **Error handling**: Proper exception handling and resource management
- **Performance patterns**: Writing efficient queries and bulk operations
- **Database compatibility**: Working with different database engines

Ready to dive in? Start with [Basic CRUD Operations](./kotlin/basic-crud.md) or jump to any specific example that interests you.
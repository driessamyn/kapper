# Documentation Style Guide

This style guide ensures consistent, clear, and professional documentation across the Kapper project.
It serves as a reference for both human contributors and LLMs when creating or reviewing documentation content.

## Language and Tone

### British English (BBC Standard)
- **Spelling**: Use British spellings throughout
  - `colour` not `color`
  - `behaviour` not `behavior`
  - `centre` not `center`
  - `optimise` not `optimize`
  - `realise` not `realize`
  - `organisation` not `organization`
  - `initialise` not `initialize`
  - `analyse` not `analyze`

- **Punctuation**: Follow BBC style
  - Use single quotes for quotations and code references: `'query()'` not `"query()"`
  - Oxford comma is optional but be consistent within documents
  - Full stops outside quotation marks unless the full sentence is quoted

### Voice and Tone
- **Active voice**: Prefer active over passive voice
  - ✅ "Kapper maps results to data classes"
  - ❌ "Results are mapped to data classes by Kapper"

- **Direct and concise**: Get to the point quickly
  - ✅ "Use `query()` to select data"
  - ❌ "In order to perform data selection operations, you should utilise the `query()` method"

- **Professional but friendly**: Accessible to developers of all levels
- **Confident tone**: Avoid unnecessary qualifiers
  - ✅ "This approach improves performance"
  - ❌ "This approach might potentially improve performance"

## Structure and Organisation

### Document Hierarchy
1. **Page title** (H1): One per document, describes the main topic
2. **Major sections** (H2): Key concepts or categories
3. **Subsections** (H3): Specific topics within major sections
4. **Minor headings** (H4): Use sparingly, only when necessary

### Content Structure
Every guide document should follow this pattern:
```markdown
# Document Title

Brief introduction explaining what this document covers and why it matters.

## Core Concept 1
Explanation with practical examples.

### Subsection
More detailed information.

## Core Concept 2
Continue with other major concepts.

## Next Steps
- Links to related documentation
- Suggested follow-up reading
```

### Navigation Elements
- **Cross-references**: Link to related concepts within the documentation
- **Next Steps**: Every guide should end with suggested next reading
- **Code examples**: Include complete, runnable examples where possible

## Code Examples

### Code Block Standards
- **Language specification**: Always specify the language
  ```kotlin
  // ✅ Good
  data class User(val id: Long, val name: String)
  ```

- **Complete examples**: Provide context, not just fragments
  ```kotlin
  // ✅ Include imports and context
  import net.samyn.kapper.query
  
  data class User(val id: Long, val name: String)
  
  val users = connection.query<User>("SELECT id, name FROM users")
  ```

- **Real-world relevance**: Use meaningful variable names and realistic scenarios
  - ✅ `SuperHero`, `User`, `Product`
  - ❌ `Foo`, `Bar`, `Example`

### SQL Formatting
- **Keywords in uppercase**: `SELECT`, `FROM`, `WHERE`, `JOIN`
- **Multi-line for complex queries**: Format for readability
  ```sql
  SELECT u.id, u.name, COUNT(o.id) as order_count
  FROM users u
  LEFT JOIN orders o ON u.id = o.user_id
  WHERE u.active = true
  GROUP BY u.id, u.name
  ```

- **Consistent indentation**: Use 4 spaces for SQL alignment

### Code Comments
- **Explain the why, not the what**: Focus on reasoning
  ```kotlin
  // Cache the mapper to avoid reflection overhead
  private val userMapper = Kapper.mapper<User>()
  ```

- **Use comments sparingly**: Well-named code should be self-documenting

## Formatting Conventions

### Lists
- Use **bullet points** for unordered information
- Use **numbered lists** for sequential steps or procedures
- Keep list items parallel in structure:
  ```markdown
  ✅ Good:
  - Configure the database connection
  - Define your data classes  
  - Execute your first query
  
  ❌ Avoid:
  - Configure the database connection
  - Data class definition
  - You should execute your first query
  ```

### Emphasis
- **Bold** for UI elements, important concepts, and key terms on first mention
- *Italic* for emphasis within sentences
- `Code formatting` for class names, method names, and inline code

### Links
- **Descriptive text**: Link text should make sense out of context
  - ✅ `See [transaction handling](./transactions.md) for details`
  - ❌ `Click [here](./transactions.md) for more information`

- **Relative paths**: Use relative links within the documentation
- **External links**: Open in new tabs where appropriate

### Tables
- Use tables for structured comparisons or reference data
- Include headers for all columns
- Keep cell content concise

| Feature | Kapper | Alternative |
|---------|--------|-------------|
| SQL-first | ✅ Yes | ❌ No |
| Lightweight | ✅ Yes | ❌ No |

## Content Guidelines

### Technical Accuracy
- **Test all code examples**: Ensure examples work as shown
- **Version-specific information**: Indicate when features require specific versions
- **Error handling**: Include appropriate error handling in examples

### Inclusivity
- **Avoid assumptions**: Don't assume reader's background or setup
- **Multiple approaches**: Show different ways to solve problems when relevant
- **Accessibility**: Use clear language and explain technical terms

### Examples and Scenarios
- **Progressive complexity**: Start simple, build up to advanced concepts
- **Practical relevance**: Use examples that reflect real development needs
- **Complete context**: Include necessary imports, setup, and teardown

### Performance Considerations
- **Highlight performance implications**: Note when examples are optimised for clarity vs. performance
- **Benchmark data**: Include actual numbers when discussing performance
- **Alternatives**: Show performance trade-offs when relevant

## Common Patterns

### API Documentation
When documenting methods or functions:

```markdown
### `query<T>(sql: String, vararg parameters: Pair<String, Any?>): List<T>`

Executes a SELECT statement and returns a list of mapped objects.

**Parameters:**
- `sql`: The SQL SELECT statement to execute
- `parameters`: Named parameters for the query

**Returns:** List of objects mapped from the result set

**Example:**
```kotlin
val users = connection.query<User>(
    "SELECT * FROM users WHERE age > :minAge",
    "minAge" to 18
)
```

**Throws:**
- `SQLException`: If the database query fails
- `KapperMappingException`: If result mapping fails
```

### Error Documentation
- **Common scenarios**: Document typical error cases
- **Solutions**: Provide clear resolution steps
- **Example fixes**: Show corrected code

### Migration Guides
- **Before/After**: Clear comparison of old vs. new approaches
- **Step-by-step**: Break complex migrations into manageable steps
- **Rollback plans**: Document how to reverse changes

## Review Checklist

Before publishing or merging documentation:

### Content Review
- [ ] Follows British English conventions
- [ ] Uses active voice and direct language
- [ ] Includes complete, tested code examples
- [ ] Links to related documentation
- [ ] Includes "Next Steps" section

### Technical Review
- [ ] Code examples compile and run
- [ ] SQL examples use correct syntax
- [ ] Version-specific features are noted
- [ ] Performance implications are discussed

### Structure Review
- [ ] Clear hierarchy with appropriate headings
- [ ] Logical flow from simple to complex concepts
- [ ] Consistent formatting throughout
- [ ] Navigation elements are functional

### Style Review
- [ ] Consistent terminology throughout
- [ ] Professional but accessible tone
- [ ] Proper use of emphasis and formatting
- [ ] Links are descriptive and functional

## Terminology Guidelines

### Consistent Usage
- **Kapper** (not "the Kapper library" or "kapper")
- **data class** (not "POJO" or "data object")
- **auto-mapping** (not "automatic mapping")
- **result set** (not "resultset" or "result-set")

### Technical Terms
- **Define on first use**: Explain technical terms when first mentioned
- **Consistent definitions**: Use the same explanation throughout all documents
- **Avoid jargon**: Explain or replace overly technical language

### Database Terms
- **SQL** (always uppercase)
- **JDBC** (always uppercase)
- **Connection** (capitalised when referring to the Java class)
- **database** (lowercase unless part of a proper noun)

## File Organisation

### File Naming
- Use kebab-case: `performance-tuning.md`
- Be descriptive: `migration-guide.md` not `migration.md`
- Group related files in directories

### Directory Structure
```
docs/
├── guide/           # Core documentation
├── examples/        # Practical examples
├── performance/     # Benchmarks and tuning
├── api/            # Generated API docs
└── assets/         # Images and other resources
```

### Cross-References
- **Maintain link integrity**: Update links when moving files
- **Use relative paths**: Keep documentation portable
- **Link both ways**: Create bi-directional navigation where appropriate

## Maintenance

### Regular Updates
- **Review quarterly**: Check for outdated information
- **Version alignment**: Ensure examples work with current releases
- **Link validation**: Verify all internal and external links

### Community Contributions
- **Style guide reference**: Direct contributors to this guide
- **Review process**: Ensure new content follows these standards
- **Template provision**: Provide templates for common document types

---

This style guide should be referenced by all contributors and used as a checklist for documentation reviews. It ensures that Kapper's documentation maintains a professional, consistent, and helpful standard across all content.

## Resources

- [BBC Style Guide](https://www.bbc.co.uk/academy/en/collections/news-style-guide) - Reference for British English
- [Government Digital Service Style Guide](https://www.gov.uk/guidance/style-guide) - Additional UK English guidance
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) - For code formatting
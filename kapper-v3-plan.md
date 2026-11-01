# Kapper v3 Plan

**Status:** draft for review
**Basis:** `kapper-review-2026-07.md` (28 July 2026, commit `c77a3c0`), verified against the working tree at HEAD `db83912`.
**Scope:** `core`, `coroutines`, build/publishing, docs site, and agent-facing surface.

This document is the plan. It is grounded in the source, not only the review. Where the review is
wrong or vague, that is stated rather than smoothed over (see "Review-quality notes").

---

## Step 1 — Grounding

### 1a. Kapper's stated philosophy (verbatim)

Stated explicitly in `docs/guide/philosophy.md` (and a shorter three-point version in `README.md`
and `docs/guide/index.md`). Not inferred.

**Core Principles** (headings and key sentences, verbatim):

> ### 1. SQL is the Best Query Language
> **SQL has evolved for decades to be the optimal language for database operations.**
> Rather than hiding SQL behind abstractions, Kapper embraces it

> ### 2. Minimal Abstraction
> **Kapper provides just enough abstraction to make JDBC comfortable, without hiding the underlying mechanisms.**

> ### 3. Transparency
> **What you write is what gets executed. No magic, no hidden behaviour.**

> ### 4. Performance by Design
> **Kapper is designed to be as fast as hand-written JDBC while providing convenience.**

> ### 5. Kotlin-First Design
> **Kapper leverages Kotlin's strengths while remaining Java-compatible.**

**"No hidden costs"** (verbatim, under Transparency):

> - No lazy loading that triggers unexpected queries
> - No automatic query generation that might be inefficient
> - No caching layers that can become stale
> - No connection management magic that can cause leaks

**"Philosophy in Practice"** (verbatim):

> ### API Design
> - Extension functions over new classes
> - Familiar JDBC concepts
> - Minimal method overloading
>
> ### Error Handling
> - Clear, actionable error messages
> - Preserve original SQL exceptions
> - Fail fast with helpful context
>
> ### Documentation
> - SQL examples for every feature
> - Performance characteristics clearly stated
> - Real-world usage patterns

**Conclusion** (verbatim):

> Kapper's philosophy prioritizes **simplicity, transparency, and performance** over feature richness.

**Design decisions** (verbatim headings): "Why Not a Query Builder?", "Why Not Active Record?",
"Why No Automatic Relationships?".

The README states the same three principles and adds: *"Kapper prefers extension of existing APIs
rather than abstraction of them."*

### 1b. Language and scope

**What it is:** a lightweight, Dapper-inspired ORM written in **Kotlin**, targeting the **JVM**.
Implemented as **extension functions on `java.sql.Connection`** (plus a `Kapper` interface for Java
callers), with a separate `kapper-coroutines` module. Maps result sets to **Kotlin data classes and
Java records**. Java-compatible.

**What it deliberately is not** (verbatim from `docs/guide/index.md`):

> It does _not_ generate code, it does not introduce another layer of abstraction, it is never intrusive.
> It does _not_ hide SQL, instead it embraces the fact that SQL is the best language for DB interaction.

Plus, from the philosophy page: not a query builder, not Active Record, no automatic relationship
loading, no lazy loading, no hidden queries, no connection-management magic.

### 1c. Current public API surface (outline)

Package `net.samyn.kapper`:

- **`Kapper`** interface — `query`, `querySingle`, `execute`, `executeAll`, `executeReturning`
  (each with a class-based and a custom-`mapper` overload; `executeReturning` also has an object-arg
  variant with separate `R`/`A` types). Companion: `createInstance()`, `instance` (lazy singleton),
  `mapperRegistry` (static global).
- **`Connection` extension functions** — `query<T>` (reified + `KClass` + custom-mapper),
  `querySingle<T>` (same), `execute` (vararg + object), `executeAll`, `executeReturning` (6 overloads),
  `withTransaction`.
- **`DataSource.withTransaction`**.
- **`Mapper<T>`** — `fun interface { createInstance(ResultSet, Map<String, Field>): T }`.
- **`MapperRegistry`** — `register`, `registerIfAbsent`, `get`.
- **`Field`** data class — `(columnIndex, type: JDBCType, typeName, dbFlavour)`.
- **`DbFlavour`** enum — POSTGRESQL, MYSQL, SQLITE, ORACLE, MSSQLSERVER, DUCKDB, FIREBIRD, UNKNOWN.
- **Exceptions** — `KapperMappingException`, `KapperUnsupportedOperationException`,
  `KapperParseException`, `KapperResultException`, `KapperQueryException`.
- **Typealiases** — `Args = Map<String, Any?>`, `ArgMapper<T> = Pair<String, (T) -> Any?>`.

Package `net.samyn.kapper.coroutines`:

- `DataSource.withConnection(context, block)` (suspend), `Connection.queryAsFlow<T>(...)`.

Package `net.samyn.kapper.internal` (**not stable**, but imported by the coroutines module):
`Query`, `parseQuery`, `KapperImpl`, `executeQuery`, `extractFields`, `getDbFlavour`, `autoConverter`,
`sqlTypesConverter`, `logger`, and public `ResultSet.getNullableX` extensions.

---

## Step 2 — Scoring model

Each review finding is scored on **Value** (reach × frequency × severity) and **Cost** (LOC + API
churn + migration + maintenance + review risk), then bucketed by ratio.

| Wave | Bar |
|---|---|
| 0 — Free | No API change, under half a day each |
| 1 — Quick wins | Additive API, small diff, no break |
| 2 — Enablers | Additive but larger, or breaking with trivial migration |
| 3 — Strategic | Breaking with real migration, or needs design work |

**Effort estimates need a sanity check.** They are calibrated from the review's own scale
(S < 1 day, M a few days, L a week+) and the size of the files involved.

**Taxonomy note:** a few findings have no API change but are ~1 day. They are kept in Wave 0
(grouped by API-change, the more useful axis) and flagged where they exceed half a day.

---

## Step 3 — The plan

### Wave 0 — Free (no API change)

| # | Item | Effort (sanity-check) |
|---|---|---|
| 0.1 | DIAG-1 self-describing mapping failures | S (~0.5–1d) |
| 0.2 | DIAG-2 self-describing conversion failures | S (~0.5d) |
| 0.3 | CORO-2 acquire connection inside `withContext` | S (~1h) |
| 0.4 | CORO-1 make `queryAsFlow` cold | S (~0.5d) |
| 0.5 | PARSE-2 cache parsed templates | S (~0.5–1d) |
| 0.6 | PERF-5 hoist `RecordMapper` constructor | S (~1h) |
| 0.7 | PERF-6 static `JDBCType` map | S (~1h) |
| 0.8 | PERF-2 resolve `DbFlavour` once | S (~0.5d) |
| 0.9 | API-9 short-circuit `querySingle` | S (~1h) |
| 0.10 | DIAG-3 parse failures point at the character | S (~0.5d) |
| 0.11 | PKG-1 fix the "Zero Dependencies" claim | S (~1h) |
| 0.12 | PKG-2 fix duplicated POM metadata | S (~1h) |
| 0.13 | PKG-3 add `Automatic-Module-Name` | S (~1h) |
| 0.14 | DOCS-2 fix docs link inconsistency | S (~15m) |
| 0.15 | DOCS-4 document threading/resource contract | S (~0.5d) |
| 0.16 | AGENT-1 add `AGENTS.md` | S (~0.5d) |
| 0.17 | AGENT-6 snippets pipeline (content deferred to Wave 1) | S (~0.5–1d) |
| 0.18 | STRAT-3 positioning | S (~0.5d) |

**0.1 DIAG-1 — self-describing mapping failures.**
- **Problem:** `Kapper.query` catches and rethrows as `KapperMappingException("Error creating mapper for $clazz", e)`; `KotlinDataClassMapper` computes a `missing` list and discards the rest.
- **Change:** Emit target class, columns returned with JDBC types, constructor params expected, the specific mismatch, and a hint. Assert on message content in tests.
- **Effort:** S. **Risk:** Low; message-only. **Why now:** highest value-per-line in the review; the entire feedback loop for an agent.

**0.2 DIAG-2 — self-describing conversion failures.** Same shape for `AutoConverter`/`Converters`; add column label, index, driver type name, flavour, and a pointer to converter registration. **Risk:** Low.

**0.3 CORO-2 — acquire the connection inside `withContext`.** `DatasourceWithConnection.kt` currently does `this.connection.use { withContext(context) { block(connection) } }`, so `getConnection()`/`close()` block on the caller's thread. One-line fix. **Risk:** Low; add a test asserting acquisition happens on the dispatcher.

**0.4 CORO-1 — make `queryAsFlow` cold.** `KapperKotlinFlowQueryFun.kt` executes the query eagerly outside `flow { }`, so an uncollected flow leaks the `ResultSet`/`Statement`. Move execution inside the builder and use `.use { }`. **Risk:** Low–medium (cancellation interaction); test uncollected-flow and mid-collection-cancel.

**0.5 PARSE-2 — cache parsed templates.** `KapperImpl` default `queryFactory = { Query(it) }` re-scans per call. Add a bounded, hand-written `ConcurrentHashMap` cache keyed on the template (no Caffeine). **Risk:** Low. Interacts with PARSE-3.

**0.6 PERF-5 — hoist `RecordMapper` constructor.** `getDeclaredConstructor(*recordComponents.map { it.type }...)` runs per row. Resolve in `init`. **Risk:** Low.

**0.7 PERF-6 — static `JDBCType` map.** `Int.jdbcType()` does a linear scan per column per query. **Risk:** Low.

**0.8 PERF-2 — resolve `DbFlavour` once.** `getDbFlavour()` is called twice per query. Thread it through; cache on product-name string, not the `Connection`. **Risk:** Low.

**0.9 API-9 — short-circuit `querySingle`.** It materialises the whole result set before checking `size > 1`. Stop at two. **Risk:** Low.

**0.10 DIAG-3 — parse failures point at the character.** Add the template with a caret. **Risk:** Low.

**0.11 PKG-1 — fix the "Zero Dependencies" claim.** `README.md` line 58 and `docs/index.md` "Zero Dependencies" card are false: the POM declares `kotlin-stdlib`, `kotlin-reflect` (runtime), `slf4j-api` (runtime). Restate honestly. **Risk:** None. (The review's *preferred* fix — make it true via STRAT-1 — conflicts with philosophy.)

**0.12 PKG-2 — duplicated POM metadata.** `kapper.library-publish.gradle.kts` has both a `publishing { }` publication and a `deployer { projectInfo { } }` block; licence/developer blocks repeat three times. **Risk:** Low.

**0.13 PKG-3 — `Automatic-Module-Name`.** `kapper.library-conventions.gradle.kts` sets only `Implementation-Title`/`-Version`. Add `net.samyn.kapper` and the coroutines equivalent. **Risk:** Low.

**0.14 DOCS-2 — link inconsistency.** README uses `kapper.samyn.net` as text but links to `driessamyn.github.io/kapper`. **Risk:** None.

**0.15 DOCS-4 — threading/resource-ownership contract.** Who closes the connection/result set, what is safe across threads, what happens on cancellation. **Risk:** None.

**0.16 AGENT-1 — `AGENTS.md`.** Hand-written, project-specific only (Gradle commands, `integrationTest` + Testcontainers, ktlint-on-compile, Kover 90% gate, conventional-commit release, `internal` is not stable). **Risk:** None.

**0.17 AGENT-6 — snippets pipeline.** Build the module, manifest, extractor and CI drift gate; land the snippets that compile against the current API. The post-v3 snippets (scalar, enum, `Row`, streaming, transaction-with-value) are deferred to Wave 1, because they depend on Wave 1 correctness. See the retrieval-chain breakdown and the spike.

**0.18 STRAT-3 — positioning.** Keep the existing identity and **add** the era framing; do not
replace the framing. The argument is that the abstraction layer's value drops as the SQL becomes
free: heavyweight frameworks like Hibernate and Spring Data exist to spare you from writing SQL, so
when the SQL is already written they add complexity, not value.

Wording (README intro, after the existing identity sentence):

> Kapper is built for the era when most SQL is written by an agent — and to be delightful for humans
> and agents alike. When the SQL is already written, a heavyweight framework like Hibernate or Spring
> Data adds complexity, not value. Kapper keeps SQL front and centre, hides nothing, and tells you how
> to fix it when something goes wrong.

Decisions taken:

- **Keep the original identity sentence verbatim** ("lightweight, Dapper-inspired ORM … embraces SQL
  rather than abstracting it away"). The era is a second dimension, not a pivot.
- **Use "agent-written SQL"** consistently (not "model-written").
- **Name Hibernate and Spring Data** — the contrast is the point, and vagueness weakens it.
- **Keep the hero text** as the original ("A Fresh Look at ORMs for Kotlin and the JVM"). The era
  belongs in the intro as the argument, not the headline.
- **"Delightful for humans and agents alike"** is the quality bar, and it is what Wave 0
  (self-describing errors) and the retrieval chain (`llms.txt`, snippets, skill) exist to earn.

**Risk:** None.

### Wave 1 — Quick wins (additive API, small diff, no break)

| # | Item | Effort |
|---|---|---|
| 1.1 | TYPE-2 enum support | S |
| 1.2 | TYPE-6 scalar/single-column mapping | S |
| 1.3 | TYPE-7 `Map<String, Any?>` row mapping | S |
| 1.4 | CORO-4 suspending `execute`/`querySingle` | S |
| 1.5 | CORO-3 `flowOn` + threading docs | S |
| 1.6 | PERF-7 chunk `executeAll` batches | S |
| 1.7 | TYPE-1 user-extensible converter registry | M |
| 1.8 | TYPE-4 `JDBCType.OTHER` via registry | M |
| 1.9 | TYPE-5 missing JDK/Kotlin types | M |
| 1.10 | DIAG-4 strict mode | M |
| 1.11 | API-7 generated-keys API | M |
| 1.12 | API-8 streaming `Sequence` in core | M |
| 1.13 | API-10 `Row` abstraction | M |
| 1.14 | DOCS-1 support matrix | M |
| 1.15 | DOCS-3 type support matrix | M |
| 1.16 | AGENT-2 `llms.txt` / `llms-full.txt` | M |
| 1.17 | AGENT-4 distributable skill | M |
| 1.18 | AGENT-7 `kapper-test` module | M |
| 1.19 | PKG-4 GraalVM reachability metadata | M |
| 1.20 | AGENT-3 submit to Context7 | S |

**1.1 TYPE-2 — enum support.** No enum handling exists; a `String` against an enum target falls through to `KapperUnsupportedOperationException`. Add name-based (case-insensitive) read + write, with a clear `KapperMappingException` naming the column, value, and valid constants. **Risk:** Low. **Why now:** table stakes; the docs already claim it works.

**1.2 TYPE-6 — scalar mapping.** `createAutoMapper` accepts only `isData`/`isRecord` and throws otherwise, so `query<Int>` fails. Add scalar mapping + a `queryScalar`/`querySingleValue` convenience; fail clearly on multi-column. **Risk:** Low.

**1.3 TYPE-7 — `Map<String, Any?>` rows.** Dapper's `dynamic`; useful for exploratory and agent-driven queries. **Risk:** Low. **Tension:** against "Kotlin-First Design"/explicit types.

**1.4 CORO-4 — suspending `execute`/`querySingle`.** Only `queryAsFlow`/`withConnection` exist. **Risk:** Low.

**1.5 CORO-3 — `flowOn` + threading docs.** No `flowOn` anywhere; blocking JDBC runs on the collector. **Risk:** Low (behaviour change, documented).

**1.6 PERF-7 — chunk `executeAll`.** One cleanup lambda per object, no flush; memory grows linearly. Add optional `batchSize` + `executeLargeBatch`. **Risk:** Low.

**1.7 TYPE-1 — converter registry.** The `converters` map is `private val`; the source comment says "we can/should extend this to allow users to register custom converters". Add a public, symmetrical read/write registry. **Risk:** Medium — decide instance-scoped vs global (interacts with PKG-5). **Why now:** keystone — every future type gap becomes the user's problem.

**1.8 TYPE-4 — `OTHER` types.** The `else` branch handles only `uuid`/`binary_float`/`binary_double` by name, then throws. Route unknown type names through the TYPE-1 registry; ship `json`/`jsonb`→`String` and `vector`→`FloatArray`/`List<Float>`. **Risk:** Medium. **Depends on 1.7.**

**1.9 TYPE-5 — missing types.** `OffsetDateTime`, `ZonedDateTime`, `OffsetTime`, `BigInteger`, `Duration`, `Period`, `Year`, `YearMonth`, `kotlin.time.Instant`, value classes, `Optional<T>`. Fold in the unmerged `kotlin-uuid-support` branch. kotlinx-datetime in a separate optional module. **Risk:** Low–medium.

**1.10 DIAG-4 — strict mode.** Off by default; fails on unbound tokens, unmatched columns, silently-defaulted params. **Risk:** Low (opt-in).

**1.11 API-7 — generated keys.** `executeReturning` needs `RETURNING`, which MySQL/older MSSQL lack. Add a `RETURN_GENERATED_KEYS` path. **Risk:** Medium.

**1.12 API-8 — streaming `Sequence`.** `query` always materialises. Add a `Sequence` variant with explicit resource management. **Risk:** Medium.

**1.13 API-10 — `Row` abstraction.** The custom-mapper signature `(ResultSet, Map<String, Field>) -> T` leaks `Field` and is hard to remember. Add a thin `Row` wrapper; keep the old signature underneath. **Risk:** Medium. **Tension:** "Minimal Abstraction".

**1.14 DOCS-1 — support matrix.** Replace "probably many others" with a tiered matrix. Merge the unmerged compat branches (ClickHouse, TiDB, YugabyteDB, StarRocks, Firebird). **Risk:** None.

**1.15 DOCS-3 — type support matrix.** Generated from tests so it cannot drift. **Risk:** None.

**1.16 AGENT-2 — `llms.txt`/`llms-full.txt`.** VitePress + markdown, so a small build task; also emit `.md` per page. **Risk:** Low.

**1.17 AGENT-4 — distributable skill.** `SKILL.md` covering API, binding rules, type table, transaction patterns, top-10 mistakes. **Risk:** Low.

**1.18 AGENT-7 — `kapper-test` module.** In-memory harness (SQLite/DuckDB) so an agent can close the loop in seconds. **Risk:** Medium (new module to maintain).

**1.19 PKG-4 — GraalVM metadata.** Reflection-based automapper breaks native image. Ship `reflect-config.json` as a stopgap. **Risk:** Low–medium.

**1.20 AGENT-3 — Context7 submission.** Depends on AGENT-2. **Risk:** None.

### Wave 2 — Enablers (additive but larger, or breaking with trivial migration)

| # | Item | Effort |
|---|---|---|
| 2.1 | API-1 `withTransaction` returns a value | S |
| 2.2 | API-3 nested `withTransaction` | S |
| 2.3 | PARSE-1 literal/comment-aware tokeniser | M |
| 2.4 | PARSE-3 `IN` list expansion | M |
| 2.5 | API-4 suspending transaction API | M |
| 2.6 | API-2 isolation/read-only/savepoint | M |
| 2.7 | PERF-3 per-row normalisation elimination | M |
| 2.8 | PERF-4 positional invoker | M |
| 2.9 | PKG-5 scope `MapperRegistry` | M |
| 2.10 | PKG-6 lock down public API surface | M |

**2.1 API-1 — `withTransaction` returns a value.** Both variants return `Unit`; forces a `var` outside the block. Make both generic in `T`. **Risk:** Source-compatible; binary break. **Break** (see list).

**2.2 API-3 — nested `withTransaction`.** A nested call commits the outer transaction — a silent data-integrity trap. Detect in-progress and either savepoint or fail fast. **Risk:** Behaviour change. **Break** (see list).

**2.3 PARSE-1 — tokeniser rewrite.** The scanner has no awareness of string literals, comments, or quoted identifiers. Time literals, emails, `:param::jsonb`, `arr[1:2]` all throw; a `:id` in a comment silently becomes a placeholder and shifts indexes. Rewrite to skip literals/comments/identifiers/dollar-quotes; treat any non-token char as a terminator; reserve `KapperParseException` for genuinely malformed templates. Decide `-` (drop it) and literal `?` (`??` escape). **Risk:** Medium — the `-` change is a break; `QueryParserTest` must be rewritten (it currently asserts `'`, `:`, `]` throw). **Break** (see list).

**2.4 PARSE-3 — `IN (:ids)` expansion.** No expansion exists; non-array DBs have no workaround. Rewrite the placeholder to `(?, ?, ?)` at bind time; cache the template and derive final SQL per call. **Risk:** Medium — interacts with PARSE-2; changes behaviour for `IN (:ids)` with a collection. **Break** (see list).

**2.5 API-4 — suspending transaction API.** Coroutine users have no transaction support. Add `suspend fun <T> DataSource.withTransaction(context, block)` with the connection acquired inside the dispatcher. **Risk:** Medium.

**2.6 API-2 — isolation/read-only/savepoint.** No isolation level, read-only flag, or savepoint API. **Risk:** Medium.

**2.7 PERF-3 — eliminate per-row normalisation.** Per row: `FieldsConverter` allocates a `ColumnValue` per column; `createInstance` calls `associateBy { it.name.normalisedColumnName() }` (three string allocations per column); an args map is built. Compute the index→param binding once per result-set shape. **Risk:** Medium. **Why now:** enables PERF-4.

**2.8 PERF-4 — positional invoker.** `constructor.callBy(args)` is the slow kotlin-reflect path. Use `call(*args)` or a `MethodHandle`/`LambdaMetafactory` invoker once per class. **Risk:** Medium. **Depends on 2.7.**

**2.9 PKG-5 — scope `MapperRegistry`.** Static mutable singleton on the companion; `register` throws on collision, no `unregister`/`clear`; `createInstance()` implies per-instance state while the registry is shared. Scope to the `Kapper` instance, keep a default global, add `unregister`/`clear`. **Risk:** Medium. **Break** (see list).

**2.10 PKG-6 — lock down the public API.** `net.samyn.kapper.internal` is public in practice (the coroutines module imports it; `SQLTypesConverter` declares public `ResultSet.getNullableX`). Enable `explicitApi()`, add `binary-compatibility-validator` with a checked-in `.api` dump, gate CI, and mark the rest `@KapperInternalApi`. **Risk:** Medium. **Break** (see list).

### Wave 3 — Strategic (breaking with real migration, or needs design work)

| # | Item | Effort |
|---|---|---|
| 3.1 | API-5 multi-mapping for joins | L |
| 3.2 | API-6 multiple result sets | M |
| 3.3 | STRAT-1 KSP-generated mappers | L |
| 3.4 | STRAT-2 compile-time SQL validation | L |
| 3.5 | AGENT-5 schema-aware MCP server | L |

**3.1 API-5 — multi-mapping.** No `splitOn` equivalent; the most conspicuous gap for a "Dapper for Kotlin" pitch. Design sketch: `query<Order, Customer, OrderWithCustomer>(sql, splitOn = "id") { order, customer -> ... }` plus a one-to-many grouping helper. **Risk:** High (new API surface, design).

**3.2 API-6 — multiple result sets.** Dapper's `QueryMultiple`. **Risk:** Medium.

**3.3 STRAT-1 — KSP-generated mappers.** Collapses four problems (removes `kotlin-reflect`, removes per-row reflection, native-image safe, enables compile-time verification). **Risk:** High. **⚠️ Conflicts with a stated principle — see "Requires philosophy change".**

**3.4 STRAT-2 — compile-time SQL validation.** Parse the SQL literal at compile time and check named params against call-site args. **Risk:** High. **⚠️ Same conflict.**

**3.5 AGENT-5 — schema-aware MCP server.** Introspect schema, generate data classes, validate templates with the real tokeniser, `EXPLAIN`. **Risk:** High (new module, ongoing maintenance). **Why now:** the most defensible differentiator, but it depends on PARSE-1 and TYPE-1.

---

## Discoverability and agent awareness

"Letting agents know about Kapper" is three distinct problems that fail independently, across two
audiences.

| Problem | Question | Artefacts | If it fails |
|---|---|---|---|
| **Retrieval** | Does the agent find Kapper at all? | Context7, `llms.txt`, docs crawlability, MCP registries, training data | The agent picks Exposed/Hibernate by default |
| **Comprehension** | Once found, does it get it right first time? | `llms-full.txt`, skill, snippets, type matrix | The agent writes plausible-but-wrong Kapper code |
| **Verification** | Can the agent check its own work? | `kapper-test`, MCP `validate`, `--check` | The agent ships code it can't test |

Two audiences:

- **Agents working *on* Kapper** (contributors) → `AGENTS.md` (AGENT-1).
- **Agents working *with* Kapper** (consumers) → skill (AGENT-4), `llms-full.txt` (AGENT-2),
  snippets (AGENT-6), MCP (AGENT-5), `kapper-test` (AGENT-7).

### The gate: correctness before discoverability

The review is emphatic and correct:

> Do not start this before Milestone 1, because driving traffic to a library that fails on a time
> literal is worse than not driving traffic at all.

Discoverability is a **multiplier on correctness**. If PARSE-1, DIAG-1, TYPE-2 and TYPE-6 are not
done, every agent attracted writes code that fails on the first attempt, and the failure is
attributed to Kapper. The retrieval tasks (AGENT-2, AGENT-3) therefore belong *after* Wave 0 and the
Wave 1 correctness items, even though they are individually cheap.

Exception: **AGENT-1 and STRAT-3 are safe to do immediately** — they do not drive consumer traffic.

### Tasks, wave placement, dependencies

| # | Task | Problem | Wave | Effort | Depends on |
|---|---|---|---|---|---|
| AGENT-1 | `AGENTS.md` | Comprehension (contributors) | 0 | S | — |
| STRAT-3 | Positioning | Retrieval (training data) | 0 | S | — |
| AGENT-6 | Canonical snippets | Comprehension | 0 | S | CI compile step |
| AGENT-3 | Context7 submission | Retrieval | 1 | S | AGENT-2 |
| AGENT-2 | `llms.txt` / `llms-full.txt` | Retrieval + comprehension | 1 | M | docs build |
| AGENT-4 | Distributable skill | Comprehension | 1 | M | — |
| AGENT-7 | `kapper-test` module | Verification | 1 | M | — |
| AGENT-5 | Schema-aware MCP server | Retrieval + verification | 3 | L | PARSE-1, TYPE-1 |

AGENT-2 is the keystone of retrieval (AGENT-3 depends on it). AGENT-5 is the keystone of
verification but is gated on the parser and converter registry. AGENT-4 and AGENT-7 are independent.

### What the review misses on discoverability

1. **No single source of truth — so the artefacts will drift.** AGENT-2, AGENT-4, AGENT-6 and the
   existing `docs/guide/*` all restate the same API surface and examples. The docs *already*
   contradict the code (enum, scalar, custom-mapper lambda). Four more copies will drift within a
   release. Generate `llms-full.txt` and the skill's API section from one machine-readable source,
   and extract snippets from compiled code.
2. **`llms.txt` will land at the wrong path.** The docs site has `base: '/kapper/'`, so `llms.txt`
   would be served at `driessamyn.github.io/kapper/llms.txt`, not at a domain root. The convention
   expects it at the root. This connects to DOCS-2 — a custom domain is a retrieval prerequisite for
   AGENT-2, not a cosmetic fix.
3. **The changelog is invisible to agents.** `docs/changelog.md` renders `<DynamicChangelog />`, a
   Vue component that fetches GitHub releases at runtime. An agent fetching the static HTML or
   markdown gets an empty page.
4. **No success metric.** The review cites the Mintlify "half of docs traffic is agents" figure but
   proposes no way to know whether any of this worked.
5. **No maintenance story for Context7.** AGENT-3 is framed as a one-time submission; the index
   drifts when the docs change. No re-index trigger.
6. **Retrieval is treated as one channel.** Most agent interactions never hit Context7 or `llms.txt`
   — they rely on training data, shaped by blog posts, Stack Overflow, awesome-lists, stars.
7. **No consumer-facing `AGENTS.md` equivalent shipped with the library.** AGENT-1 is
   contributors-only.
8. **The MCP server is the highest-maintenance item and the review doesn't say so.** Gate it hard on
   PARSE-1 and TYPE-1; ship it as a separate repo.
9. **No `--check` CLI.** No standalone way to validate a SQL template's named parameters against an
   argument set from the command line — the cheapest, most portable verification primitive, reusing
   the real tokeniser from PARSE-1.

### Recommended sequencing for this cluster

1. **Now (Wave 0):** AGENT-1, STRAT-3.
2. **After Wave 0 + Wave 1 correctness (PARSE-1, DIAG-1, TYPE-2, TYPE-6):** AGENT-6 (with the CI
   compile step), then AGENT-2, then AGENT-3.
3. **In parallel:** AGENT-4 and AGENT-7.
4. **Last (Wave 3):** AGENT-5, gated on PARSE-1 + TYPE-1, ideally a separate repo.

Add to the review's list, in Wave 1 alongside AGENT-2: **a single machine-readable source of truth**
(signature index + compiled snippets) that AGENT-2, AGENT-4, AGENT-6 and the docs all generate from.

See `kapper-v3-retrieval-chain.md` for the concrete breakdown.

---

## Breaks for review

Every breaking change, batched into a single v3 migration, each with justification and migration note.

| # | Break | Justification | Migration note | Codemod |
|---|---|---|---|---|
| B1 | **PARSE-1:** drop `-` from valid token characters; define literal `?` as `??` | removes a class of user error (silent wrong SQL / index desync) | Rename hyphenated params (`:my-param` → `:myParam`). Literal `?` (Postgres `jsonb` existence) must be written `??`. | Yes — regex rename `:([A-Za-z0-9_]+)-([A-Za-z0-9_-]*)` → camelCase |
| B2 | **PARSE-1:** `@` no longer unconditionally a token start (MSSQL `SET @x = 1` currently misparses) | removes a class of user error | If you used `@param`, switch to `:param`. MSSQL variable assignment now passes through. | Partial |
| B3 | **API-1:** `withTransaction` becomes generic in `T`; receiver styles unified | removes a class of user error (var-outside-block); unblocks agent consumption | Source-compatible for existing callers; binary break only. | No |
| B4 | **API-3:** nested `withTransaction` no longer commits the outer transaction | removes a class of user error (silent partial commit) | None for correct code; behaviour change for code relying on the bug. | No |
| B5 | **PARSE-3:** `IN (:ids)` expands a collection into placeholders | removes a class of user error (invalid SQL on non-array DBs); unblocks agent consumption | Additive for the broken case; changes behaviour where a collection was previously bound as a single array param. | No |
| B6 | **TYPE-3:** numeric coercion becomes table-driven; narrowing throws | removes a class of user error (asymmetric coercion) | Previously-throwing conversions now work (additive). Narrowing that loses data now throws a **conversion** exception (the review says `KapperParseException`, which is semantically wrong). | No |
| B7 | **PKG-5:** `MapperRegistry` scoped to the `Kapper` instance; `unregister`/`clear` added | removes a class of user error (order-dependent tests, cross-library collisions); deletes a maintenance sink | `Kapper.mapperRegistry` kept as a deprecated global default; new code uses `Kapper.instance.mapperRegistry` or `Kapper.createInstance().mapperRegistry`. | No |
| B8 | **PKG-6:** `explicitApi()` + binary-compat validator; `internal` marked `@KapperInternalApi` | deletes a maintenance sink (accidental public API); unblocks agent consumption (stable surface) | `net.samyn.kapper.internal` becomes opt-in; the coroutines module's imports move to a supported package. | No |
| B9 | **Review gap:** replace `ArgMapper<T> = Pair<String, (T) -> Any?>` with a named type | removes a class of user error (non-obvious `Pair`); unblocks agent consumption | Mechanical; keep a deprecated typealias for one release. | Yes |

No cosmetic renames or taste-driven churn. Notably, renaming the overloaded
`query`/`querySingle`/`executeReturning` families is **not** proposed as a break: its justification
("greppability") is weaker than the four allowed justifications. It is listed as a gap for a decision.

---

## Review gaps

Findings the review misses, judged against the agentic checklist.

1. **The docs assert the opposite of the code.** `docs/guide/mapping.md` says *"Enums are
   automatically mapped from string values"* and shows a working enum example; `docs/guide/queries.md`
   shows `query<String>` and `querySingle<Int>` scalar queries. Neither works (TYPE-2, TYPE-6).
   Worse, `mapping.md`'s custom-mapper example — `Kapper.mapperRegistry.registerIfAbsent<Money> { rs, index -> ... }`
   — does not compile: `registerIfAbsent` takes a `Mapper<T>`, and the lambda's second parameter is
   `Map<String, Field>`, not an index. An agent reading these docs writes code that fails.
2. **No stable doc anchors / deep-linkable errors.** DIAG-1/2/3 improve messages but the review never
   requires them to link to a doc anchor.
3. **No machine-readable API reference (signatures, not prose).** AGENT-2 adds `llms-full.txt`, but
   the API reference is Dokka-generated prose. PKG-6's `.api` dump is for CI, not consumption.
4. **Examples are not compiled or verified in CI.** The docs snippets are not extracted or compiled,
   which is *why* gap #1 exists.
5. **No golden-file harness as executable spec.** Appendix A should become a checked-in golden file.
6. **No `--check`/dry-run for codegen-like paths.** STRAT-2's KSP processor and a standalone template
   validator have no `--check` mode.
7. **Overloaded names hurt greppability.** `query` has 4 overloads, `querySingle` 4,
   `executeReturning` 6. The philosophy itself says "Minimal method overloading".
8. **No `init`/scaffolding path.** AGENT-7 adds a test module but no project template or Gradle init.
9. **`@` token start misfires on MSSQL variables.** Folded into B2.
10. **`querySingle` KDoc is wrong.** `KapperKotlinQuerySingleFun.kt` says `@throws IllegalStateException`,
    but `KapperImpl.querySingle` throws `KapperResultException`.
11. **`ArgMapper` as a `Pair` is non-obvious.** See B9.
12. **The review misses that `docs/guide/index.md` says "It does not generate code"** — which directly
    contradicts its own STRAT-1/STRAT-2 recommendations.

---

## Requires philosophy change

These conflict with a **stated** principle. Kept out of the waves as recommendations.

1. **STRAT-1 (KSP-generated mappers).** Conflicts with `docs/guide/index.md`: *"It does _not_ generate
   code"*. Also sits against "Minimal Abstraction" and "no magic, no hidden behaviour". The review
   proposes it as the strategic bet **without flagging the conflict**, and PKG-1's *preferred* fix
   ("make it true via STRAT-1") inherits it. This is the single most important tension in the
   document: the agentic checklist wants "no runtime reflection surprises" and "if it can fail at
   runtime, prefer a compile error", but the philosophy forbids code generation. You cannot have both
   without changing the philosophy.
2. **STRAT-2 (compile-time SQL validation).** Same codegen/annotation-processing conflict.
3. **TYPE-7 (`Map<String, Any?>` row mapping).** Tension, not a hard conflict: opt-in and additive,
   but the opposite of "explicit types over inference" and of "Kotlin-First Design".
4. **API-10 (`Row` abstraction).** Tension with "Minimal Abstraction" and "Extension functions over
   new classes". Thin and optional, so judged compatible, but it is a new abstraction.
5. **PARSE-2 (template cache).** Tension with "No caching layers that can become stale". Judged
   compatible because a parsed template is a pure function of the template string and cannot go
   stale — but the wording is broad enough that you may want to amend it.
6. **API-5 (multi-mapping).** Tension with the "Another API to learn" objection raised under "Why Not
   a Query Builder?". Judged compatible because it is explicit and SQL-first.

---

## Review-quality notes

Where the review is vague or wrong.

1. **TYPE-3's stated trace is factually wrong against the current source.** The review says
   `convertLong` "has branches for `java.lang.Long`, `Float` and `BigDecimal` but **not** `Integer`,
   so it throws." The current `Converters.kt` (lines 212–233) has `is Int -> value.toLong()` **and**
   `is java.lang.Integer -> value.toLong()`. So `Long` from `INTEGER` does **not** throw. The review
   is dated against commit `c77a3c0`; HEAD is `db83912`, but the `Converters.kt` history shows no
   commit that would have added the `Integer` branch, so the trace is simply incorrect, not stale.
   **The real coercion gaps are different:** `convertInt` has no `Double`/`Short`/`Byte` branches (so
   a `DOUBLE` column into an `Int` property throws), and `convertFloat` has no `Integer`/`Long`/`Short`
   branches. The redundant `is java.lang.Float`/`is Float` duplication the review flags **is** real.
   Write the failing test first — but test the actual gaps, not the claimed one.
2. **TYPE-3's acceptance criteria use the wrong exception.** It says narrowing "throws
   `KapperParseException`". A numeric narrowing failure is a conversion error, not a parse error.
3. **The sequencing contradicts itself.** Section 13 puts PERF-3/PERF-4 in Milestone 4, then says
   "or skip straight to STRAT-1", and STRAT-1 "supersedes PERF-3 and PERF-4". Pick one.
4. **PKG-1's preferred fix conflicts with philosophy.** The review presents three options and
   recommends the one that requires a philosophy change.
5. **API-1 "unify the receiver style" doesn't say which style.** The `Connection` variant uses
   `Connection.() -> Unit`; the `DataSource` variant uses `(Connection) -> Unit`. State the target.
6. **CORO-1 "keep the existing statement-cancellation behaviour" is hand-wavy.** Moving execution
   inside the `flow { }` builder changes *when* the statement is created, which changes the
   cancellation window.
7. **AGENT-5's competitive claim is self-flagged as unverified.** Treat it as a hypothesis.
8. **Appendix B is honest about this** — TYPE-3, TYPE-2 and the duplicate-branch claim are all "from
   code reading, not executed". TYPE-2 (real) and the duplicate branches (real) were verified;
   TYPE-3's specific trace is not.

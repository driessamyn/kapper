# Handover: Kapper v3 — Wave 0

> This file is a self-contained brief for an agent session that did **not** participate in the
> planning. Read it top to bottom before touching anything.

---

## 1. What you are working on

**Kapper** is a lightweight, Dapper-inspired ORM for Kotlin/JVM. It extends `java.sql.Connection`
with `query` / `querySingle` / `execute` / `executeAll` / `executeReturning` / `withTransaction`
extension functions, plus a separate `kapper-coroutines` module. It maps result sets to Kotlin data
classes and Java records.

Read these first, in order:

1. `README.md` — what Kapper is.
2. `docs/guide/philosophy.md` — the design principles. **These are a hard constraint, not a preference.**
3. `kapper-v3-plan.md` — the full v3 plan. Your work is **Wave 0**.
4. `kapper-v3-retrieval-chain.md` — the discoverability breakdown (only relevant if you take AGENT-6).
5. `docs/scripts/SPIKE-api-manifest.md` — spike findings (only relevant if you take AGENT-6).

Do **not** re-plan. The plan is settled. If you believe an item is wrong, implement the rest and flag
it in your summary.

**Branch:** you are on `v3-planning`, the v3 planning branch. All work for this handover is reported
back as pull requests **targeting `v3-planning`** (see §10). Do not target or merge to `main`.

## 2. The philosophy (hard constraint)

Verbatim from `docs/guide/philosophy.md`:

> 1. **SQL is the Best Query Language** — SQL has evolved for decades to be the optimal language for database operations.
> 2. **Minimal Abstraction** — Kapper provides just enough abstraction to make JDBC comfortable, without hiding the underlying mechanisms.
> 3. **Transparency** — What you write is what gets executed. No magic, no hidden behaviour.
> 4. **Performance by Design** — Kapper is designed to be as fast as hand-written JDBC while providing convenience.
> 5. **Kotlin-First Design** — Kapper leverages Kotlin's strengths while remaining Java-compatible.

And from `docs/guide/index.md`:

> It does _not_ generate code, it does not introduce another layer of abstraction, it is never intrusive.

**If a change you are about to make conflicts with a stated principle, stop and flag it rather than
implementing it.** Wave 0 contains no such conflicts; this is a guard against drift.

## 3. Your task

Implement **Wave 0** of `kapper-v3-plan.md`: the "Free" wave. Every item is **no API change** and
**no breaking change** — behaviour and messages only. Each is roughly half a day or less.

Wave 0 is split into **three workstreams with disjoint write scopes**, so they can be run in parallel
by three agents. If you are one agent, do them in order WS1 → WS2 → WS3.

**You may — and should — delegate the independent workstreams to sub-agents.** WS1, WS2 and WS3 have
disjoint write scopes, so they are safe to run concurrently. Give each sub-agent the relevant section
of this brief plus the context from §1–§2; do not ask a sub-agent to re-plan. Keep the integration and
the final verification for yourself.

| Workstream | Items | Write scope |
|---|---|---|
| **WS1 — Core** | DIAG-1, DIAG-2, DIAG-3, PARSE-2, PERF-2, PERF-5, PERF-6, API-9 | `core/src/main/kotlin/net/samyn/kapper/**` |
| **WS2 — Coroutines** | CORO-1, CORO-2 | `coroutines/src/main/kotlin/net/samyn/kapper/coroutines/**` |
| **WS3 — Packaging & docs** | PKG-1, PKG-2, PKG-3, DOCS-2, DOCS-4, AGENT-1, STRAT-3 | `README.md`, `docs/**`, `buildSrc/**` |

WS1 and WS2 also add tests under their respective `src/test` trees. WS3 touches no Kotlin.

---

## 4. WS1 — Core

All paths are relative to `core/src/main/kotlin/net/samyn/kapper/`.

### DIAG-1 — self-describing mapping failures
- **Problem:** `Kapper.query` catches and rethrows as `KapperMappingException("Error creating mapper for $clazz", e)`. `KotlinDataClassMapper` computes a `missing` list and discards the rest of the context. The reader learns nothing actionable.
- **Change:** Emit a message naming the target class, the columns actually returned with their JDBC types, the constructor parameters expected, the specific mismatch, and — where there is an obvious remedy — a hint (e.g. "alias the columns (`SELECT full_name AS name`), or register a custom mapper").
- **Files:** `Kapper.kt`, `internal/automapper/KotlinDataClassMapper.kt`, `internal/automapper/RecordMapper.kt`.
- **Acceptance:** every `KapperMappingException` names the target class, the columns returned with JDBC types, the constructor parameters expected, and the specific mismatch. Tests assert on **message content**, not just exception type.

### DIAG-2 — self-describing conversion failures
- **Problem:** messages are of the form `Cannot auto-convert from class java.lang.Integer to long`, with no indication of which column caused it.
- **Change:** add the column label, the column index, the driver type name, the flavour, and a pointer to converter registration.
- **Files:** `internal/AutoConverter.kt`, `internal/Converters.kt`.
- **Acceptance:** a conversion failure names the column and the two types; tests assert on message content.

### DIAG-3 — parse failures point at the character
- **Problem:** `KapperParseException("'$c' is not a valid character part of the template token.")` gives no position.
- **Change:** include the template with a caret under the offending index.
- **Files:** `internal/Query.kt`.
- **Acceptance:** the message contains the template and a caret line; a test asserts the caret is under the right column.

### PARSE-2 — cache parsed templates
- **Problem:** `KapperImpl`'s default `queryFactory = { Query(it) }` re-scans the template character by character on every `query`/`execute`/`executeAll`/`executeReturning` call.
- **Change:** a **bounded** cache keyed on the template string. Hand-write the bound (`ConcurrentHashMap` with a size cap); do **not** add Caffeine or any dependency. The bound must be configurable, and a pathological caller generating unique SQL must not be able to exhaust the heap.
- **Files:** `internal/KapperImpl.kt`, `internal/Query.kt`.
- **Acceptance:** repeated execution of the same template parses once (assert with a counting `queryParser` test double); the cache is bounded and the bound is configurable.
- **Note:** this interacts with PARSE-3 (Wave 2), which needs the *template* cached and the final SQL derived per call. Design the cache so PARSE-3 can build on it, but do not implement PARSE-3.

### PERF-2 — resolve `DbFlavour` once
- **Problem:** `getDbFlavour()` is called **twice per query** — once in `executeQuery` for parameter binding, once for `extractFields`. Each call reads `connection.metaData.databaseProductName` and runs a chain of case-insensitive `contains` checks.
- **Change:** resolve once per call and thread it through. Cache the mapping on the **product-name string** (or driver class), **not** on the `Connection` instance — caching per `Connection` is unsafe with pooling and proxy wrappers.
- **Files:** `internal/DbFlavourFunc.kt`, `internal/KapperImpl.kt`, `internal/QueryExecution.kt`.
- **Acceptance:** at most one metadata read per query, asserted with a mock connection that counts `getMetaData` calls.

### PERF-5 — hoist reflection out of `RecordMapper`'s row loop
- **Problem:** `createInstance` calls `clazz.getDeclaredConstructor(*recordComponents.map { it.type }.toTypedArray())` on **every row**, rebuilding the parameter type array each time.
- **Change:** resolve the canonical constructor in `init`, call `setAccessible` once, and cache it. Consider a `MethodHandle`.
- **Files:** `internal/automapper/RecordMapper.kt`.
- **Acceptance:** the constructor is resolved once per mapper instance; existing record-mapping tests still pass.

### PERF-6 — replace the `JDBCType` linear scan
- **Problem:** `Int.jdbcType()` does `JDBCType.entries.firstOrNull { it.vendorTypeNumber == this }` per column per query.
- **Change:** a static `Map<Int, JDBCType>` built once.
- **Files:** `internal/Metadata.kt`.
- **Acceptance:** behaviour unchanged; a test covers the known vendor type numbers.

### API-9 — short-circuit `querySingle`
- **Problem:** `KapperImpl.querySingle` calls `query` (materialising the whole result set) and only then checks `results.size > 1`.
- **Change:** stop at two rows. Keep throwing `KapperResultException` when more than one row is present.
- **Files:** `internal/KapperImpl.kt`.
- **Acceptance:** a query returning many rows does not materialise them all; the "more than one result" behaviour is unchanged.
- **Note:** do **not** add `queryFirst` here — that is a new API and belongs to a later wave.

---

## 5. WS2 — Coroutines

All paths are relative to `coroutines/src/main/kotlin/net/samyn/kapper/coroutines/`.

### CORO-1 — make `queryAsFlow` cold
- **Problem:** `KapperKotlinFlowQueryFun.kt` executes the query **eagerly**, outside the `flow { }` builder, and calls `extractFields` eagerly too. Consequences: the flow is not cold; the blocking JDBC call runs on the caller's thread; and **if the flow is never collected, the `ResultSet` and `Statement` leak**, because `rs.close()` only runs in the builder's `finally`.
- **Change:** move execution inside the `flow { }` builder, use `.use { }` for the result set, and keep the existing statement-cancellation behaviour.
- **Files:** `KapperKotlinFlowQueryFun.kt`.
- **Acceptance:** a test that creates a flow and never collects it leaves no open statement or result set; a test that cancels mid-collection closes both.

### CORO-2 — acquire the connection inside `withContext`
- **Problem:** `DatasourceWithConnection.kt` does `this.connection.use { withContext(context) { block(connection) } }`. `getConnection()` blocks (potentially for the full pool timeout) and `close()` blocks, both on the calling thread — which could be `Dispatchers.Default` or a Netty event loop.
- **Change:** `withContext(context) { this@withConnection.connection.use { block(it) } }`.
- **Files:** `DatasourceWithConnection.kt`.
- **Acceptance:** a test asserting connection acquisition happens on the specified dispatcher. Document that the connection must not escape the block.

---

## 6. WS3 — Packaging & docs

### PKG-1 — fix the "Zero Dependencies" claim
- **Problem:** `README.md` claims "zero external dependencies" and `docs/index.md` has a "Zero Dependencies" feature card. The published POM declares `kotlin-stdlib` (compile), `kotlin-reflect` (runtime), `slf4j-api` (runtime). `kotlin-reflect` is not small.
- **Change:** restate honestly (e.g. "two small runtime dependencies, no framework"). Do **not** attempt to make the claim true — that requires STRAT-1, which conflicts with the philosophy.
- **Files:** `README.md`, `docs/index.md`.

### PKG-2 — fix the duplicated POM metadata
- **Problem:** the published POM repeats the licence block three times and the developer block three times — almost certainly the `publishing { }` publication and the `deployer { projectInfo { } }` block overlapping.
- **Change:** de-duplicate.
- **Files:** `buildSrc/src/main/kotlin/kapper.library-publish.gradle.kts`.

### PKG-3 — add `Automatic-Module-Name`
- **Problem:** the published jar manifest contains only `Implementation-Title` and `Implementation-Version`; JPMS consumers get a module name derived from the filename.
- **Change:** add `Automatic-Module-Name: net.samyn.kapper` and the equivalent for the coroutines module.
- **Files:** `buildSrc/src/main/kotlin/kapper.library-conventions.gradle.kts`.

### DOCS-2 — fix the docs link inconsistency
- **Problem:** `README.md` uses `kapper.samyn.net` as link text while the URL points at `driessamyn.github.io/kapper`.
- **Change:** pick one. (A custom domain is a separate decision — see the retrieval-chain breakdown; do not set one up here.)
- **Files:** `README.md`.

### DOCS-4 — document the threading and resource-ownership contract
- **Problem:** who closes the connection, who closes the result set, what is safe to share across threads, and what happens on cancellation are all implicit. The CORO bugs existed partly because this was never written down.
- **Change:** write it down.
- **Files:** `docs/guide/coroutines.md`, `docs/guide/basic-usage.md`.
- **Note:** follow `docs/style-guide.md` (British English, active voice, `Next Steps` section).

### AGENT-1 — add `AGENTS.md` at the repository root
- **Problem:** no such file exists. The convention is read natively by many agent tools.
- **Change:** write it **by hand**, project-specific only. Include: the Gradle commands, the `integrationTest` source set and its Testcontainers requirement, the ktlint-on-compile behaviour, the Kover 90% gate, the conventional-commit release pattern, and the fact that `net.samyn.kapper.internal` is not a stable API. Do **not** write generic advice an agent already knows — research shows LLM-generated `AGENTS.md` files reduce task success.
- **Files:** `AGENTS.md` (new).
- **Note:** this file is for agents working **on** Kapper (contributors), not for consumers.

### STRAT-3 — positioning
- **Problem:** the current pitch is "lightweight, Dapper-inspired, fast". "Fast" is table stakes and "lightweight" does not currently hold (see PKG-1).
- **Change:** adopt the available position: *the JVM data-access library designed for the era when most SQL is written by a model. SQL-first because models already know SQL. Errors that tell you how to fix them. Verification before the query runs.* Update the README and docs landing page.
- **Files:** `README.md`, `docs/index.md`.
- **Note:** this is judgement, not fact. Keep it honest — do not claim features that do not exist yet.

---

## 7. Constraints

- **No API changes, no breaking changes.** Wave 0 is invisible to callers except for better messages and fixed bugs. If you find yourself changing a signature, stop — that is a later wave.
- **Philosophy is a hard constraint.** See §2.
- **Tests.** Every change needs a test. The Kover gate is **90% instruction coverage** and `./gradlew check` enforces it. Unit tests live in `src/test`; integration tests in `integrationTest` (Docker/Testcontainers).
- **ktlint runs on compile.** `ktlintFormat` is wired to `KotlinCompile`, so formatting is automatic. Do not fight it.
- **Conventional commits.** CI enforces the format via `webiny/action-conventional-commits`.
- **Dependency lock files.** If you add a dependency, run `./gradlew resolveAndLockAll --write-locks` and commit the changed `*.lockfile`s. CI fails otherwise.
- **Do not touch the public surface of `net.samyn.kapper.internal`** beyond what a task requires. PKG-6 (a later wave) will lock it down.
- **Do not commit** unless the branch owner asks.

## 8. How to verify

```bash
./gradlew :kapper:test                 # core unit tests
./gradlew :kapper-coroutines:test      # coroutine unit tests
./gradlew check                        # ktlint + kover (90%) + tests
./gradlew integrationTest -Ddb=SQLITE  # integration tests; most DBs need Docker
```

Integration tests need Docker/Testcontainers, which may not be available. If they are not, say so in
your summary rather than claiming they passed.

## 9. Out of scope

Everything in Wave 1 and later: enum support (TYPE-2), scalar mapping (TYPE-6), the converter
registry (TYPE-1), the tokeniser rewrite (PARSE-1), `IN` expansion (PARSE-3), `withTransaction`
returning a value (API-1), nested transactions (API-3), the `Row` abstraction (API-10), `llms.txt`
(AGENT-2), the skill (AGENT-4), `kapper-test` (AGENT-7), and everything in Waves 2–3. Do not start
them, and do not "prepare" for them by changing signatures.

## 10. Deliverable

**Report back with pull requests targeting the `v3-planning` branch — not `main`.** This branch is the
v3 planning branch and is where progress against the overall plan is tracked. Do not merge to `main`.

- One PR per workstream (WS1, WS2, WS3) is preferred, so progress is visible against the plan; a
  single PR is acceptable if you did the work serially.
- Each PR must have tests and pass `./gradlew check`.
- In the PR description (and your summary), state:
  - which items you completed, and which you did not (and why);
  - the exact commands you ran and their results;
  - anything you found that contradicts the plan.

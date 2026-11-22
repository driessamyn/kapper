# Handover: Kapper v3 — Wave 1

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
3. `kapper-v3-plan.md` — the full v3 plan. Your work is **Wave 1**.
4. `kapper-v3-retrieval-chain.md` — the discoverability breakdown (only relevant for WS4).
5. `docs/scripts/SPIKE-api-manifest.md` — spike findings (only relevant for AGENT-2).

Do **not** re-plan. The plan is settled. If you believe an item is wrong, implement the rest and flag
it in your summary.

**Where these files live.** This brief and the planning docs it references (`kapper-v3-plan.md`,
`kapper-v3-retrieval-chain.md`, `docs/scripts/SPIKE-api-manifest.md`) live on the `v3-planning`
branch. If you are working in a worktree branched off `main`, read them from the `v3-planning`
worktree — the absolute path is in your task prompt. `README.md` and `docs/guide/philosophy.md` exist
in every worktree.

**Branch.** Wave 1 is **additive and non-breaking**, so it ships as **2.2.0** and PRs target **`main`**
(the 2.x integration branch). This supersedes the Wave 0 brief's "target `v3-planning`" instruction,
which was changed after Wave 0. Reference the plan item IDs (e.g. `TYPE-2`) in each PR description so
progress is visible against `kapper-v3-plan.md`.

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
implementing it.** Two Wave 1 items sit close to the line and are called out where they appear:
**TYPE-7** (`Map<String, Any?>` rows — against "explicit types") and **API-10** (`Row` — a new
abstraction). Both are judged compatible because they are opt-in and additive, but keep them thin.

## 3. Your task

Implement **Wave 1** of `kapper-v3-plan.md`: the "Quick wins". Every item is **additive or
non-breaking**. The wave ships as **2.2.0**.

Wave 1 is split into **six workstreams**. WS3, WS4, WS5 and WS6 have disjoint write scopes and can run
in parallel. **WS1 and WS2 both touch `core/`** and must be coordinated (see §12).

**You may — and should — delegate the independent workstreams to sub-agents.** Give each sub-agent the
relevant section of this brief plus the context from §1–§2; do not ask a sub-agent to re-plan. Keep the
integration and the final verification for yourself.

| Workstream | Items | Write scope |
|---|---|---|
| **WS1 — Core types** | TYPE-1, TYPE-2, TYPE-4, TYPE-5, TYPE-6, TYPE-7, DIAG-4 | `core/src/main/kotlin/net/samyn/kapper/internal/**`, `core/src/main/kotlin/net/samyn/kapper/*.kt` |
| **WS2 — Core API** | API-7, API-8, API-10, PERF-7 | `core/src/main/kotlin/net/samyn/kapper/**` |
| **WS3 — Coroutines** | CORO-3, CORO-4 | `coroutines/src/main/kotlin/net/samyn/kapper/coroutines/**` |
| **WS4 — Docs & retrieval** | DOCS-1, DOCS-3, AGENT-2, AGENT-3 | `docs/**`, `buildSrc/**` |
| **WS5 — Agent tooling** | AGENT-4, AGENT-7 | `.agents/**`, `kapper-test/**`, `settings.gradle.kts` |
| **WS6 — Packaging** | PKG-4 | `buildSrc/**`, `core/src/main/resources/**` |

Each workstream adds tests under its own `src/test` tree.

---

## 4. WS1 — Core types

All paths are relative to `core/src/main/kotlin/net/samyn/kapper/`.

**WS1 is the keystone.** TYPE-1 unblocks TYPE-4; TYPE-2, TYPE-6 and TYPE-7 unblock the deferred
snippets (§11). Do TYPE-1 first.

### TYPE-1 — user-extensible converter registry
- **Problem:** the `converters` map in `internal/Converters.kt` is `private val`. The source comment
  says "we can/should extend this to allow users to register custom converters", but there is no public
  way to do so. Every future type gap becomes the user's problem.
- **Change:** add a public, symmetrical read/write registry (register, look up, and — unlike
  `MapperRegistry` — `unregister`/`clear`). Decide **instance-scoped vs global** deliberately: PKG-5
  (Wave 2) will scope `MapperRegistry` to the `Kapper` instance, so design this to match that direction
  (instance-scoped with a default global), and note the decision in the PR.
- **Files:** `internal/Converters.kt`, `internal/AutoConverter.kt`, `Kapper.kt`.
- **Acceptance:** a user can register a converter for a custom type and have it used by `query`; a test
  proves registration, lookup and removal; the existing built-in conversions are unchanged.
- **Note:** this is the item the MCP server (AGENT-5, Wave 3) depends on. Keep it small and stable.

### TYPE-2 — enum support
- **Problem:** no enum handling exists. A `String` column against an enum target falls through to
  `KapperUnsupportedOperationException`. The docs already claim enums work (`docs/guide/mapping.md`),
  so this is a documented-but-missing feature.
- **Change:** name-based (case-insensitive) read **and** write. On failure, throw a
  `KapperMappingException` naming the column, the value, and the valid constants.
- **Files:** `internal/Converters.kt`, `internal/AutoConverter.kt`, `internal/automapper/**`.
- **Acceptance:** a `VARCHAR` column maps to an enum property and back; an unknown value produces a
  message naming the column, the value and the constants; tests assert on message content.

### TYPE-6 — scalar / single-column mapping
- **Problem:** `createAutoMapper` accepts only `isData`/`isRecord` and throws otherwise, so
  `query<Int>` fails. The docs show `query<String>` and `querySingle<Int>`.
- **Change:** add scalar mapping, plus a `queryScalar` / `querySingleValue` convenience. Fail clearly
  when the result has more than one column.
- **Files:** `internal/automapper/**`, `internal/KapperImpl.kt`, `Kapper.kt`.
- **Acceptance:** `query<Int>`, `query<String>`, `query<UUID>` work; a multi-column result produces a
  clear error; tests cover the scalar types.

### TYPE-7 — `Map<String, Any?>` row mapping
- **Problem:** Dapper's `dynamic` equivalent does not exist. Useful for exploratory and agent-driven
  queries.
- **Change:** opt-in mapping of a row to `Map<String, Any?>` (column label → value). Do not make it the
  default for any existing path.
- **Files:** `internal/automapper/**`, `internal/KapperImpl.kt`.
- **Acceptance:** `query<Map<String, Any?>>` returns one map per row with the correct labels and values.
- **Tension:** the opposite of "explicit types over inference" and of "Kotlin-First Design". Judged
  compatible because it is opt-in and additive. Keep it thin; do not build a query-builder on top.

### TYPE-4 — `JDBCType.OTHER` via the registry
- **Problem:** the `else` branch in `internal/Converters.kt` handles only `uuid`, `binary_float` and
  `binary_double` by type name, then throws. PostgreSQL `json`/`jsonb` and `vector` are common and
  unsupported.
- **Change:** route unknown type names through the **TYPE-1** registry. Ship `json`/`jsonb` → `String`
  and `vector` → `FloatArray` / `List<Float>` as built-ins.
- **Files:** `internal/Converters.kt`.
- **Acceptance:** a `jsonb` column maps to `String`; a `vector` column maps to `FloatArray`; an unknown
  type name produces a message pointing at the registry.
- **Depends on:** TYPE-1.

### TYPE-5 — missing JDK / Kotlin types
- **Problem:** several common types have no conversion: `OffsetDateTime`, `ZonedDateTime`, `OffsetTime`,
  `BigInteger`, `Duration`, `Period`, `Year`, `YearMonth`, `kotlin.time.Instant`, value classes,
  `Optional<T>`.
- **Change:** add the conversions. Fold in the unmerged `kotlin-uuid-support` branch (PR #79, draft).
  Put kotlinx-datetime support in a **separate optional module**, not in `core`.
- **Files:** `internal/Converters.kt`, `internal/AutoConverter.kt`; a new module for kotlinx-datetime.
- **Acceptance:** each type round-trips through a test; the kotlinx-datetime module is optional and does
  not add a dependency to `core`.
- **Note:** if you add a dependency, run `./gradlew resolveAndLockAll --write-locks` and commit the
  changed `*.lockfile`s.

### DIAG-4 — strict mode
- **Problem:** unbound tokens, unmatched columns and silently-defaulted parameters pass silently.
- **Change:** add an **opt-in** strict mode (off by default) that fails on each of these. Wire it through
  the existing `Kapper` configuration; do not change default behaviour.
- **Files:** `internal/KapperImpl.kt`, `internal/Query.kt`, `Kapper.kt`.
- **Acceptance:** with strict mode on, each of the three conditions throws a clear error; with it off,
  behaviour is unchanged.

---

## 5. WS2 — Core API

All paths are relative to `core/src/main/kotlin/net/samyn/kapper/`.

### API-7 — generated-keys API
- **Problem:** `executeReturning` needs `RETURNING`, which MySQL and older MSSQL lack. There is no
  `RETURN_GENERATED_KEYS` path.
- **Change:** add a generated-keys path (`Statement.RETURN_GENERATED_KEYS` + `getGeneratedKeys`).
- **Files:** `internal/QueryExecution.kt`, `internal/KapperImpl.kt`, `Kapper.kt`.
- **Acceptance:** an insert on a DB without `RETURNING` returns the generated key; tests cover both the
  `RETURNING` and the generated-keys paths.

### API-8 — streaming `Sequence` in core
- **Problem:** `query` always materialises the whole result set. There is no streaming variant in
  `core` (only `queryAsFlow` in the coroutines module).
- **Change:** add a `Sequence` variant with **explicit resource management** (the caller owns the
  lifetime; document it). Do not change `query`.
- **Files:** `Kapper.kt`, `KapperKotlinQueryFun.kt`, `internal/QueryExecution.kt`.
- **Acceptance:** a large result set can be consumed lazily; the resource contract is documented and
  tested (including the abandoned-sequence case).

### API-10 — `Row` abstraction
- **Problem:** the custom-mapper signature `(ResultSet, Map<String, Field>) -> T` leaks `Field` and is
  hard to remember. The docs' custom-mapper example does not even compile.
- **Change:** add a thin `Row` wrapper. **Keep the old signature working underneath** — this is
  additive.
- **Files:** `Kapper.kt`, `internal/automapper/**`.
- **Acceptance:** a custom mapper can be written against `Row`; the old signature still compiles and
  works; the docs example is corrected.
- **Tension:** "Minimal Abstraction" / "Extension functions over new classes". Judged compatible
  because it is thin and optional.

### PERF-7 — chunk `executeAll` batches
- **Problem:** `executeAll` builds one cleanup lambda per object and never flushes, so memory grows
  linearly with the input size.
- **Change:** add an optional `batchSize` and use `executeLargeBatch`; flush per chunk.
- **Files:** `internal/KapperImpl.kt`, `internal/QueryExecution.kt`.
- **Acceptance:** a large batch is flushed in chunks; existing `executeAll` behaviour is unchanged when
  no `batchSize` is given.

---

## 6. WS3 — Coroutines

All paths are relative to `coroutines/src/main/kotlin/net/samyn/kapper/coroutines/`.

### CORO-4 — suspending `execute` / `querySingle`
- **Problem:** only `queryAsFlow` and `withConnection` exist. Coroutine users have no suspending
  `execute` or `querySingle`.
- **Change:** add them, following the existing `withConnection` pattern (connection acquired inside the
  dispatcher).
- **Files:** `KapperKotlin*Fun.kt` (new functions).
- **Acceptance:** `execute` and `querySingle` can be called from a coroutine; tests assert the blocking
  work runs on the supplied dispatcher.

### CORO-3 — `flowOn` + threading docs
- **Problem:** there is no `flowOn` anywhere, so blocking JDBC runs on the collector's thread.
- **Change:** apply `flowOn` where appropriate and document the threading model. This is a behaviour
  change — document it.
- **Files:** `KapperKotlinFlowQueryFun.kt`, `docs/guide/coroutines.md`.
- **Acceptance:** the flow runs on the supplied dispatcher; the docs state the threading contract.

---

## 7. WS4 — Docs & retrieval

### DOCS-1 — support matrix
- **Problem:** the docs say "probably many others" instead of stating what is tested.
- **Change:** replace it with a **tiered** matrix (tier 1 = CI-tested, tier 2 = community-reported,
  tier 3 = untested). Merge the unmerged compatibility branches where practical:
  `feature/add-clickhouse-compat`, `feature/add-tidb-compat`, `feature/add-yugabytedb-compat`,
  `feature/add-starrocks-compat`, `feature/add-firebird-compat`, `feature/add-mariadb-compat`.
- **Files:** `docs/guide/**`, `.github/workflows/**`.
- **Acceptance:** the matrix states each database's tier and how it is tested; no "probably" claims.
- **Note:** the compat branches are old and may not merge cleanly. If a branch does not merge, record
  the database as tier 3 rather than blocking the item.

### DOCS-3 — type support matrix
- **Problem:** there is no single statement of which SQL types map to which Kotlin types.
- **Change:** generate the matrix **from the tests**, so it cannot drift. This is the same
  single-source-of-truth principle as the snippets.
- **Files:** `core/src/test/**`, `docs/guide/**`.
- **Acceptance:** the matrix is generated by a test/build task and CI fails on drift.

### AGENT-2 — `llms.txt` / `llms-full.txt`
- **Problem:** there is no agent-consumable form of the docs.
- **Change:** generate `docs/public/llms-full.txt` (complete API surface + one worked example per
  feature) and `docs/public/llms.txt` (the index), plus a `.md` variant per page. Add a validator for
  the llmstxt.org layout. Make `docs/changelog.md` static (it currently renders a runtime Vue
  component, invisible to agents).
- **Files:** `buildSrc/src/main/kotlin/kapper.apimanifest.gradle.kts` (new),
  `docs/scripts/generate-llms.mjs` (new), `docs/scripts/validate-llms.mjs` (new),
  `docs/public/**`, `docs/changelog.md`, `.github/workflows/docs.yml`.
- **Acceptance:** both files are served from the docs site and regenerated on every docs build;
  `llms-full.txt` is within a size budget (target < 100k tokens) and contains every public API; the
  validator passes; CI fails on drift.
- **Depends on:** AGENT-6 (done), DOCS-1/DOCS-3 (matrices), and the Wave 1 correctness items
  (TYPE-2, TYPE-6) so the examples are correct.
- **Important:** the API manifest must be generated from **Dokka JSON** (the `generateApiDocs` task
  already produces it). PKG-6's binary-compatibility-validator `.api` dump is **Wave 2** and does not
  exist yet.
- **Important:** `docs/.vitepress/config.ts` has `base: '/kapper/'`, so `llms.txt` lands at
  `driessamyn.github.io/kapper/llms.txt`, not a domain root. The convention expects a root path. This
  is a prerequisite for AGENT-3 — see §12.

### AGENT-3 — Context7 submission
- **Problem:** no presence in the retrieval path agents actually use.
- **Change:** submit to Context7, add a re-index trigger on release, a verification check, and a
  success metric.
- **Files:** `.github/workflows/**`, `docs/scripts/verify-context7.mjs` (new).
- **Acceptance:** searching Context7 for "kapper" resolves to this project; re-index happens on
  release; the verification check runs in CI.
- **Depends on:** AGENT-2, **and PARSE-1 (Wave 2)** — see §12. **Build the plumbing, but do not submit
  until PARSE-1 lands.**

---

## 8. WS5 — Agent tooling

### AGENT-4 — distributable skill
- **Problem:** there is no packaged skill for agents working *with* Kapper.
- **Change:** write a `SKILL.md` covering the API, the binding rules, the type table, the transaction
  patterns, and the top-10 mistakes. Generate the API section from the same source as AGENT-2 where
  possible.
- **Files:** `.agents/skills/kapper/SKILL.md` (new).
- **Acceptance:** the skill is self-contained and its examples match the compiled snippets.

### AGENT-7 — `kapper-test` module
- **Problem:** an agent cannot close the loop quickly — there is no in-memory harness.
- **Change:** add a `kapper-test` module with an in-memory harness (SQLite/DuckDB) so an agent can run
  a query and see the result in seconds.
- **Files:** `kapper-test/**` (new), `settings.gradle.kts`.
- **Acceptance:** the module provides a ready-to-use in-memory `DataSource`; a test in the module proves
  a query round-trips; the module is published (or clearly marked as test-only).
- **Note:** this is a new module to maintain — keep it minimal.

---

## 9. WS6 — Packaging

### PKG-4 — GraalVM reachability metadata
- **Problem:** the reflection-based automapper breaks native image.
- **Change:** ship `reflect-config.json` as a stopgap (STRAT-1 would remove the need, but conflicts with
  the philosophy).
- **Files:** `core/src/main/resources/META-INF/native-image/**`, `buildSrc/**`.
- **Acceptance:** a native-image build of a simple query succeeds; the metadata is documented as a
  stopgap.

---

## 10. Follow-up — re-enable the deferred snippets

AGENT-6 (done, PR #407) shipped 13 compiled snippets and deferred 9 with `"status": "todo"` in
`docs/snippets.json`. Once their dependencies land, remove the `todo` status and add the source region:

| Snippet | Unblocked by |
|---|---|
| `select-scalar` | TYPE-6 |
| `enum-property` | TYPE-2 |
| `custom-mapper` | API-10 |
| `jsonb` | TYPE-4 / API-10 |
| `streaming` | API-8 |
| `testing` | AGENT-7 |
| `transaction-with-value` | API-1 (Wave 2) |
| `nested-transaction` | API-3 (Wave 2) |
| `in-list` | PARSE-3 (Wave 2) |

The last three cannot be done in Wave 1 — leave them `todo`. For the rest, add the snippet, regenerate
(`node docs/scripts/generate-snippets.mjs`), and commit the generated output.

---

## 11. Dependencies and gates

- **TYPE-1 → TYPE-4.** Do TYPE-1 first.
- **TYPE-2 / TYPE-6 / TYPE-7 → the deferred snippets** (§10) and **AGENT-2** (the examples must be
  correct).
- **AGENT-6 (done) → AGENT-2.**
- **DOCS-1 / DOCS-3 → AGENT-2** (the matrices feed `llms-full.txt`).
- **AGENT-2 → AGENT-3.**
- **PARSE-1 (Wave 2) → AGENT-3.** PARSE-1 is a **breaking change** and cannot ship in a 2.x. The rule
  (from `kapper-v3-plan.md`, "The gate"): **build** the retrieval artefacts in Wave 1, but do not
  **promote** them (Context7 submission, traffic-driving) until PARSE-1 lands. Build AGENT-3's plumbing
  and leave the submission for Wave 2.
- **Root path → AGENT-3.** Because of `base: '/kapper/'`, `llms.txt` is not at a domain root. Either
  adopt a custom domain or publish a root-level redirect before submitting to Context7.

## 12. Constraints

- **No breaking changes.** Wave 1 is additive. If you find yourself changing an existing signature,
  stop — that is Wave 2. The one exception is CORO-3 (`flowOn`), which is a documented behaviour change.
- **WS1 and WS2 both touch `core/`.** They are not safe to run concurrently without coordination. Run
  WS1 first (it is the keystone), then WS2; or split the files explicitly and merge carefully.
- **Philosophy is a hard constraint.** See §2.
- **Tests.** Every change needs a test. The Kover gate is **90% instruction coverage** and
  `./gradlew check` enforces it. Unit tests live in `src/test`; integration tests in `integrationTest`
  (Docker/Testcontainers).
- **ktlint runs on compile.** `ktlintFormat` is wired to `KotlinCompile`, so formatting is automatic.
- **Conventional commits.** CI enforces the format via `webiny/action-conventional-commits`. Use `feat:`
  for the new API surface — that is what moves the version to `2.2.0`.
- **Dependency lock files.** If you add a dependency, run `./gradlew resolveAndLockAll --write-locks`
  and commit the changed `*.lockfile`s. CI fails otherwise.
- **Do not touch the public surface of `net.samyn.kapper.internal`** beyond what a task requires.
  PKG-6 (Wave 2) will lock it down.
- **Do not commit** unless the branch owner asks.

## 13. How to verify

```bash
./gradlew :kapper:test                 # core unit tests
./gradlew :kapper-coroutines:test      # coroutine unit tests
./gradlew check                        # ktlint + kover (90%) + tests
./gradlew integrationTest -Ddb=SQLITE  # integration tests; most DBs need Docker
node docs/scripts/generate-snippets.mjs # regenerate snippets (WS4 / §10)
```

Integration tests need Docker/Testcontainers, which may not be available. If they are not, say so in
your summary rather than claiming they passed.

## 14. Out of scope

Everything in Wave 2 and later: the tokeniser rewrite (PARSE-1), `IN` expansion (PARSE-3),
`withTransaction` returning a value (API-1), nested transactions (API-3), the suspending transaction
API (API-4), isolation/read-only/savepoint (API-2), scoping `MapperRegistry` (PKG-5), locking down the
public API (PKG-6), multi-mapping (API-5), multiple result sets (API-6), KSP-generated mappers
(STRAT-1), compile-time SQL validation (STRAT-2), and the MCP server (AGENT-5). Do not start them, and
do not "prepare" for them by changing signatures.

## 15. Deliverable

**Report back with pull requests targeting `main`** (the 2.x integration branch). Wave 1 ships as 2.2.0.

- One PR per workstream is preferred, so progress is visible against the plan; a single PR is
  acceptable if you did the work serially.
- Each PR must have tests and pass `./gradlew check`.
- In the PR description (and your summary), state:
  - which plan item IDs you completed, and which you did not (and why);
  - the exact commands you ran and their results;
  - anything you found that contradicts the plan.

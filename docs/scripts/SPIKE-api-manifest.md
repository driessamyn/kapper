# Spike: API manifest source (step 2.1)

**Question:** can the binary-compatibility-validator `.api` dump be the source for the
consumer-facing API manifest (`api-manifest.json`), or do we need Dokka JSON?

**Answer: no — use Dokka JSON for the manifest, and keep the `.api` dump for the CI gate only.**

**Artefacts produced by this spike**

- `docs/scripts/fixtures/kapper.api` — a representative `.api` dump, hand-authored from the source at
  HEAD `db83912` (no real dump exists yet; PKG-6 is not done).
- `docs/scripts/parse-api-dump.mjs` — a working parser for the `.api` format.
- `docs/scripts/generate-snippets.mjs`, `docs/snippets.json`, `docs-snippets/` — the snippet pipeline
  (see the second half of this document).

**Caveat:** the fixture is hand-authored, not captured from a real Gradle run (the sandbox has no
network, so the plugin cannot be added and run). The parser and the lossiness findings are real; the
fixture is representative, not authoritative. Confirm against a real dump before committing.

---

## 1. The `.api` parser works

`node docs/scripts/parse-api-dump.mjs docs/scripts/fixtures/kapper.api` parses all 13 classes and
emits JSON. Two bugs were found and fixed during the spike, both worth knowing:

- Declarations **without a superclass** end in ` {`, which broke a naive regex. Only classes with a
  superclass parsed until the trailing brace was stripped.
- **Enums are not marked as enums** in the declaration; they are detected from
  `: java/lang/Enum`. A parser that keys on the `enum` keyword will misclassify them.

## 2. The `.api` format is too lossy for a consumer-facing manifest

Sample output for the extension functions:

```
net.samyn.kapper.KapperKotlinQueryFunKt [class]
  query(java.sql.Connection, java.lang.String, kotlin.Pair[]) -> java.util.List
  query(java.sql.Connection, java.lang.String, kotlin.jvm.functions.Function2, kotlin.Pair[]) -> java.util.List
  query(java.sql.Connection, kotlin.reflect.KClass, java.lang.String, kotlin.Pair[]) -> java.util.List

net.samyn.kapper.TransactionFunKt [class]
  withTransaction(java.sql.Connection, kotlin.jvm.functions.Function1) -> void

net.samyn.kapper.coroutines.KapperKotlinFlowQueryFunKt [class]
  queryAsFlow(java.sql.Connection, java.lang.String, kotlin.Pair[]) -> kotlinx.coroutines.flow.Flow
```

What is lost, concretely:

| Lost | Example |
|---|---|
| **Parameter names** | `query(sql, vararg args)` becomes `query(java.lang.String, kotlin.Pair[])` |
| **Nullability** | `email: String?` is indistinguishable from `String` |
| **Default values** | `fetchSize: Int = 1000` is invisible |
| **`vararg`** | `vararg args: Pair<String, Any?>` shows as `kotlin.Pair[]` |
| **Extension receiver** | `Connection.query(...)` shows the receiver as the first parameter |
| **Reified-ness** | `inline fun <reified T> query` loses `reified` |
| **Typealiases** | `Args` and `ArgMapper` are absent entirely |
| **KDoc** | absent entirely |
| **Kotlin-level types** | `List<T>` shows as `java.util.List`; `Flow<T>` loses `T` |
| **Suspend-ness** | only visible as a trailing `Continuation` parameter |

For a *consumer-facing* reference — the thing an agent reads to write correct code — parameter names
and nullability are not optional. The `.api` dump cannot supply them.

## 3. Recommendation

Split the two jobs:

- **Manifest source → Dokka JSON.** The build already uses Dokka (`org.jetbrains.dokka`,
  `dokka-javadoc`, `dokkatoo-mkdocs`). Add the JSON output plugin and generate
  `api-manifest.json` from it. It carries parameter names, nullability, defaults, receivers, and KDoc.
  Representative shape (confirm against the plugin version):

  ```json
  {
    "modules": [{
      "name": "kapper",
      "packages": [{
        "name": "net.samyn.kapper",
        "functions": [{
          "name": "query",
          "receiver": { "name": "Connection" },
          "parameters": [
            { "name": "sql", "type": { "name": "String" } },
            { "name": "args", "type": { "name": "Pair", "arguments": ["String", "Any?"] }, "vararg": true }
          ],
          "returnType": { "name": "List", "arguments": ["T"] },
          "documentation": "Execute a SQL query and map the results ..."
        }]
      }]
    }]
  }
  ```

- **CI gate → the `.api` dump.** That is its actual purpose (PKG-6): fail the build on an unintended
  binary-incompatible change. Keep it checked in and CI-gated; do not use it as the manifest.

This also means the manifest generator is a Dokka task, not a `.api` parser. The parser written here
is a spike artefact; it does not need to ship.

## 4. Open questions to resolve before committing

1. **Confirm the Dokka JSON schema** against the pinned Dokka version (`2.2.0`). The shape above is
   representative, not verified.
2. **Does Dokka JSON include `@sample`/KDoc code blocks?** If so, the manifest and the snippets can
   share a source.
3. **Size.** `llms-full.txt` must fit a context window. Decide whether the manifest contributes
   signatures only, or signatures plus KDoc summaries.
4. **Extension receivers and suspend functions** — verify Dokka JSON represents both cleanly.

---

# Spike: snippet pipeline (AGENT-6)

**Question:** can snippets be compiled and extracted without a YAML dependency, and does the drift
gate work?

**Answer: yes.** JSON manifest + a Node extractor, no new dependencies.

**What was built**

- `docs-snippets/` — a Kotlin module (22 snippets across 7 files) that depends on `:kapper` and
  `:kapper-coroutines`. Compiled by `./gradlew check`, so a snippet that stops compiling fails the
  build.
- `docs/snippets.json` — the inventory: id, file, region, title, description, tags, pages.
- `docs/scripts/generate-snippets.mjs` — extracts region-marked snippets and writes
  `docs/_generated/snippets/<id>.md` plus the combined `docs/guide/snippets.md`.

**Result**

```
rendered 22 snippets -> docs/_generated/snippets
wrote docs/guide/snippets.md
```

Re-running produces no diff (idempotent), so the CI drift gate
(`git diff --exit-code -- docs/_generated docs/guide/snippets.md`) will work.

**Design decisions taken**

- **JSON, not YAML.** Gradle has no built-in YAML parser and Node has no YAML dependency installed.
  JSON is parseable on both sides with zero new dependencies. This changes the plan's
  `docs/snippets.yaml` to `docs/snippets.json`.
- **Node extractor, not a Gradle task.** The docs build is already Node/VitePress, and the extractor
  runs in the same place as `generate-llms.mjs`. Gradle's job is only to *compile* the snippets.
- **`status: todo` entries.** The manifest can list a snippet before it is written; the extractor
  warns instead of failing. Useful while the inventory fills in.

**Not yet done (deliberately)**

- The snippets reference the **post-v3** API (`queryScalar`, `Row`, `queryAsSequence`,
  `withTransaction` returning a value, enum mapping). They will not compile until the Wave 1 items
  land. That is the intended dependency, not an oversight.
- `docs-snippets` is not yet in `settings.gradle.kts`; the build file is drafted but unverified
  (the sandbox cannot run Gradle).
- `docs/guide/snippets.md` is not yet in the VitePress sidebar (`docs/.vitepress/config.ts`).
- The `testing` snippet references a hypothetical `KapperTest` (AGENT-7), which does not exist yet.

**Open questions**

1. **Verify the `docs-snippets` build file** — `kotlin("jvm")` without a version relies on the plugin
   being on the buildSrc classpath; confirm it resolves.
2. **Should generated files be committed?** The drift gate requires it. Decide whether
   `docs/_generated/` is committed or regenerated in CI and diffed against a committed copy.
3. **Snippet length.** Some snippets are long. Decide whether the combined page shows full snippets
   or collapses them, and whether `llms-full.txt` uses the same fragments.

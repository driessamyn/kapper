# Handover: Kapper — AGENT-6 (snippets pipeline)

> Self-contained brief for an agent session that did **not** participate in the planning. Read it top
> to bottom before touching anything.

---

## 1. What you are working on

**Kapper** is a lightweight, Dapper-inspired ORM for Kotlin/JVM. It extends `java.sql.Connection`
with `query` / `querySingle` / `execute` / `executeAll` / `executeReturning` / `withTransaction`
extension functions, plus a separate `kapper-coroutines` module. It maps result sets to Kotlin data
classes and Java records.

Read these first, in order:

1. `README.md` — what Kapper is.
2. `docs/guide/philosophy.md` — the design principles. **These are a hard constraint, not a preference.**
3. `kapper-v3-plan.md` — item **0.17 (AGENT-6)** is your task.
4. `kapper-v3-retrieval-chain.md` — the pipeline design (the "single source of truth" section).
5. `docs/scripts/SPIKE-api-manifest.md` — the spike findings.

Do **not** re-plan. The design is settled. If you believe something is wrong, implement the rest and
flag it in your summary.

**Where these files live.** This brief and the planning docs are on the `v3-planning` branch, as are
the spike artefacts (commit `fad2cba`). If you are working in a worktree branched off `main`, read
them from the `v3-planning` worktree — the absolute path is in your task prompt. `README.md` and
`docs/guide/philosophy.md` exist in every worktree.

## 2. What already exists (the spike)

The spike built most of the pipeline on `v3-planning` (commit `fad2cba`):

- `docs-snippets/` — a Kotlin module with 22 snippets across 7 files.
- `docs/snippets.json` — the inventory (id, file, region, title, description, tags, pages).
- `docs/scripts/generate-snippets.mjs` — the Node extractor.
- `docs/guide/snippets.md` and `docs/_generated/snippets/*.md` — generated output.

**Bring these across to your branch** — cherry-pick `fad2cba`, or copy the files.

## 3. Your task

Finish the pipeline so it is wired into the build and CI. The goal: snippets that are **compiled**,
so the docs cannot rot.

1. **Wire the module.** Add `docs-snippets` to `settings.gradle.kts`. Verify
   `docs-snippets/build.gradle.kts` compiles — `kotlin("jvm")` without a version relies on the plugin
   being on the buildSrc classpath; confirm it resolves, and adjust if not.

2. **Trim the snippets to the current API.** The spike's snippets were written against the *post-v3*
   API. Keep only those that compile **and** work against `main` today; mark the rest
   `"status": "todo"` in `docs/snippets.json` (they are deferred to Wave 1).
   - **Keep:** `select-many`, `select-one`, `join`, `insert`, `insert-returning-id`, `update`,
     `delete`, `batch-insert`, `nullable-column`, `coroutines`, `flow`, `connection-setup`,
     `error-handling`.
   - **Defer (`"status": "todo"`):**
     - `select-scalar` — `queryScalar` is Wave 1 (TYPE-6).
     - `in-list` — `IN` expansion is Wave 2 (PARSE-3); the snippet compiles but fails at runtime.
     - `streaming` — `queryAsSequence` is Wave 1 (API-8).
     - `transaction-with-value` — `withTransaction` returns `Unit` until Wave 2 (API-1).
     - `nested-transaction` — nested handling is Wave 2 (API-3); the snippet compiles but is misleading.
     - `enum-property` — enum mapping is Wave 1 (TYPE-2); the snippet compiles but fails at runtime.
     - `custom-mapper` — `Row` is Wave 1 (API-10).
     - `jsonb` — `Row` / `OTHER` handling is Wave 1 (TYPE-4, API-10).
     - `testing` — `kapper-test` is Wave 1 (AGENT-7).

3. **Sidebar.** Add the generated snippets page to the VitePress sidebar in
   `docs/.vitepress/config.ts`.

4. **CI drift gate.** Wire the extractor into the docs build and add a `git diff --exit-code` check on
   the generated artefacts, mirroring the lock-file check in
   `.github/workflows/build-and-test.yml`.

5. **Verify.** `./gradlew check` compiles the snippets; the extractor is idempotent (re-running
   produces no diff); the drift gate fails when a snippet is edited without regenerating.

## 4. Constraints

- **No API changes.** This is a docs/build task.
- **Philosophy is a hard constraint.** See §1.
- **Conventional commits.** CI enforces the format.
- **Dependency lock files.** If you add a dependency, run
  `./gradlew resolveAndLockAll --write-locks` and commit the changed `*.lockfile`s.
- **Do not commit** unless the branch owner asks.

## 5. How to verify

```bash
./gradlew check                                    # compiles docs-snippets, runs ktlint + kover
node docs/scripts/generate-snippets.mjs            # regenerate
git diff --exit-code -- docs/_generated docs/guide/snippets.md   # drift gate
```

## 6. Deliverable

A pull request to **`main`** (the integration branch for all non-breaking work). In the PR
description, state which snippets you kept, which you deferred and why, and the exact commands you
ran with their results.

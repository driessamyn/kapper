# AGENTS.md

Guidance for agents and contributors working **on** Kapper itself. If you are *using* Kapper in an
application, read the [documentation](https://driessamyn.github.io/kapper) instead — this file is
about the repository, not the library's API.

## Repository layout

- `core/` — the `kapper` module (`net.samyn.kapper`). The public API is extension functions on
  `java.sql.Connection`.
- `coroutines/` — the `kapper-coroutines` module (`net.samyn.kapper.coroutines`).
- `benchmark/` — JMH benchmarks. Not published.
- `examples/` — sample projects. Not published.
- `buildSrc/` — shared Gradle convention plugins (`kapper.library-conventions`,
  `kapper.library-publish`). A change here affects every module, so re-run `./gradlew check` after
  editing it.
- `docs/` — the VitePress documentation site. Follow `docs/style-guide.md` (British English, active
  voice, a `Next Steps` section on guide pages).

## Build and test

Run everything from the repository root with the Gradle wrapper:

```bash
./gradlew check                        # ktlint + Kover (90% gate) + unit tests
./gradlew :kapper:test                 # core unit tests only
./gradlew :kapper-coroutines:test      # coroutine unit tests only
./gradlew integrationTest -Ddb=SQLITE  # integration tests against one database
```

`./gradlew check` is the gate CI enforces. Run it before treating a change as done.

### Integration tests

Integration tests live in the `integrationTest` source set, not `test`, and run through the
`integrationTest` task. They use Testcontainers, so **most databases need a working Docker daemon**.
Select the database with `-Ddb=<FLAVOUR>` — for example `POSTGRESQL`, `MYSQL`, `SQLITE`,
`MSSQLSERVER`, `ORACLE` or `DUCKDB`. SQLite runs in-process and needs no Docker. If Docker is not
available, say so rather than claiming the integration tests passed.

### Formatting

ktlint runs on compile: `ktlintFormat` is wired to every `KotlinCompile` task, so formatting is
applied automatically when you build. Write code that ktlint accepts rather than disabling rules.

### Coverage

The Kover gate requires **90% instruction coverage**, and `./gradlew check` fails below it. Add tests
for new code. Unit tests go in `src/test`; integration tests go in `integrationTest`.

## Public API and stability

- The public API is the `net.samyn.kapper` and `net.samyn.kapper.coroutines` packages.
- **`net.samyn.kapper.internal` is not a stable API.** It is excluded from the published
  documentation and may change in any release. Do not depend on it from outside the module, and do
  not treat its current shape as a contract.
- Do not change public signatures or break callers without an explicit, planned reason.

## Commits and releases

- Commits follow [Conventional Commits](https://www.conventionalcommits.org/). CI enforces the
  format with `webiny/action-conventional-commits`, so a non-conforming commit fails the build.
- Releases are cut from a `build: release <version>` commit (for example `build: release 2.0.0`),
  which the git-semver plugin matches to derive the version. `create_release.sh` documents the full
  flow.

## Dependencies

Dependency lock files (`*.lockfile`) are committed, and CI verifies they are current. If you add or
change a dependency, run:

```bash
./gradlew resolveAndLockAll --write-locks
```

and commit the changed lock files.

---
name: kapper-release
description: Cut and publish a Kapper release — pre-flight checks, the release branch, the `build: release <version>` commit, the semver tag, publication to Maven Central and GitHub Packages, and the GitHub release with changelog and benchmark package. Use when asked to release Kapper, cut a version, or publish it.
---

# Releasing Kapper

Kapper releases are cut from a `release/<version>` branch, tagged, and published by CI. The version
is derived from the git history by the `com.github.jmongard.git-semver-plugin`.

## When to use

Use this when the user asks to release Kapper, cut a version, or publish to Maven Central or GitHub
Packages.

## Prerequisites

- A clean working tree on an up-to-date `main`.
- The `gh` CLI, authenticated (`gh auth status`).
- Write access to `driessamyn/kapper`.
- Publishing secrets are configured in GitHub Actions (`GPG_SIGNING_KEY`, `GPG_SIGNING_PASSPHRASE`,
  `OSSRH_USERNAME`, `OSSRH_TOKEN`). You do not need them locally.

## Pre-flight checks

Run all of these first. **Do not push anything until the user has explicitly confirmed the version** —
pushing `release/*` triggers an irreversible Maven Central publish.

1. **Clean tree, current `main`:**
   ```bash
   git status --porcelain     # must be empty
   git checkout main && git pull
   ```

2. **Tests pass:**
   ```bash
   ./gradlew check            # ktlint + Kover 90% gate + tests
   ```

3. **Determine the version, and confirm it with the user.** The plugin derives the next version from
   the tags and the conventional commits since the last one:
   ```bash
   git describe --tags --abbrev=0   # the last release tag, e.g. 2.0.0
   ./gradlew printVersion           # the next version, e.g. 2.1.0-SNAPSHOT
   ```
   The `-SNAPSHOT` suffix means "not yet released"; the release version is that value without the
   suffix (e.g. `2.1.0`). **Show the user the last tag, the derived version, and the proposed release
   version, and get explicit confirmation before continuing.**

4. **Review the changelog** that will go into the GitHub release:
   ```bash
   ./gradlew printChangeLog
   ```

5. **Confirm the branch and tag do not already exist:**
   ```bash
   git ls-remote --tags origin <version>
   git ls-remote --heads origin release/<version>
   ```

## The flow

1. **Create the release branch:**
   ```bash
   git checkout -b release/2.1.0
   ```

2. **Create the release commit** — empty; it exists only to mark the release:
   ```bash
   git commit --allow-empty -m "build: release 2.1.0"
   ```

3. **Tag it:**
   ```bash
   git tag 2.1.0
   ```

4. **Confirm with the user, then push the branch and the tag:**
   ```bash
   git push origin release/2.1.0
   git push origin 2.1.0
   ```
   Pushing `release/*` triggers two workflows:
   - `.github/workflows/mc-publish.yml` → publishes `kapper` and `kapper-coroutines` to **Maven
     Central** (`deployCentralPortal`).
   - `.github/workflows/gh-publish.yml` → publishes to **GitHub Packages** (`deployGithub`).

   ⚠️ The Maven Central publish is triggered by the branch push and is effectively irreversible. This
   is the point of no return — make sure the user has confirmed.

5. **Create the GitHub release and attach the benchmarks:**
   ```bash
   ./create_release.sh
   ```
   This:
   - reads the latest tag (`git describe --tags --abbrev=0`) and the version
     (`./gradlew -PnoDirtyCheck=true -q printVersion`);
   - creates a **draft** GitHub release titled `Kapper <version>` with the generated changelog
     (`gh release create <tag> -d -t "Kapper <version>" -F -`);
   - packages the JMH benchmarks (`./package-benchmarks.sh` →
     `benchmark/build/distributions/kapper-benchmarks-<version>.zip`);
   - uploads the benchmark zip to the release.

6. **Publish the draft release** on GitHub once you have reviewed the changelog.

7. **Merge the release branch to `main`** via a PR (`release/2.1.0` → `main`).

## Versioning

- The version comes from the `com.github.jmongard.git-semver-plugin`, configured in the root
  `build.gradle.kts`:
  ```kotlin
  semver {
      changeLogFormat = git.semver.plugin.changelog.ChangeLogFormat.defaultChangeLog
      releasePattern = "\\\\ABuild: release(?:\\\\([^()]+\\\\))?:"
  }
  ```
- The release commit convention is `build: release <version>` — observed in the history as
  `build: release 2.0.0`, `build: release 1.8.0`, `build: release 1.7.1`, and so on.
- Useful tasks: `./gradlew printVersion`, `./gradlew printSemVersion`, `./gradlew printChangeLog`.
  Pass `-PnoDirtyCheck=true` when the working tree is dirty (the plugin otherwise fails).

## Files involved

| File | Role |
|---|---|
| `create_release.sh` | Creates the GitHub release, packages and uploads the benchmarks |
| `package-benchmarks.sh` | Builds `kapper-benchmarks-<version>.zip` |
| `.github/workflows/mc-publish.yml` | Maven Central publish on `release/*` |
| `.github/workflows/gh-publish.yml` | GitHub Packages publish on `main` and `release/*` |
| `buildSrc/src/main/kotlin/kapper.library-publish.gradle.kts` | The `deployer` config (Central Portal, GitHub, signing) |
| `build.gradle.kts` | The `semver` block |

## Caveats

- **The `releasePattern` does not match the actual release commits.** As written it requires a literal
  backslash (the `\\A` is over-escaped) *and* a colon straight after `release` (or `release(scope)`),
  whereas the commits are `build: release 2.0.0` — a space, not a colon. It matches none of the
  observed release commits. This is **harmless in practice**: the plugin also treats any version tag
  as a release marker, and Kapper always tags, so the pattern is effectively unused. If you want it to
  match, use `"\\Abuild: release"`.
- **Pushing `release/*` publishes to Maven Central.** There is no dry-run. Verify with
  `./gradlew check` and `./gradlew printVersion` first, and get the user's confirmation.
- **`create_release.sh` creates a *draft* release** (`-d`); it is not visible until you publish it on
  GitHub.

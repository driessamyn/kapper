#!/usr/bin/env node
// Extract region-marked snippets from compiled Kotlin into markdown.
//
// Reads docs/snippets.json, pulls each region out of its source file, and writes:
//   docs/_generated/snippets/<id>.md   (one fragment per snippet)
//   docs/guide/snippets.md             (the combined page)
//
// Run it from anywhere: paths are resolved from this script's location.
// The drift gate in CI runs this, then `git diff --exit-code`.

import { readFileSync, writeFileSync, mkdirSync, rmSync } from 'node:fs'
import { join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = resolve(fileURLToPath(new URL('../..', import.meta.url)))
const MANIFEST = join(ROOT, 'docs/snippets.json')
const OUT_DIR = join(ROOT, 'docs/_generated/snippets')
const PAGE = join(ROOT, 'docs/guide/snippets.md')

const manifest = JSON.parse(readFileSync(MANIFEST, 'utf8'))

const sourceCache = new Map()

/** Read a repo-relative file once. */
function readSource(file) {
  if (!sourceCache.has(file)) {
    sourceCache.set(file, readFileSync(join(ROOT, file), 'utf8'))
  }
  return sourceCache.get(file)
}

/** True when the line is a `region: <name>` marker for exactly `name`. */
function isRegionMarker(line, name) {
  const match = line.match(/region:\s*(\S+)/)
  return match != null && match[1] === name
}

/** Extract the body between `region: <name>` and `endregion`, dedented. */
function extractRegion(source, region) {
  const lines = source.split('\n')
  const start = lines.findIndex((l) => isRegionMarker(l, region))
  if (start === -1) return null
  const end = lines.findIndex((l, i) => i > start && l.includes('endregion'))
  if (end === -1) return null
  const body = lines.slice(start + 1, end)
  const indents = body.filter((l) => l.trim()).map((l) => l.match(/^\s*/)[0].length)
  const indent = indents.length ? Math.min(...indents) : 0
  return body
    .map((l) => l.slice(indent))
    .join('\n')
    .replace(/\s+$/, '')
}

rmSync(OUT_DIR, { recursive: true, force: true })
mkdirSync(OUT_DIR, { recursive: true })

const rendered = []
const skipped = []

for (const s of manifest.snippets) {
  if (s.status === 'todo') {
    skipped.push(s.id)
    continue
  }
  let source
  try {
    source = readSource(s.file)
  } catch (cause) {
    throw new Error(`active snippet '${s.id}' could not read ${s.file}`, { cause })
  }
  const code = extractRegion(source, s.region)
  if (code == null) {
    throw new Error(`active snippet '${s.id}' has no region '${s.region}' in ${s.file}`)
  }
  writeFileSync(
    join(OUT_DIR, `${s.id}.md`),
    `### ${s.title}\n\n${s.description}\n\n\`\`\`kotlin\n${code}\n\`\`\`\n`,
  )
  rendered.push({ ...s, code })
}

// Shared setup: the compiled types plus the union of the imports the active
// snippets rely on, rendered once above them.
function renderSetup() {
  const { prelude } = manifest
  if (!prelude) return []
  const code = extractRegion(readSource(prelude.file), prelude.region)
  if (code == null) {
    throw new Error(`prelude has no region '${prelude.region}' in ${prelude.file}`)
  }
  const imports = new Set()
  for (const file of new Set([prelude.file, ...rendered.map((s) => s.file)])) {
    for (const line of readSource(file).split('\n')) {
      const trimmed = line.trim()
      if (trimmed.startsWith('import ')) imports.add(trimmed)
    }
  }
  const setup = [[...imports].sort().join('\n'), code].join('\n\n')
  return [`## ${prelude.title}`, '', prelude.description, '', '```kotlin', setup, '```', '']
}

const page = [
  '# Snippets',
  '',
  'One blessed way to do each common task. Generated from compiled code; do not edit by hand.',
  '',
  ...renderSetup(),
]
for (const s of rendered) {
  page.push(`## ${s.title}`, '', s.description, '', '```kotlin', s.code, '```', '')
}
page.push(
  '## Next Steps',
  '',
  '- Learn about [Queries](./queries.md) for the full query API',
  '- Explore [Mapping](./mapping.md) for mapping result sets to data classes and records',
  '- See [Coroutines](./coroutines.md) for running queries on a coroutine dispatcher',
  '',
)
writeFileSync(PAGE, page.join('\n'))

console.log(`rendered ${rendered.length} snippets -> ${OUT_DIR}`)
console.log(`wrote ${PAGE}`)
if (skipped.length) {
  console.log(`skipped ${skipped.length} todo snippet(s): ${skipped.join(', ')}`)
}

#!/usr/bin/env node
// Extract region-marked snippets from compiled Kotlin into markdown.
//
// Reads docs/snippets.json, pulls each region out of its source file, and writes:
//   docs/_generated/snippets/<id>.md   (one fragment per snippet)
//   docs/guide/snippets.md             (the combined page)
//
// Run from the repo root: node docs/scripts/generate-snippets.mjs
// The drift gate in CI runs this, then `git diff --exit-code`.

import { readFileSync, writeFileSync, mkdirSync, rmSync } from 'node:fs'
import { join, resolve } from 'node:path'

const ROOT = resolve(process.cwd())
const MANIFEST = join(ROOT, 'docs/snippets.json')
const OUT_DIR = join(ROOT, 'docs/_generated/snippets')
const PAGE = join(ROOT, 'docs/guide/snippets.md')

const manifest = JSON.parse(readFileSync(MANIFEST, 'utf8'))

/** Extract the body between `region: <name>` and `endregion`, dedented. */
function extractRegion(source, region) {
  const lines = source.split('\n')
  const start = lines.findIndex((l) => l.includes(`region: ${region}`))
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
const warnings = []

for (const s of manifest.snippets) {
  if (s.status === 'todo') {
    warnings.push(`todo (not written): ${s.id}`)
    continue
  }
  let source
  try {
    source = readFileSync(join(ROOT, s.file), 'utf8')
  } catch {
    warnings.push(`missing file: ${s.id} (${s.file})`)
    continue
  }
  const code = extractRegion(source, s.region)
  if (code == null) {
    warnings.push(`missing region: ${s.id} (${s.region})`)
    continue
  }
  writeFileSync(
    join(OUT_DIR, `${s.id}.md`),
    `### ${s.title}\n\n${s.description}\n\n\`\`\`kotlin\n${code}\n\`\`\`\n`,
  )
  rendered.push({ ...s, code })
}

const page = [
  '# Snippets',
  '',
  'One blessed way to do each common task. Generated from compiled code; do not edit by hand.',
  '',
]
for (const s of rendered) {
  page.push(`## ${s.title}`, '', s.description, '', '```kotlin', s.code, '```', '')
}
writeFileSync(PAGE, page.join('\n'))

console.log(`rendered ${rendered.length} snippets -> ${OUT_DIR}`)
console.log(`wrote ${PAGE}`)
if (warnings.length) {
  console.error('warnings:')
  for (const w of warnings) console.error(`  ${w}`)
}

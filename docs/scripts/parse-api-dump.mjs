#!/usr/bin/env node
// SPIKE: parse a binary-compatibility-validator .api dump into JSON.
//
// Purpose: de-risk step 2.1 of the retrieval chain (the API manifest source).
// Usage:   node docs/scripts/parse-api-dump.mjs docs/scripts/fixtures/kapper.api
//
// The .api format is JVM-descriptor text. This parser recovers class/member names,
// arity, JVM types, generic bounds and modifiers. It CANNOT recover parameter names,
// nullability, default values, vararg, extension receivers, reified-ness, typealiases
// or KDoc -- see the lossiness report this script prints to stderr.

import { readFileSync } from 'node:fs'

const PRIMITIVES = {
  V: 'void',
  Z: 'boolean',
  B: 'byte',
  C: 'char',
  S: 'short',
  I: 'int',
  J: 'long',
  F: 'float',
  D: 'double',
}

/** Decode a single JVM type starting at `i`. Returns [type, nextIndex]. */
function decodeType(s, i) {
  const c = s[i]
  if (c === '[') {
    const [inner, next] = decodeType(s, i + 1)
    return [`${inner}[]`, next]
  }
  if (c === 'L') {
    const end = s.indexOf(';', i)
    const name = s.slice(i + 1, end).replaceAll('/', '.')
    return [name, end + 1]
  }
  if (PRIMITIVES[c]) return [PRIMITIVES[c], i + 1]
  throw new Error(`Cannot decode descriptor at ${i}: ${s.slice(i)}`)
}

/** Decode a full method descriptor `(params)ret`. */
function decodeDescriptor(desc) {
  if (!desc.startsWith('(')) throw new Error(`Not a descriptor: ${desc}`)
  const close = desc.indexOf(')')
  const paramsPart = desc.slice(1, close)
  const params = []
  let i = 0
  while (i < paramsPart.length) {
    const [t, next] = decodeType(paramsPart, i)
    params.push(t)
    i = next
  }
  const [ret] = decodeType(desc, close + 1)
  return { params, returnType: ret }
}

/** Parse `<T : Ljava/lang/Object;, A : Ljava/lang/Object;>` into [{name, bound}]. */
function parseGenerics(s) {
  if (!s) return []
  const inner = s.slice(1, -1)
  return inner
    .split(',')
    .map((p) => p.trim())
    .filter(Boolean)
    .map((p) => {
      const [name, bound] = p.split(':').map((x) => x.trim())
      return { name, bound: bound ? decodeType(bound, 0)[0] : null }
    })
}

const MODIFIERS = new Set(['public', 'protected', 'private', 'static', 'final', 'abstract', 'open', 'synthetic', 'sealed'])

function parseApi(text) {
  const classes = []
  let current = null

  for (const raw of text.split('\n')) {
    const line = raw.replace(/\r$/, '')
    if (!line.trim() || line.trimStart().startsWith('//')) continue

    const isMember = line.startsWith('\t')
    const body = line.trim()

    if (!isMember) {
      // Class declaration. Strip the trailing `{` so declarations without a
      // superclass still match.
      const decl = body.replace(/\s*\{$/, '')
      const m = decl.match(/^(public|protected|private)?\s*(.*?)\bclass\s+([^\s:]+)(?:\s*:\s*(.+))?$/)
      if (!m) continue
      const mods = m[2].trim().split(/\s+/).filter(Boolean)
      const superclass = m[4] ? m[4].trim().replaceAll('/', '.') : null
      // Enums are not marked as such in the declaration; they extend java.lang.Enum.
      const kind = mods.includes('interface')
        ? 'interface'
        : superclass === 'java.lang.Enum'
          ? 'enum'
          : 'class'
      current = {
        name: m[3].replaceAll('/', '.'),
        internalName: m[3],
        kind,
        superclass,
        modifiers: mods.filter((x) => x !== 'interface' && x !== 'enum'),
        fields: [],
        methods: [],
      }
      classes.push(current)
      continue
    }

    if (!current) continue

    // Field: `public static final field NAME TYPE`
    const fieldMatch = body.match(/^((?:\w+\s+)*)field\s+(\S+)\s+(\S+)$/)
    if (fieldMatch) {
      current.fields.push({
        name: fieldMatch[2],
        type: decodeType(fieldMatch[3], 0)[0],
        modifiers: fieldMatch[1].trim().split(/\s+/).filter(Boolean),
      })
      continue
    }

    // Method / constructor: `public [mods] fun [<generics>] name (descriptor)ret`
    const funMatch = body.match(/^((?:\w+\s+)*)fun\s+(<[^>]*>\s*)?(\S+)\s+(\(.*)$/)
    if (funMatch) {
      const modifiers = funMatch[1].trim().split(/\s+/).filter(Boolean)
      const generics = parseGenerics(funMatch[2]?.trim())
      const name = funMatch[3]
      const { params, returnType } = decodeDescriptor(funMatch[4])
      current.methods.push({
        name: name === '<init>' ? 'constructor' : name,
        isConstructor: name === '<init>',
        generics,
        params,
        returnType,
        modifiers,
      })
      continue
    }
  }

  return classes
}

// --- run -------------------------------------------------------------------

const file = process.argv[2]
if (!file) {
  console.error('usage: node parse-api-dump.mjs <file.api>')
  process.exit(2)
}

const classes = parseApi(readFileSync(file, 'utf8'))

const manifest = {
  source: 'binary-compatibility-validator .api dump',
  classes: classes.map((c) => ({
    name: c.name,
    kind: c.kind,
    superclass: c.superclass,
    fields: c.fields,
    methods: c.methods,
  })),
}

console.log(JSON.stringify(manifest, null, 2))

// --- lossiness report ------------------------------------------------------

const methodCount = classes.reduce((n, c) => n + c.methods.length, 0)
console.error('\n--- lossiness report ---')
console.error(`classes: ${classes.length}, methods: ${methodCount}`)
console.error('recoverable:   names, arity, JVM types, generic bounds, modifiers')
console.error('NOT recoverable:')
console.error('  - parameter names        (only types are present)')
console.error('  - nullability            (erased in the descriptor)')
console.error('  - default values         (only a synthetic ctor marker survives)')
console.error('  - vararg                 (appears as an array parameter)')
console.error('  - extension receiver     (appears as the first parameter)')
console.error('  - reified-ness           (erased)')
console.error('  - typealiases            (absent entirely)')
console.error('  - KDoc                   (absent entirely)')
console.error('  - Kotlin-level types     (List<T> shows as java.util.List)')

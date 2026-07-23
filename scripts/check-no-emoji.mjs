#!/usr/bin/env node
// Emoji-ban guard for the masapay monorepo.
//
// Project rule: emoji are forbidden in code, UI, docs and commit messages.
// Visual symbols must use lucide-react icon components instead.
//
// Usage:
//   node scripts/check-no-emoji.mjs            # scan git-staged text files
//   node scripts/check-no-emoji.mjs <paths...> # scan the given files
//
// Exits with code 1 (and prints file:line) if any emoji is found.

import { execSync } from 'node:child_process'
import { readFileSync, existsSync, statSync } from 'node:fs'

// Extended_Pictographic covers the emoji code points; variation selector 16
// (U+FE0F) and zero-width joiner sequences are caught by the pictographic base.
const EMOJI_RE = /\p{Extended_Pictographic}/u

// Files we never scan (binary, generated or lockfiles).
const SKIP_RE =
  /(^|[/\\])(package-lock\.json|yarn\.lock|pnpm-lock\.yaml)$|\.(png|jpe?g|gif|ico|svg|webp|woff2?|ttf|eot|jar|class|lock)$/i

function stagedFiles() {
  const out = execSync('git diff --cached --name-only --diff-filter=ACM', {
    encoding: 'utf8',
  })
  return out.split('\n').filter(Boolean)
}

function isTextFile(file) {
  if (SKIP_RE.test(file)) return false
  if (!existsSync(file)) return false
  const st = statSync(file)
  if (!st.isFile() || st.size > 2 * 1024 * 1024) return false
  return true
}

function scan(file) {
  const hits = []
  let content
  try {
    content = readFileSync(file, 'utf8')
  } catch {
    return hits
  }
  const lines = content.split(/\r?\n/)
  lines.forEach((line, i) => {
    if (EMOJI_RE.test(line)) {
      hits.push({ line: i + 1, text: line.trim().slice(0, 120) })
    }
  })
  return hits
}

const args = process.argv.slice(2)
const files = (args.length ? args : stagedFiles()).filter(isTextFile)

let failed = false
for (const file of files) {
  const hits = scan(file)
  for (const hit of hits) {
    if (!failed) {
      console.error('Emoji forbidden by project rules. Use lucide-react icons.')
      console.error('')
    }
    failed = true
    console.error(`  ${file}:${hit.line}  ${hit.text}`)
  }
}

if (failed) {
  console.error('')
  console.error('Commit aborted. Remove the emoji above and try again.')
  process.exit(1)
}

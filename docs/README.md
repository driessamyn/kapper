# Kapper Documentation

This directory contains the Kapper documentation website built with [VitePress](https://vitepress.dev/).

## Prerequisites

- Node.js 18+ and npm

## Development

Install dependencies:
```bash
npm install
```

Start the development server:
```bash
npm run dev
```

This will start a local development server at `http://localhost:5173` with hot reload.

## Building

Build the static site:
```bash
npm run build
```

The built files will be in `.vitepress/dist/`.

## Preview

Preview the built site locally:
```bash
npm run preview
```

## Structure

- `guide/` - User guides and tutorials
- `examples/` - Code examples
- `blog/` - Blog posts
- `api/` - API reference
- `performance/` - Performance benchmarks
- `.vitepress/config.ts` - VitePress configuration

## Deployment

The documentation is automatically deployed to GitHub Pages via GitHub Actions when changes are pushed to the main branch.
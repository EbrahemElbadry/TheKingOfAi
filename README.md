# TheKingOfAI Portfolio

A dark editorial portfolio for full-stack product engineering, applied AI, automation, and systems work. English is the default experience, with an Arabic toggle in the header. Product starting prices can be viewed in USD, EGP, or EUR.

## Setup

Requirements:

- Node.js `24.18.0`
- Corepack-enabled `pnpm`

Install dependencies:

```bash
make install
```

Run locally:

```bash
make dev
```

Run checks:

```bash
make check
make test
make build
```

## Docker

Build and run the production image:

```bash
docker build -t thekingofai-portfolio .
docker run --rm -p 3000:3000 thekingofai-portfolio
```

## Project structure

- `client/` — React and Vite frontend
- `server/` — Express and tRPC backend
- `shared/` — shared types and constants
- `drizzle/` — database schema and migrations

The contact flow stores the submitted brief through the server endpoint and falls back to email when the endpoint is unavailable.

# QlinkR Order

QlinkR started as a QR link-page product for local businesses: each business
gets engraved wooden QR coasters that open a themed page with its links. This
repository turns it into a multi-business ordering platform: scanning a QR
code shows the catalogue, the customer orders without an account, and staff
see the order live.

- Target architecture and data model: [docs/PLAN.md](docs/PLAN.md)
- The existing product, read from its code: [docs/CURRENT_STATE.md](docs/CURRENT_STATE.md)
- Decisions and their reasons: [docs/DECISIONS.md](docs/DECISIONS.md)

Client data and the pages generated from it are not part of this repository.

## Backend

Spring Boot 4.1.2 on Java 21, Oracle Database Free 23ai, Redis, Flyway.
Lives in `backend/`.

```bash
cd backend
./mvnw spring-boot:run       # starts Oracle and Redis from ../compose.yaml, then the API
curl http://localhost:8080/actuator/health
```

Tests run against a real Oracle and Redis started by Testcontainers:

```bash
cd backend && ./mvnw verify
```

Whole platform in containers: `docker compose --profile full up --build`.

| Phase | Content | State |
|---|---|---|
| 0 | Current state and plan | done |
| 1 | Spring Boot skeleton, Docker Compose, Flyway baseline, health endpoint, CI | done |
| 2 | Businesses, users, roles, JWT login, multi-business isolation | next |
| 3 | Catalogue in 3 languages, scan points, QR generation | |
| 4 | Ordering: idempotency, stock with optimistic locking, state machine | |
| 5 | Real-time staff screen | |
| 6 | Owner dashboard SQL | |
| 7 | Caching and rate limiting | |
| 8 | Load test, measured numbers, deployment plan | |

## The existing link-page product

Marketing site + admin tool + client QR link pages for a wood QR-coaster
business. React + Vite + Tailwind CSS v4 on the frontend, a small Express
server for the admin tool, and **pre-generated static HTML** for the public
`/r/:slug` pages (see "Deployment model" below — this is important).

## Deployment model (two tiers)

The public site (`qlinkr-cards.pages.dev` today, `qlinkr.ma` once bought) is
hosted on **Cloudflare Pages, which only serves static files** — there's no
server running there, so `/admin` and `/api/*` do not and cannot work on
that domain. That's expected, not a bug.

- **Locally (or any persistent host)** — run the full app (`npm run dev` or
  `npm start`). This is where you log into `/admin`, create/edit clients.
  Every create/update/delete automatically regenerates that client's static
  file at `public/r/<slug>.html` (via `server/staticGen.js`).
- **Commit + push** the updated `public/r/*.html` (and `public/404.html`)
  to git. `server/data/restaurants.json` — the actual database — is
  intentionally **not** committed (gitignored); the generated HTML is what
  ships.
- **Cloudflare Pages** auto-deploys on push: build command `npm run build`
  (plain `vite build` — Vite copies everything in `public/`, including your
  generated `/r/*.html` files, straight into `dist/`), output directory
  `dist`.

**Practical implication:** unlike a fully server-backed setup, admin changes
are not instantly live on the public domain — you need to commit and push
after editing. If you want truly-instant publishing later, the fix would be
having the server auto-commit+push after `generatePage()`, not re-adding a
live backend to Cloudflare (which it can't run).

## Getting started

```bash
npm install
cp .env.example .env   # then set a real ADMIN_PASSWORD
npm run dev             # runs Vite (frontend) + Express (API) together
```

Visit `http://localhost:5173` — the marketing page. Visit `/admin` to log
in and create your first client page.

## Structure

- `src/pages/Landing.jsx` — the marketing/pitch page (`/`)
- `src/pages/Admin.jsx` + `src/components/admin/` — the admin tool (local
  use only): login, create/edit form (1–7 links per client), list, QR
  download, "regenerate all" button
- `server/staticGen.js` — generates `public/r/<slug>.html` per client, and
  `public/404.html`, from `server/render/clientPage.js`'s templates
- `server/generateAll.js` — CLI: `npm run generate:static` regenerates every
  page by hand (rarely needed — create/edit/delete already do this per-item)
- `src/config/site.js` — site name, WhatsApp number, **`SITE_URL`** (the
  domain encoded into every QR code — currently the temporary
  `qlinkr-cards.pages.dev`, see "Still to do" below)
- `public/_redirects` — Cloudflare Pages rewrite rule so `/r/<slug>` (no
  `.html`) serves the generated file. Static, hand-written, doesn't depend
  on Cloudflare's "Clean URLs" project setting.
- `src/assets/mockups/` — product photos used in the landing page showcase

## Before going live

Search for `TODO` in:

- `src/config/site.js` — real brand name, WhatsApp number, and **`SITE_URL`**
- `src/components/landing/Pricing.jsx` — real package names/prices
- `.env` — a real `ADMIN_PASSWORD` (never commit this file)

## Admin tool (local only — see "Deployment model")

- URL: `/admin` — not linked anywhere in the site, and marked `noindex`.
  On Cloudflare it 404s (no backend there); run the app locally to use it.
- Each submission creates a permanent page at `/r/<slug>`. The slug is
  auto-suggested from the name but editable before creation — **it's locked
  afterward** since it's what gets engraved.
- After creating a client, download the QR SVG immediately (level-H error
  correction, black-on-white for maximum scanner compatibility). You can
  re-download the QR for any existing client at any time from the list.
- Editing an existing client's name/links is safe and doesn't affect the
  physical QR code — only the slug is permanent. **Remember to commit +
  push `public/r/` afterward** so the change actually goes live.
- Each client also has a **theme** (`src/lib/themes.js` for the picker,
  `server/render/clientPage.js`'s `THEMES` object for the actual colors):
  Simple, Restaurant, Café, Salon de thé, Salle de sport, Salon de beauté.
  Pure CSS, no extra weight. Safe to change anytime — has no effect on the
  QR code.
- **If you ever edit the page template/design** (`server/render/clientPage.js`),
  existing clients' already-generated files won't update on their own —
  run `npm run generate:static` (or the "Regenerate all" flow) afterward,
  then commit + push, or the live pages will still show the old design.

## Cloudflare Pages project settings

For the `qlinkr-cards` project (Workers & Pages → qlinkr-cards → Settings →
Builds & deployments):

- **Build command:** `npm run build`
- **Build output directory:** `dist`

Do not point it at `dist-static` (an old, now-deleted intermediate output
folder from an earlier iteration of this setup).

## Still to do

- Buy the `qlinkr.ma` domain.
- Once bought: point it at the Cloudflare Pages project (custom domain),
  then update `SITE_URL` in `src/config/site.js` to `https://qlinkr.ma`.
- **Regenerate every QR code after that URL change** (`npm run
  generate:static` regenerates the pages; each restaurant's QR itself is
  regenerated from the admin list's "QR" button, since the SVG is generated
  client-side from the current `SITE_URL` at download time, not stored).
  Any QR code downloaded before this point encodes the temporary
  `qlinkr-cards.pages.dev` URL — fine for testing, not for real coasters.

## Running the full app elsewhere (not Cloudflare)

If you ever want `/admin` and instant publishing to work on the public
domain itself, deploy the full Express app (not just `dist/`) to a host with
a persistent process + disk — Render, Railway, Fly.io, or a small VPS:

```bash
npm run build   # builds the React app into dist/
npm start        # node server/index.js — serves dist/ + the API + /r/:slug
```

Environment variables needed: `ADMIN_PASSWORD` (required), `SESSION_SECRET`
(optional — auto-generated and persisted to `.session-secret` if omitted),
`PORT`, `DATA_DIR`.

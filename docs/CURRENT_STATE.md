# QlinkR: current state (read from the code, 2026-11-14)

## What QlinkR really is today

Summary of the link-page product as it exists today:

| Assumption | What the code does |
|---|---|
| Customer sees "an information page about the business" | Customer sees a **link page**: business name and 1 to 7 buttons (menu link, Instagram, Facebook, website, …) with a theme. There's no catalogue, no items and no prices. |
| An Express backend | Express exists but **runs only locally**. Production is **static HTML on Cloudflare Pages**, with no server at all. |
| (implied) a database | No database. Data is one JSON file, `server/data/restaurants.json`, committed to git as the backup. |
| QR codes | Generated **in the browser** (the `qrcode` npm package, SVG, error correction level H) when you click download. They're not stored and not generated on a server. |
| Admin tool | A single admin password (from `.env`), an HMAC-signed session cookie, and a login rate limit of 10 per 15 minutes. There are no users or roles. |

The business is wooden QR coasters, cards and plates for local businesses (restaurant, café, tea room, gym, beauty salon).

## Structure

- `src/pages/Landing.jsx` + `src/components/landing/*`: the marketing site (Hero, How it works, Pricing, WhatsApp buttons). Text is in French.
- `src/pages/Admin.jsx` + `src/components/admin/*`: login, create/edit form, client list, QR preview and download.
- `server/index.js`: Express 5 with security headers, `/api/admin/*` and `/api/restaurants/*`, and server-rendered `/r/:slug`.
- `server/db.js`: JSON-file store with validation (slug format, 1 to 7 links, http/https URLs only, theme whitelist).
- `server/staticGen.js` + `server/render/clientPage.js`: render each client to `public/r/<slug>.html` on every create, edit or delete.
- `src/config/site.js`: `SITE_URL = https://qlinkr-cards.pages.dev`. This URL is encoded in every QR code.

## Data model (current)

```
Restaurant { id: uuid, name, slug (permanent, engraved), theme, links: [{label, url, icon}] (1..7), createdAt, updatedAt }
```

## Routes

| Route | Where it runs | What it does |
|---|---|---|
| `/` | Cloudflare (static) | marketing site |
| `/r/<slug>` | Cloudflare (static HTML file) | public client link page, the page the QR points to |
| `/admin` | local only | admin SPA. It 404s on Cloudflare (no backend) |
| `POST /api/admin/login`, `/logout`, `GET /me` | local only | session |
| `GET/POST/PUT/DELETE /api/restaurants`, `POST /regenerate` | local only | client CRUD and static page generation |

## Deployment

Edit in local admin, which writes `public/r/<slug>.html`. Then `git push`, and Cloudflare Pages runs `vite build` and serves `dist/`. Changes go live only after a push.

## Worth keeping

- **Every `/r/<slug>` URL, forever.** They're engraved on real coasters. The new platform must never break them.
- The landing page, themes and visual style.
- The input validation in `db.js` (slug rules, URL checks) and the security headers.
- Level-H QR codes, black on white.

## Problems found

1. **`.env.example` contains a value that looks like a real admin password.** It should be rotated and replaced with a placeholder.
2. **Client data is committed** (`server/data/restaurants.json`, real client names and social links). That's fine in a private repo, but this repo **must not be made public as it is**, and git history keeps the data even if the file is deleted.
3. The README mentions `public/_redirects`, but that file was removed in an earlier fix. The doc is out of date.
4. No tests.
5. `.gitignore` says the JSON is committed on purpose. The comment warns about this too.

## Visibility

The original repository holds real client data in its history (`server/data/restaurants.json`), so it stays private. This repository contains the code only, with synthetic demo data.

# Decisions

One entry per decision: context, choice, reason.

## D1. Oracle Database instead of PostgreSQL
- **Context:** the ordering platform needs real reporting SQL, row locking and a database that small businesses' IT providers already run.
- **Choice:** Oracle Database Free 23ai in Docker, with Testcontainers `oracle-free` in tests.
- **Reason:** the dashboard SQL, `EXPLAIN PLAN`, locking and sequences are then real Oracle work to discuss. The cost is a bigger image and slower startup.

## D2. Keep the existing React app and the `/r/{slug}` pages
- **Context:** QR codes with `/r/{slug}` are already engraved on wood and sold to clients.
- **Choice:** new routes (`/s/{scanCode}`, `/staff`, `/owner`), and old link pages stay as they are.
- **Reason:** a physical QR code can't be reprinted for free. Breaking those URLs would break real clients.

## D3. Work on branch `qlinkr-order`; `main` keeps deploying
- **Context:** Cloudflare Pages deploys `main` automatically.
- **Choice:** all new work goes on `qlinkr-order`, and nothing merges to `main` until the new platform is ready to replace the live site.
- **Reason:** the live site must keep working until you decide to switch.

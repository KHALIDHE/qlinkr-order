# QlinkR Order: plan

## 1. Goal

Scanning a QR code shows the business's catalogue. The customer orders (or books, or takes a queue ticket) without an account, and the staff see it live. It works for any business type. Payment is at the counter.

## 2. Target architecture

```
 Phone (customer)            Staff / owner browser
      │ scan                        │ JWT
      ▼                             ▼
 ┌──────────────────────── React + Vite + Tailwind (existing app, new routes) ────────────────────────┐
 │  /                landing (unchanged)        /s/{scanCode}   public catalogue + cart + order status │
 │  /r/{slug}        old link pages (unchanged) /staff  /owner  /admin   screens behind login         │
 └───────────────┬───────────────────────────────────────────────────────┬────────────────────────────┘
                 │ REST (JSON)                                           │ WebSocket (STOMP)
                 ▼                                                       ▼
 ┌─────────────────────────────── Spring Boot 4.1.2 API (Java 21) ───────────────────────────────────┐
 │  controller → service → repository     Spring Security + JWT     rate limit     springdoc (Swagger)│
 └──────────────┬───────────────────────────────────────┬───────────────────────────────────────────┘
                ▼                                       ▼
   Oracle Database Free 23ai (Flyway)          Redis (catalogue cache, rate-limit counters)
```

Folder layout on branch `qlinkr-order`: `backend/` (Maven, Spring Boot), the existing React app stays at the root, and `docker-compose.yml` starts Oracle, Redis, the backend and the frontend.

## 3. Versions (checked 2026-11-14)

| Component | Version |
|---|---|
| Java | 21 (Temurin 21.0.13) |
| Spring Boot | 4.1.2 (current stable; 4.2 still RC) |
| Oracle | `gvenzl/oracle-free:23-slim-faststart`, `ojdbc11` 23.26 |
| Flyway | Boot-managed, plus `flyway-database-oracle` |
| Testcontainers | 2.0.5 (`testcontainers-oracle-free`) |
| springdoc-openapi | 3.2.0 |
| ZXing | 3.5.4 |
| JWT | Spring Security OAuth2 Resource Server (Nimbus): no extra library |
| Load test | k6 |

## 4. Data model (Oracle)

```
BUSINESS        (id, name, slug UNIQUE, business_type, default_lang, theme, created_at)
APP_USER        (id, business_id NULL for PLATFORM_ADMIN, email UNIQUE, password_hash, role)
SCAN_POINT      (id, business_id, code UNIQUE, label, kind)         -- kind: TABLE, COUNTER, SHELF, DOOR
CATEGORY        (id, business_id, position)
CATEGORY_I18N   (category_id, lang, name)                           -- PK (category_id, lang)
ITEM            (id, business_id, category_id, price_cents, photo_url, available, stock NULL, version)
ITEM_I18N       (item_id, lang, name, description)                  -- lang: ar, fr, en
CUSTOMER_ORDER  (id, business_id, scan_point_id, kind, status, idempotency_key, total_cents,
                 public_token, created_at, accepted_at, ready_at, completed_at, version)
                 UNIQUE (business_id, idempotency_key)
ORDER_LINE      (id, order_id, item_id, quantity, unit_price_cents)
ORDER_EVENT     (id, order_id, from_status, to_status, actor_user_id, at)   -- history, also feeds stats
LINK_PAGE       (business_id, label, url, icon, position)           -- keeps the old 1..7 links feature
```

- **Isolation:** every business-owned table has `business_id`. Every repository method takes the business ID from the JWT, never from the URL, and integration tests try every endpoint across businesses.
- **Neutral names:** "menu", "table" and "kitchen" are only display labels chosen by `business_type` (RESTAURANT, SHOP, SERVICE).
- `kind` on an order covers ORDER, CALL_ME (shops) and BOOKING/QUEUE_TICKET (services) with the same table.

## 5. Key designs, to be explained in the matching phase

- **Last item:** optimistic locking with `@Version` on `ITEM`. A conflict is retried a few times, and if stock is 0 the answer is "no longer available". Phase 4 includes a short note comparing it with pessimistic `SELECT … FOR UPDATE`.
- **Double tap:** the client sends an `Idempotency-Key`. A unique constraint means a repeat returns the first order.
- **Lifecycle:** an enum `OrderStatus` where each value lists its allowed next states. Invalid transitions throw `InvalidTransitionException`, which returns 409.
- **Dashboard SQL:** real Oracle SQL with `GROUP BY`, `TRUNC(created_at)`, window functions (`RANK() OVER`, moving average), and `EXPLAIN PLAN` + `DBMS_XPLAN` before and after indexes, kept in `docs/sql-notes.md`.

## 6. Moving from Express to Spring Boot without breaking the live site

1. **The live site isn't touched until you decide.** `main` keeps deploying to Cloudflare Pages exactly as today, and all work happens on `qlinkr-order`.
2. **Old QR codes keep working forever.** `/r/{slug}` stays a static page. Later, a "Migrate" script imports each `restaurants.json` entry as a `BUSINESS` + `LINK_PAGE`, and the new backend can serve `/r/{slug}` too. Old coasters can get an "Order" button with no reprint.
3. **New QR codes** point to `/s/{scanCode}`, which is a different path, so the two can't collide.
4. **Hosting:** Cloudflare Pages can't run Java. For a live demo we run everything locally and expose it with a Cloudflare Tunnel, so any phone reaches the laptop over HTTPS. A permanent host (a VPS, Oracle Cloud Free Tier, …) is a later decision.
5. **Public code, private client data:** the original repository has real client data in its history, so it stays private. The ordering platform lives in a new public repository with clean history and a synthetic demo seed.

## 7. Phases (from the prompt)

0. Current state and plan. Done.
1. Spring Boot skeleton, Docker Compose (Oracle, Redis), Flyway baseline, health endpoint, CI.
2. Businesses, users, roles, JWT login, isolation tests.
3. Catalogue (ar/fr/en, RTL), scan points, ZXing QR (PNG/SVG, print sheet), public page.
4. Ordering: cart, idempotency, optimistic locking, state machine, 100-orders-vs-10-items test.
5. Real time: staff screen and live customer status.
6. Owner dashboard SQL, indexes, EXPLAIN notes.
7. Redis cache and rate limiting.
8. k6 load test, README, diagram, demo script, deployment plan.


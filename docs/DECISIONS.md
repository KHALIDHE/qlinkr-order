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

## D4. Stateless JWT, signed with HS256
- **Context:** staff screens and the owner dashboard call the API from browsers and phones.
- **Choice:** the backend issues a short-lived JWT at login (8 hours) and verifies it on every request, with no server session. One symmetric key (HS256) from `JWT_SECRET`, at least 256 bits, checked at startup.
- **Reason:** no session store to share between instances, and the token carries the role and business id the checks need. HS256 fits because the same service issues and verifies; with several verifying services, RS256 would keep the signing key in one place.

## D5. The business id comes from the token, never from the URL
- **Context:** multi-business isolation must hold on every endpoint.
- **Choice:** "my business" endpoints have no business id in their path (`/api/business`, `/api/business/staff`). Services read the id from the verified token, and every member lookup filters on it (`findByIdAndBusinessId`).
- **Reason:** a user cannot reach another business by editing a number in a URL, and the rule lives in one place instead of a check in every controller.

## D6. Another business's resource is "not found", not "forbidden"
- **Context:** an owner of business A asks to delete user 42, who belongs to business B.
- **Choice:** answer 404, exactly as if user 42 did not exist.
- **Reason:** a 403 would confirm that the id exists elsewhere, which leaks information and helps enumeration. 403 is kept for "you are in the right business but your role cannot do this" (staff trying an owner action).

## D7. Optimistic locking for stock, with retry
- **Context:** two customers can order the last item at the same moment.
- **Choice:** `@Version` on `item`. Taking stock updates the row only if its version is unchanged; the loser of a race gets a version conflict, reads again (up to 40 attempts with a short random pause) and then sees the real stock.
- **Reason:** most of the time there is no conflict, so no lock is held. The alternative, pessimistic `SELECT ... FOR UPDATE`, makes every order wait in line on the item row even when stock is plentiful, and holds a lock while the order is built. It would be the better choice if conflicts were the normal case.

## D8. Idempotency key enforced by a unique constraint
- **Context:** a double tap or a mobile network retry must not create two orders.
- **Choice:** the phone sends an `Idempotency-Key`; `UNIQUE (business_id, idempotency_key)` makes Oracle refuse a second order, and the API returns the first one (200 instead of 201).
- **Reason:** a check-then-insert in Java has a race between the check and the insert; the database constraint does not.

## D9. Catalogue cache in Redis, evicted on every change
- **Context:** the public catalogue is read on every scan and changes rarely.
- **Choice:** JSON per business and language in Redis with a 10-minute TTL. Every catalogue edit and stock change deletes the business's entries, so the next scan rebuilds them. If Redis is down, the database answers.
- **Reason:** eviction on write keeps edits visible immediately; the TTL is only a safety net.


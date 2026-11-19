# API contract proposal (WAS-68)

Review required from A/B/C and the frontend owner before endpoint implementation. This file specifies the intended contract; feature endpoints below are NOT implemented by this foundation. Generated /v3/api-docs currently describes implemented endpoints only. Health and the security boundary are operational.

## Wire rules

Base /api/v1; Authorization: Bearer <access token>; JSON camelCase; UUID identifiers as strings. Persisted timestamps and timestamp API fields use ISO-8601 UTC. Money is a JSON number in EUR with at most two decimals; backend calculations use BigDecimal without silent rounding. Single objects and arrays have no wrapper. Paginated lists alone return {content,page,size,totalElements,totalPages}; zero-based page, default size 20, maximum 100.

Existing Flutter display fields are preserved: Deal.pickupStart/pickupEnd are HH:mm strings, pickupWindow is HH:mm–HH:mm, and Order.timeAgo is a display string. These are NOT ISO timestamps. Derive display times using the business timezone at the API mapping boundary (Bucharest demo: Europe/Bucharest); expose additive pickupStartAt/pickupEndAt for absolute times. Existing category labels on BusinessOffer are display strings (e.g. Bakery); Deal.category is a lowercase enum. The proposed `other` category needs an additive frontend enum value before it is returned to Deal consumers.

## Endpoint ownership

| Method | Path | Owner / result |
|---|---|---|
| POST | /auth/register, /auth/login | A; tokens and user |
| POST | /auth/refresh, /auth/logout | A; rotate/revoke refresh token |
| POST | /auth/password-reset/request, /auth/password-reset/confirm | A; generic response, no account enumeration |
| GET/PATCH/DELETE | /users/me | A; current user/update/account deletion |
| PUT/DELETE | /users/me/location | A; consented coordinates/withdraw consent |
| GET | /users/me/impact | C; saved money/meals/impact |
| GET | /admin/users | A; admin-only PageResponse |
| GET | /categories | B; public array |
| GET | /offers/nearby | B; public PageResponse of Deal, lat/lng/radiusKm/category/query/page/size |
| GET | /offers/{id}, /businesses/{id} | B; public detail |
| GET/PATCH | /businesses/me | B; owned business |
| GET/POST | /businesses/me/locations | B; owned locations |
| GET/POST | /businesses/me/offers | B; PageResponse/create |
| GET/PATCH/DELETE | /businesses/me/offers/{id} | B; owned offer/detail/soft deletion |
| POST | /businesses/me/offers/{id}/pause, /businesses/me/offers/{id}/resume | B; updated Offer |
| GET/POST | /orders | C; own PageResponse/create order |
| GET | /orders/{id} | C; own Order |
| POST | /orders/{id}/cancel | C; own Order, refund/restock |
| GET | /businesses/me/orders | C; PageResponse, status filter |
| POST | /businesses/me/orders/{id}/validate-pickup | C; body {code}, completed Order |
| GET | /businesses/me/dashboard | C; aggregates |
| POST | /media | B; authenticated multipart file, {url} |

Login/register/refresh will be explicitly permitted by A when handlers and rate limiting are delivered. Foundation denies all unspecified routes by default; no placeholder login bypass is enabled. JWT validation/filter implementation belongs to A. The principal name must be the user UUID, used by CurrentUser. B implements OfferStockPort/StoragePort, C implements PaymentGateway/IdempotencyStore. Shared interfaces contain no feature dependencies. reserve must atomically fail on insufficient stock; claim is an atomic unique-key insert, never a check-then-insert. Payment providers must honor passed idempotency keys.

## DTO examples

Deal:
```json
{"id":"00000000-0000-0000-0000-000000000001","name":"Bakery bag","shop":"Fresh Bakery","oldPrice":12.00,"price":4.00,"distanceKm":1.2,"pickupStart":"17:30","pickupEnd":"19:00","left":3,"rating":4.9,"imageAsset":"https://example.test/bag.jpg","category":"bakery","description":"Surprise bag"}
```
Offer:
```json
{"id":"00000000-0000-0000-0000-000000000001","title":"Bakery bag","imageAsset":"https://example.test/bag.jpg","available":3,"price":4.00,"originalPrice":12.00,"stockLevel":"low","category":"Bakery","description":"Surprise bag","pickupWindow":"17:30–19:00","isPaused":false}
```
Order:
```json
{"id":"00000000-0000-0000-0000-000000000002","quantity":1,"itemName":"Bakery bag","shop":"Fresh Bakery","imageAsset":"https://example.test/bag.jpg","pickupWindow":"17:30–19:00","code":"123456","price":4.00,"timeAgo":"Just now","isCompleted":false,"status":"ACTIVE","createdAt":"2026-11-16T15:00:00Z","canceledAt":null}
```
Order.price is total paid (quantity × unit price), not unit price. status ACTIVE|COMPLETED|CANCELED; isCompleted is true only for COMPLETED. stockLevel low|medium|high. Deal categories bakery|meals|groceries|desserts|fruitAndVeg|drinks; `other` is pending frontend compatibility review. Remote imageAsset rendering must be agreed with frontend before HTTP wiring (existing assets are local paths).

POST /orders body: {offerId,quantity,paymentMethod}; require Idempotency-Key. Same user/scope/key and request hash replay saved status/body; different payload returns 409 IDEMPOTENCY_MISMATCH; incomplete claims return 409 CONFLICT for retry. Never persist card data. Cancel body {reason}; pickup validation body {code}. DTO implementation and endpoint-specific schemas must be added by owners and tested against these examples.

## Errors

RFC 9457 application/problem+json, status/title/detail plus code/message/traceId/fieldErrors. Each field error is {field,message}; never echo rejected secrets. X-Request-Id is accepted only as 1–64 letters/digits/underscore/hyphen; otherwise generated and returned.

400 VALIDATION_FAILED; 401 UNAUTHORIZED; 403 FORBIDDEN; 404 NOT_FOUND; 409 CONFLICT/OFFER_SOLD_OUT/OFFER_NOT_AVAILABLE/ORDER_NOT_CANCELABLE/INVALID_PICKUP_CODE/IDEMPOTENCY_MISMATCH; 429 RATE_LIMITED; 502 REFUND_FAILED. Failures must not expose SQL, tokens or personal coordinates.

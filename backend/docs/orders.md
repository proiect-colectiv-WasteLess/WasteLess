# Orders, payments and fulfilment

Branches are stacked in dependency order from rari-platform-foundation (20ae32f).
Each branch introduces one Jira ticket; main and the existing backend remain untouched.
Review each diff against the preceding ticket branch, not main.

WAS-42 maps the existing P13 orders schema with scalar identifiers and durable snapshots.
References use the database sequence. Four-digit codes, including leading zeroes, are
serialized per business and protected by a partial unique index. V6 adjusts the P13
six-digit constraint with explicit approval. Existing collisions abort migration
rather than assigning an unknown code to an already-issued ticket.

The foundation framework versions and Java 21 target remain unchanged.

WAS-43: PAYMENT_MODE=mock selects a deterministic local payment port. Charge
amounts 13.13 / 14.14 simulate decline / timeout, 15.15 makes refunds permanently
fail, 16.16 makes the first two refund attempts transiently fail. Normal amounts
succeed. CARD, APPLE_PAY and GOOGLE_PAY are recorded, never card credentials.
Captured payment amount/currency are immutable (no setters, non-updatable columns).
To replace the mock implement common.port.PaymentGateway in a different mode;
order code depends only on that port. P2 currently defines charge/refund, not a
separate authorization operation; charge represents immediate authorize+capture.
Real-provider timeout/DB-commit reconciliation must be implemented by that adapter;
this local provider does not claim distributed atomicity for a future remote bank.

WAS-44: POST /api/v1/orders accepts offerId, integer quantity and paymentMethod.
Idempotency-Key is required; WAS-45 supplies durable replay. Order.id in JSON is
the WL reference, not the internal UUID. The response preserves Flutter keys;
UTC timestamps and date-aware Bucharest pickup labels are computed by an injected
Clock. Source image URLs are preserved; null means the frontend should use a placeholder.
No Flutter files are modified.

A conditional CatalogStockAdapter bridges the existing OfferStockPort reserve/release
contract to the shared schema until B6 is installed. It guards pause, deletion,
expiry and stock atomically in PostgreSQL and participates in the order transaction.
The reader locks the source offer before snapshotting. A future B6 bean replaces
this fallback automatically. Currency EUR and total NUMERIC(10,2) bounds are checked.
Payment is authorize+capture through the common port; no provider class is imported.
Authentication must supply a UUID principal and ROLE_CLIENT/ROLE_BUSINESS as defined
by team A. No authentication bypass or replacement login endpoint is introduced.

WAS-45: Idempotency-Key is required (1..100 characters) on create and cancel.
The database unique key atomically serializes concurrent claims. The claim, side
effects and JSON result commit together: no successful response is visible before
its order/payment commits. JSONB replay preserves JSON values and original status;
object member order is insignificant. Different canonical DTO payload -> 409
IDEMPOTENCY_MISMATCH. Rejected/rolled-back requests do not cache a success.
Keys expire after 24h; expired bodies are never returned and a new claim replaces
the expired row. JdbcIdempotencyStore.purgeExpired is available to P18 cleanup.
The mock rolls back its local operation cache if the application transaction fails.

WAS-46: GET /orders?status=ACTIVE|COMPLETED|CANCELED&page=0&size=20 and
GET /orders/{WL-reference} are CLIENT-only. Other users see 404 on detail.
Pages are bounded to 100, sorted createdAt descending with a stable id tie-breaker.
List/detail use stored snapshots after hard offer deletion (FK becomes null).
The additive refund object contains status, amount, currency and a safe failure
reason for the canceled tab; null means no refund exists. No live offer is needed.

WAS-47: POST /orders/{reference}/cancel with {reason?} requires the owner CLIENT
and Idempotency-Key. Default cutoff is strictly before pickupStartAt; optional
app.orders.cancel-grace-seconds (>=0) extends it. Order lock serializes cancellation
and pickup. Successful full refund, CANCELED transition and restock commit together.
Already-canceled requests return 200 without another refund or restock.

Refund failure policy: persist FAILED refund, keep order ACTIVE and stock reserved,
return 502 REFUND_FAILED. The failed response is replayed for its original request key;
a fresh attempt uses a new key. Detail exposes the failure. No failed refund is
reported as refunded. Provider details are not exposed in error messages.

WAS-48: Refund records map P13. The captured payment row is locked before summing
prior successful refunds; only the remaining captured amount can be returned.
Transient failures retry at 30s, 60s (default maximum 3 attempts, capped backoff 30min);
permanent failures do not retry. Attempts are encoded as safe failure-code#attempt
in P13 failure_reason because the shared schema has no retry columns. No sleep is
used in requests or tests. Config: app.orders.refund-max-attempts (1..8),
refund-backoff-seconds, refund-pending-age-seconds, refund-reconciliation-delay-ms.
Pending refunds are reconciled after 300s by default; a bounded 100-order batch runs
each minute. Each retry locks the order and rechecks ACTIVE/cutoff, so validation
winning first cannot lead to a later refund. Success cancels and restores stock
in the same transaction. Original failed HTTP replies remain replayable; fresh
GET shows reconciliation outcome. Existing failed rows remain visible.

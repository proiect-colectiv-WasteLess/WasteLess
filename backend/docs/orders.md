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

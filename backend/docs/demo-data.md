# Development demo data (P15/P16)

Only the dev profile adds `classpath:db/dev`. Production explicitly restricts Flyway to `classpath:db/migration` and disables the demo placeholder; the SQL refuses to run unless enabled. Never enable dev on a production deployment.

Accounts: `noa@wasteless.app` (CLIENT) and `freshbakery@wasteless.app` (BUSINESS). Both use **WasteLess-Dev-Only-2026!**, BCrypt cost 10. These are public development credentials, never production secrets. Accounts are verified and active; login endpoints are still owned by A.

Stable UUIDs are UUIDv5 of `https://wasteless.app/demo/<key>` in the URL namespace. Keys include customer, owner, business, location, offer-0..3, order-0..6, payment-0..6 and refund. The SQL contains the literal IDs for direct use by developers.

Seven categories, Fresh Bakery (4.9 rating), one Bucharest location and four offers are provided: two active, one paused and one sold out. Pickup times are relative to the first migration run, not automatically rolled forward at every restart. Reset the local volume to refresh the entire dataset; the repeatable seed intentionally does not overwrite user edits. Images are placeholder HTTPS URLs; Flutter rendering still requires frontend integration.

Orders: 3 ACTIVE, 3 COMPLETED, 1 CANCELED; every order has a successful stub payment and the canceled order has a successful full refund. DEMO-0001..0007 references and pickup codes 123450..123456 are stable. Money equals quantity × unit price. Stock for active offers reflects their active allocations (bakery 20 minus 3; meals 12 minus 2). Historical completed orders are from earlier illustrative batches; the sold-out bag is an independent demo state. Stock is illustrative, not an accounting replay of all historical sales.

No API/UI flow is claimed to work until the A/B/C endpoints and Flutter HTTP wiring exist. Tests validate the seeded database directly, including BCrypt credentials, all statuses, payment/refund relationships, repeatability and production exclusion.

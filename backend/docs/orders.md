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

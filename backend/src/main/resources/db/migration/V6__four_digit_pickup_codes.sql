-- WAS-42: user-approved contract adjustment from six digits to four.
-- Existing ambiguous active suffixes must be resolved before upgrade; never silently reassign tickets.
ALTER TABLE orders DROP CONSTRAINT ck_order_code;
UPDATE orders SET pickup_code = right(pickup_code,4) WHERE length(pickup_code)=6;
ALTER TABLE orders ADD CONSTRAINT ck_order_code CHECK (pickup_code ~ '^[0-9]{4}$');
CREATE UNIQUE INDEX uq_order_active_business_code ON orders(business_id,pickup_code) WHERE status='ACTIVE';
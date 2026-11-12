CREATE TABLE user_account (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('CLIENT','BUSINESS')),
    business_name VARCHAR(200),
    pickup_location VARCHAR(255),
    CHECK (email = lower(btrim(email))),
    CHECK (role <> 'BUSINESS' OR
        (length(btrim(business_name)) > 0 AND business_name IS NOT NULL
         AND length(btrim(pickup_location)) > 0 AND pickup_location IS NOT NULL))
);
CREATE TABLE offer (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL REFERENCES user_account(id),
    product_name VARCHAR(200) NOT NULL,
    category VARCHAR(20) NOT NULL CHECK (category IN ('Bakery','Cafe','Restaurant','Grocery','Other')),
    description VARCHAR(2000) NOT NULL,
    original_price NUMERIC(12,2) NOT NULL CHECK (original_price > 0),
    waste_less_price NUMERIC(12,2) NOT NULL CHECK (waste_less_price > 0 AND waste_less_price <= original_price),
    currency VARCHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    pickup_location VARCHAR(255) NOT NULL,
    pickup_start TIMESTAMPTZ NOT NULL,
    pickup_end TIMESTAMPTZ NOT NULL CHECK (pickup_end > pickup_start),
    created_at TIMESTAMPTZ NOT NULL,
    image_url VARCHAR(2048)
);
CREATE INDEX offer_business_idx ON offer(business_id);
CREATE INDEX offer_available_idx ON offer(pickup_end) WHERE quantity > 0;
CREATE TABLE reservation (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES user_account(id),
    offer_id UUID NOT NULL REFERENCES offer(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMPTZ NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    total NUMERIC(22,2) NOT NULL CHECK (total = unit_price * quantity),
    status VARCHAR(20) NOT NULL CHECK (status = 'RESERVED')
);
CREATE INDEX reservation_client_idx ON reservation(client_id);
CREATE INDEX reservation_offer_idx ON reservation(offer_id);
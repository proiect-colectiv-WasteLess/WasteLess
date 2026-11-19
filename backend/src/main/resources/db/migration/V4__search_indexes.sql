CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX ix_location_geo ON business_locations USING gist(location);
CREATE INDEX ix_offer_title_search ON offers USING gin(title gin_trgm_ops);
CREATE INDEX ix_offer_description_search ON offers USING gin(description gin_trgm_ops);
CREATE INDEX ix_business_name_search ON businesses USING gin(name gin_trgm_ops);
CREATE INDEX ix_offer_availability ON offers(status,quantity_available,pickup_end_at);

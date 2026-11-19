CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE TABLE roles (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), code varchar(40) NOT NULL UNIQUE, name varchar(80) NOT NULL, description varchar(255), is_system boolean NOT NULL DEFAULT true, created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE users (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), email citext NOT NULL UNIQUE, password_hash varchar(100) NOT NULL,
 first_name varchar(80), last_name varchar(80), phone varchar(30), avatar_url text, preferred_locale varchar(10) NOT NULL DEFAULT 'en',
 status varchar(20) NOT NULL DEFAULT 'ACTIVE', email_verified boolean NOT NULL DEFAULT false, email_verified_at timestamptz,
 failed_login_attempts smallint NOT NULL DEFAULT 0, locked_until timestamptz, last_login_at timestamptz, password_changed_at timestamptz,
 location_sharing_enabled boolean NOT NULL DEFAULT false, marketing_opt_in boolean NOT NULL DEFAULT false, terms_accepted_at timestamptz,
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(), deleted_at timestamptz, version bigint NOT NULL DEFAULT 0,
 CONSTRAINT ck_users_status CHECK(status IN ('ACTIVE','LOCKED','SUSPENDED','DELETED')), CONSTRAINT ck_users_failed_attempts CHECK(failed_login_attempts>=0));
CREATE TABLE user_roles (user_id uuid NOT NULL, role_id uuid NOT NULL, assigned_at timestamptz NOT NULL DEFAULT now(), assigned_by uuid,
 PRIMARY KEY(user_id,role_id), CONSTRAINT fk_user_roles_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
 CONSTRAINT fk_user_roles_role FOREIGN KEY(role_id) REFERENCES roles(id), CONSTRAINT fk_user_roles_assigner FOREIGN KEY(assigned_by) REFERENCES users(id) ON DELETE SET NULL);
CREATE INDEX ix_user_roles_role ON user_roles(role_id);
CREATE INDEX ix_user_roles_assigner ON user_roles(assigned_by);
CREATE TABLE refresh_tokens (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL, token_hash char(64) NOT NULL UNIQUE,
 device_id varchar(255), user_agent text, ip_address inet, issued_at timestamptz NOT NULL DEFAULT now(), expires_at timestamptz NOT NULL,
 revoked_at timestamptz, replaced_by_token_id uuid, created_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT fk_refresh_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
 CONSTRAINT fk_refresh_replacement FOREIGN KEY(replaced_by_token_id) REFERENCES refresh_tokens(id) ON DELETE SET NULL);
CREATE INDEX ix_refresh_user_expiry ON refresh_tokens(user_id,expires_at);
CREATE INDEX ix_refresh_replacement ON refresh_tokens(replaced_by_token_id);
CREATE TABLE password_reset_tokens (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL, token_hash char(64) NOT NULL UNIQUE, expires_at timestamptz NOT NULL, used_at timestamptz, created_at timestamptz NOT NULL DEFAULT now(), CONSTRAINT fk_reset_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE);
CREATE INDEX ix_reset_user ON password_reset_tokens(user_id);
CREATE TABLE email_verification_tokens (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL, token_hash char(64) NOT NULL UNIQUE, expires_at timestamptz NOT NULL, used_at timestamptz, created_at timestamptz NOT NULL DEFAULT now(), CONSTRAINT fk_verification_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE);
CREATE INDEX ix_verification_user ON email_verification_tokens(user_id);
CREATE TABLE user_locations (user_id uuid PRIMARY KEY, latitude numeric(9,6) NOT NULL, longitude numeric(9,6) NOT NULL, accuracy_m numeric(7,2), created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT fk_location_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
 CONSTRAINT ck_user_coordinates CHECK(latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180), CONSTRAINT ck_location_accuracy CHECK(accuracy_m>=0));
CREATE INDEX ix_users_status ON users(status);
CREATE INDEX ix_users_deleted ON users(deleted_at);
CREATE INDEX ix_user_locations_updated ON user_locations(updated_at);

-- Query tables in an existing Astra keyspace; no relational joins or ALLOW FILTERING.
CREATE TABLE IF NOT EXISTS scanpay_users (id bigint PRIMARY KEY, document text);
CREATE TABLE IF NOT EXISTS scanpay_vendors (id bigint PRIMARY KEY, document text);
CREATE TABLE IF NOT EXISTS scanpay_qr_codes (id bigint PRIMARY KEY, document text);
CREATE TABLE IF NOT EXISTS scanpay_scan_logs (id bigint PRIMARY KEY, document text);
CREATE TABLE IF NOT EXISTS scanpay_wallets (id bigint PRIMARY KEY, document text);
CREATE TABLE IF NOT EXISTS scanpay_transactions (id bigint PRIMARY KEY, status text, document text);
CREATE TABLE IF NOT EXISTS scanpay_entity_lookup (entity text, field text, value text, id bigint, PRIMARY KEY ((entity, field, value), id));
CREATE TABLE IF NOT EXISTS scanpay_accounts_by_email (email text PRIMARY KEY, owner_type text, id bigint);
CREATE TABLE IF NOT EXISTS scanpay_qr_by_vendor (vendor_id bigint, created_at text, id bigint, PRIMARY KEY ((vendor_id), created_at, id)) WITH CLUSTERING ORDER BY (created_at DESC, id DESC);
CREATE TABLE IF NOT EXISTS scanpay_transactions_by_user_month (user_id bigint, month text, initiated_at text, id bigint, PRIMARY KEY ((user_id, month), initiated_at, id)) WITH CLUSTERING ORDER BY (initiated_at DESC, id DESC);
CREATE TABLE IF NOT EXISTS scanpay_transactions_by_vendor_month (vendor_id bigint, month text, initiated_at text, id bigint, PRIMARY KEY ((vendor_id, month), initiated_at, id)) WITH CLUSTERING ORDER BY (initiated_at DESC, id DESC);
CREATE TABLE IF NOT EXISTS scanpay_payments_by_request (user_id bigint, request_key text, transaction_id bigint, PRIMARY KEY ((user_id), request_key));
CREATE TABLE IF NOT EXISTS scanpay_revoked_tokens (jti text PRIMARY KEY) WITH default_time_to_live = 604800;


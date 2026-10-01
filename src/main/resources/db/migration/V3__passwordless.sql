-- Passwordless login storage (spec.md §5): one-time-token magic links and
-- WebAuthn passkeys. Columns match what Spring Security's OTT and WebAuthn
-- JDBC components read/write; types are SQLite-friendly (text/blob/integer,
-- timestamps as text like the rest of the schema).
-- Passkey user entities need no table: AppUserWebAuthnUserEntityRepository
-- maps them onto app_user (id = 8-byte big-endian app_user.id, name = email).

-- Magic-link tokens. token_value holds the SHA-256 digest of the value in the
-- emailed link; consuming deletes the row, so a link is single use.
create table one_time_tokens (
    token_value text not null primary key,
    username text not null,
    expires_at text not null
);
create index idx_one_time_tokens_expires_at on one_time_tokens (expires_at);

-- WebAuthn credential records (Spring Security JdbcUserCredentialRepository).
-- credential_id / user_entity_user_id are the base64url text of the WebAuthn
-- byte arrays; user_entity_user_id decodes back to app_user.id.
create table user_credentials (
    credential_id text not null primary key,
    user_entity_user_id text not null,
    public_key blob not null,
    signature_count integer not null,
    uv_initialized boolean not null,
    backup_eligible boolean not null,
    authenticator_transports text,
    public_key_credential_type text,
    backup_state boolean not null,
    attestation_object blob not null,
    attestation_client_data_json blob not null,
    created text not null,
    last_used text,
    label text,
    unique (user_entity_user_id, credential_id)
);
create index idx_user_credentials_user_entity on user_credentials (user_entity_user_id);

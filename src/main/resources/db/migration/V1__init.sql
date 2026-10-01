-- Grove schema v1 — spec.md §6 (data model), minus passkey_credential
-- (Spring Security WebAuthn owns its tables) and push_subscription (lands with notifications).
-- SQLite: text for enums/dates, integer cents for money. Foreign keys enforced
-- via PRAGMA foreign_keys=ON (see datasource hikari connection-init-sql).

create table creche (
    id integer primary key,
    name text not null,
    address text,
    timezone text,
    logo_url text
);

create table app_user (
    id integer primary key,
    email text not null unique,
    name text not null,
    password_hash text,
    status text not null,
    created_at text not null
);

create table membership (
    id integer primary key,
    user_id integer not null references app_user (id),
    creche_id integer not null references creche (id),
    role text not null
);

create table invite (
    id integer primary key,
    creche_id integer not null references creche (id),
    email text not null,
    role text not null,
    token_hash text not null,
    expires_at text not null,
    accepted_at text
);

create table room (
    id integer primary key,
    creche_id integer not null references creche (id),
    name text not null,
    capacity integer not null,
    age_min_months integer not null,
    age_max_months integer not null
);

create table child (
    id integer primary key,
    creche_id integer not null references creche (id),
    first_name text not null,
    last_name text not null,
    dob text not null,
    room_id integer references room (id),
    start_date text,
    end_date text,
    allergies text,
    medical_notes text,
    photo_consent boolean not null
);

create table guardian (
    id integer primary key,
    creche_id integer not null references creche (id),
    user_id integer references app_user (id),
    child_id integer not null references child (id),
    relationship text,
    can_collect boolean not null,
    is_billing_contact boolean not null
);

create table emergency_contact (
    id integer primary key,
    creche_id integer not null references creche (id),
    child_id integer not null references child (id),
    name text not null,
    phone text,
    relationship text
);

create table attendance (
    id integer primary key,
    creche_id integer not null references creche (id),
    child_id integer not null references child (id),
    date text not null,
    check_in text,
    check_out text,
    status text not null,
    absence_reason text
);

create table diary_entry (
    id integer primary key,
    creche_id integer not null references creche (id),
    child_id integer not null references child (id),
    type text not null,
    body text,
    media_url text,
    recorded_by integer references app_user (id),
    recorded_at text not null,
    visible_to_parents boolean not null
);

create table fee_item (
    id integer primary key,
    creche_id integer not null references creche (id),
    child_id integer not null references child (id),
    name text not null,
    amount_cents integer not null,
    frequency text not null,
    valid_from text,
    valid_to text
);

create table subvention (
    id integer primary key,
    creche_id integer not null references creche (id),
    child_id integer not null references child (id),
    type text not null,
    hours_per_week real not null,
    hourly_rate_cents integer not null,
    reference_code text,
    valid_from text,
    valid_to text
);

create table invoice (
    id integer primary key,
    creche_id integer not null references creche (id),
    family_key text not null,
    period_start text not null,
    period_end text not null,
    number text not null,
    status text not null,
    total_cents integer not null,
    due_date text,
    pdf_url text
);

create table invoice_line (
    id integer primary key,
    creche_id integer not null references creche (id),
    invoice_id integer not null references invoice (id),
    child_id integer not null references child (id),
    description text,
    amount_cents integer not null
);

create table payment (
    id integer primary key,
    creche_id integer not null references creche (id),
    invoice_id integer not null references invoice (id),
    amount_cents integer not null,
    method text,
    paid_on text,
    reference text
);

create table conversation (
    id integer primary key,
    creche_id integer not null references creche (id),
    type text not null,
    title text
);

create table conversation_member (
    id integer primary key,
    creche_id integer not null references creche (id),
    conversation_id integer not null references conversation (id),
    user_id integer not null references app_user (id),
    last_read_at text
);

create table message (
    id integer primary key,
    creche_id integer not null references creche (id),
    conversation_id integer not null references conversation (id),
    sender_id integer references app_user (id),
    body text,
    attachment_url text,
    sent_at text not null
);

create table form_template (
    id integer primary key,
    creche_id integer not null references creche (id),
    title text not null,
    body_html text,
    requires_signature boolean not null
);

create table form_request (
    id integer primary key,
    creche_id integer not null references creche (id),
    template_id integer not null references form_template (id),
    child_id integer not null references child (id),
    guardian_id integer references guardian (id),
    status text not null,
    signed_at text,
    signature_png_url text,
    signed_pdf_url text,
    signer_ip text
);

create table audit_log (
    id integer primary key,
    creche_id integer not null references creche (id),
    user_id integer references app_user (id),
    action text not null,
    entity text not null,
    entity_id integer,
    at text not null
);

create index idx_membership_creche on membership (creche_id);
create index idx_invite_creche on invite (creche_id);
create index idx_invite_email on invite (email);
create index idx_room_creche on room (creche_id);
create index idx_child_creche on child (creche_id);
create index idx_child_room on child (room_id);
create index idx_guardian_creche on guardian (creche_id);
create index idx_guardian_child on guardian (child_id);
create index idx_guardian_user on guardian (user_id);
create index idx_emergency_contact_creche on emergency_contact (creche_id);
create index idx_emergency_contact_child on emergency_contact (child_id);
create index idx_attendance_creche on attendance (creche_id);
create index idx_attendance_child_date on attendance (child_id, date);
create index idx_diary_entry_creche on diary_entry (creche_id);
create index idx_diary_entry_child on diary_entry (child_id);
create index idx_fee_item_creche on fee_item (creche_id);
create index idx_fee_item_child on fee_item (child_id);
create index idx_subvention_creche on subvention (creche_id);
create index idx_subvention_child on subvention (child_id);
create index idx_invoice_creche on invoice (creche_id);
create index idx_invoice_family_key on invoice (family_key);
create index idx_invoice_line_creche on invoice_line (creche_id);
create index idx_invoice_line_invoice on invoice_line (invoice_id);
create index idx_invoice_line_child on invoice_line (child_id);
create index idx_payment_creche on payment (creche_id);
create index idx_payment_invoice on payment (invoice_id);
create index idx_conversation_creche on conversation (creche_id);
create index idx_conversation_member_creche on conversation_member (creche_id);
create index idx_conversation_member_conversation on conversation_member (conversation_id);
create index idx_conversation_member_user on conversation_member (user_id);
create index idx_message_creche on message (creche_id);
create index idx_message_conversation on message (conversation_id);
create index idx_form_template_creche on form_template (creche_id);
create index idx_form_request_creche on form_request (creche_id);
create index idx_form_request_child on form_request (child_id);
create index idx_form_request_template on form_request (template_id);
create index idx_audit_log_creche on audit_log (creche_id);
create index idx_audit_log_user on audit_log (user_id);

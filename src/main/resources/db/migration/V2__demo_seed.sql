-- Demo seed: Grove Family Creche (spec.md §4-§5, demo.md fee screen names).
-- Owner demo@grove.ie / "grove" (bcrypt, cost 10). Fixed literals only.

insert into creche (id, name, address, timezone)
values (1, 'Grove Family Creche', '12 Ashwood Grove, Dublin 6, D06 X2K4', 'Europe/Dublin');

insert into app_user (id, email, name, password_hash, status, created_at)
values (1, 'demo@grove.ie', 'Demo Owner',
        '{bcrypt}$2a$10$dSx/UWf/qUzmfwJnFVMybeVXV.I4pFcAmRIutksJu18XyE3GTrx7i',
        'ACTIVE', '2026-09-01 09:00:00');

insert into membership (id, user_id, creche_id, role) values (1, 1, 1, 'OWNER');

insert into room (id, creche_id, name, capacity, age_min_months, age_max_months) values
  (1, 1, 'Nestlings',  8,  0, 12),
  (2, 1, 'Wobblers',  12, 12, 24),
  (3, 1, 'Explorers', 14, 24, 36),
  (4, 1, 'Pre-school', 16, 36, 60);

-- Same families as the Easy-fees stub (BillingPageController SEED).
-- Dates carry the time part: xerial (date_class=text) only parses that format.
insert into child (id, creche_id, first_name, last_name, dob, room_id, start_date, allergies, photo_consent) values
  (1, 1, 'Maria',   'Jones',   '2025-03-10 00:00:00.000', 2, '2025-09-01 00:00:00.000', null,             1),
  (2, 1, 'Aisling', 'Byrne',   '2026-01-15 00:00:00.000', 1, '2026-02-02 00:00:00.000', 'Penicillin',     1),
  (3, 1, 'Leo',     'Wilson',  '2024-05-02 00:00:00.000', 3, '2024-08-19 00:00:00.000', null,             1),
  (4, 1, 'Aoife',   'Murphy',  '2022-11-21 00:00:00.000', 4, '2025-01-06 00:00:00.000', null,             1),
  (5, 1, 'Fionn',   'O''Brien','2024-02-14 00:00:00.000', 3, '2024-06-10 00:00:00.000', 'Mild egg allergy', 1);

-- Guardian names live on the fees stub, not in the schema: user_id stays open
-- until a parent account is invited (later chunk).
insert into guardian (id, creche_id, user_id, child_id, relationship, can_collect, is_billing_contact) values
  (1, 1, null, 1, 'Mother', 1, 1),  -- Alison Jones
  (2, 1, null, 1, 'Father', 1, 0),  -- John Jones
  (3, 1, null, 2, 'Mother', 1, 1),  -- Mary Byrne
  (4, 1, null, 3, 'Mother', 1, 1),  -- Sarah Wilson
  (5, 1, null, 4, 'Mother', 1, 1),  -- Denise Murphy
  (6, 1, null, 5, 'Mother', 1, 1);  -- Niamh O'Brien

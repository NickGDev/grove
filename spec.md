# Creche platform MVP spec (Spring Boot, 2026)

A minimal-code build of the core of a childcare management product: account management, a creche admin area, a parent portal and a working login. It is a mobile-first web app (installable PWA), not a native app.

## 1. What the research found

Grove provides three connected apps to Irish childcare providers.

| App | Users | Key functions on the public site |
| --- | --- | --- |
| Manager | Owners, managers | Inspection-ready reports, NCS and attendance tracking, fee management, occupancy planning and waiting lists, checklists and forms with digital signatures, messaging |
| Care | Classroom staff (tablet) | Daily logs (meals, naps, toileting), observations and photos, links to Aistear, Siolta and Montessori |
| Family | Parents (iOS, Android) | Photos and updates, observations, instant messaging with read receipts and push notifications, forms to sign, invoices |

The fee module ties three things together: subvention tracking (hours claimed x hourly rate), automated invoices (with CHICK codes) and paperless direct debit through a payment partner.

The positioning: one system, three audiences, admin removed from the day.

**Out of scope for the MVP** (each is a large feature on its own): NCS/Pobal report generation, curriculum-linked observations, direct debit collection, automated room transitions, native apps. The design below leaves room for each.

Grove uses its own branding and copy. Recreate the functionality, not any third-party assets.

## 2. MVP scope

| Area | In the MVP | Later |
| --- | --- | --- |
| Accounts | Creche sign-up, staff and parent invites, roles, password reset, passkeys, session control | SSO, SMS codes |
| Admin | Children, families, rooms, staff, attendance, fees and invoices, payments, messaging, daily diary, forms with signatures, basic reports | Occupancy planner, NCS reports, rota, checklists |
| Parent portal | Child overview and diary, invoices and balance, messages, forms to sign, report an absence | Photo albums, learning journals, online payment |
| Platform | Multi-creche tenancy, audit log, GDPR export and delete | Multi-site groups, billing for the SaaS itself |

## 3. Stack

| Concern | Choice | Why |
| --- | --- | --- |
| Language, framework | Java 25 LTS, Spring Boot 4.x | Current generation; check versions on start.spring.io when you begin |
| UI | Thymeleaf templates + htmx | Server-rendered HTML with partial updates. No SPA, no build step, very little JavaScript |
| Styling | Tailwind CSS (standalone CLI, no Node) with a small component layer | Mobile-first utilities. Reuse the tokens from the Easy fees UX guide |
| Small interactions | Alpine.js only where needed (menus, dialogs) | Around 15 KB, no tooling |
| Mobile | PWA: web manifest, service worker, Web Push | One codebase, installable on iOS and Android, no app-store release cycle |
| Database | PostgreSQL 17+, Flyway migrations, Spring Data JPA | Plain relational model fits this domain |
| Auth | Spring Security 7: form login, one-time-token (magic link), WebAuthn passkeys | All built in, so login needs almost no custom code |
| Files | S3-compatible storage (photos, signed PDFs), local disk in development | Keeps the app stateless |
| Email | Spring Mail with a transactional provider hosted in the EU | Invites, resets, magic links, invoices |
| PDF | OpenPDF or a Thymeleaf-to-PDF library | Invoices and signed forms |
| Realtime | htmx SSE extension for message updates, Web Push for alerts | No WebSocket infrastructure |
| Hosting | One container, one Postgres, EU region | GDPR and simple operations |

Structure it as a modular monolith with one package per feature: `account`, `creche`, `child`, `attendance`, `billing`, `messaging`, `forms`, `portal`, `shared`.

## 4. Roles and account management

Roles: `OWNER`, `MANAGER`, `STAFF`, `PARENT`. A user can have a membership in more than one creche (for example a parent with children at two sites, or a manager across sites).

| Role | Can do |
| --- | --- |
| Owner | Everything, including billing settings, user management, deleting the creche |
| Manager | Children, families, fees, invoices, reports, forms, staff invites (not owners) |
| Staff | Attendance, daily diary, messages, view children in their rooms |
| Parent | Only their own children's data, their invoices, their messages, their forms |

### Account features

1. **Creche sign-up:** email, password or passkey, creche name. Verify the email, then land in a short setup checklist (add a room, add a child, invite a parent).
2. **Invites:** managers invite staff and parents by email. The link contains a single-use token that expires in 7 days. The invitee sets up a passkey or password.
3. **Login options:** email and password, magic link by email, passkey. Parents default to magic link or passkey, which avoids password support requests.
4. **Password reset:** the magic link doubles as reset.
5. **Profile:** change name, email (re-verify), password, register or remove passkeys.
6. **Sessions:** list active sessions and revoke them. Remember-me for 30 days on a trusted device.
7. **Deactivate, not delete:** deactivated users lose access, and their records stay for audit and legal retention.
8. **Audit log:** who viewed or changed a child's record, invoice or form. Visible to owners.

## 5. Login design

The login must work on day one and across both portals.

```java
@Bean
SecurityFilterChain security(HttpSecurity http, MagicLinkSender sender) throws Exception {
  return http
    .authorizeHttpRequests(a -> a
      .requestMatchers("/login/**", "/invite/**", "/signup/**",
                       "/css/**", "/js/**", "/manifest.webmanifest", "/sw.js").permitAll()
      .requestMatchers("/admin/**").hasAnyRole("OWNER", "MANAGER", "STAFF")
      .requestMatchers("/portal/**").hasRole("PARENT")
      .anyRequest().authenticated())
    .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/home", true))
    .oneTimeTokenLogin(o -> o.tokenGenerationSuccessHandler(sender))
    .webAuthn(w -> w.rpName("Your Creche App")
                    .rpId("yourapp.ie")
                    .allowedOrigins("https://app.yourapp.ie"))
    .rememberMe(r -> r.tokenValiditySeconds(60 * 60 * 24 * 30))
    .logout(Customizer.withDefaults())
    .build();
}
```

**`/home` router:** after login, send the user to `/admin` or `/portal` based on role. A user with several memberships sees a small creche picker first.

**Hardening checklist:**

- Passwords hashed with Spring's delegating encoder (bcrypt or Argon2). Minimum length 12, no forced rotation.
- Rate limit login and magic-link requests per IP and per email. Always show the same message whether or not the email exists.
- CSRF stays enabled. htmx sends the token via a meta tag and `hx-headers`.
- Session cookie: `Secure`, `HttpOnly`, `SameSite=Lax`. Sessions stored in Postgres (Spring Session JDBC) so restarts do not log people out.
- Security headers: HSTS, a strict Content-Security-Policy, `X-Content-Type-Options`.
- Magic-link tokens: single use, 10-minute expiry, stored hashed.
- Every query is scoped to the user's creche (see section 6).

## 6. Data model

All money is stored as integer cents. Every business table has `creche_id`, enforced with Hibernate's `@TenantId` so no query can cross creches by accident.

```
creche(id, name, address, timezone, logo_url)
app_user(id, email, name, password_hash, status, created_at)
membership(user_id, creche_id, role)
passkey_credential(...)            -- managed by Spring Security
invite(id, creche_id, email, role, token_hash, expires_at, accepted_at)

room(id, creche_id, name, capacity, age_min_months, age_max_months)
child(id, creche_id, first_name, last_name, dob, room_id, start_date,
      end_date, allergies, medical_notes, photo_consent)
guardian(id, creche_id, user_id, child_id, relationship,
         can_collect, is_billing_contact)
emergency_contact(id, child_id, name, phone, relationship)

attendance(id, creche_id, child_id, date, check_in, check_out,
           status  -- PRESENT | ABSENT | HOLIDAY
           absence_reason)
diary_entry(id, creche_id, child_id, type,   -- MEAL | NAP | TOILET | ACTIVITY | NOTE | PHOTO
            body, media_url, recorded_by, recorded_at, visible_to_parents)

fee_item(id, creche_id, child_id, name, amount_cents, frequency, valid_from, valid_to)
subvention(id, creche_id, child_id, type, hours_per_week,
           hourly_rate_cents, reference_code, valid_from, valid_to)
invoice(id, creche_id, family_key, period_start, period_end, number,
        status,  -- DRAFT | SENT | PART_PAID | PAID | VOID
        total_cents, due_date, pdf_url)
invoice_line(id, invoice_id, child_id, description, amount_cents)   -- negative for subventions
payment(id, creche_id, invoice_id, amount_cents, method, paid_on, reference)

conversation(id, creche_id, type, title)       -- DIRECT | GROUP | ROOM
conversation_member(conversation_id, user_id, last_read_at)
message(id, conversation_id, sender_id, body, attachment_url, sent_at)

form_template(id, creche_id, title, body_html, requires_signature)
form_request(id, creche_id, template_id, child_id, guardian_id, status,
             signed_at, signature_png_url, signed_pdf_url, signer_ip)

push_subscription(id, user_id, endpoint, keys)
audit_log(id, creche_id, user_id, action, entity, entity_id, at)
```

**Invoice calculation:** for each child, add active fee items for the period, subtract `hours_per_week x hourly_rate` for each active subvention (prorated for the days in range), and produce lines. Group lines by billing contact into one invoice per family.

## 7. Screens

### Creche admin (`/admin`, sidebar on desktop, bottom tab bar on mobile)

| Screen | Purpose |
| --- | --- |
| Dashboard | Today's headcount by room, absences, unpaid total, unread messages |
| Attendance | Room register for today with one-tap check-in and check-out. Week view for history |
| Children | List with search, filter by room. Profile with guardians, medical info, fees, subventions, diary |
| Families | Guardians and their linked children, invite status |
| Diary | Quick-add entries (meal, nap, toilet, note, photo) for one child or a whole room |
| Fees | Week and month view: total due, paid, per-child rows. Generate invoices, send, record payment. This is the screen from your Easy fees screenshot |
| Messages | Inbox, one-to-one, group and room conversations, read receipts |
| Forms | Build from a template, send to parents, track who has signed |
| Reports | Attendance by child and period, fees and payments, sign-in sheet. CSV and print-friendly PDF |
| Settings | Rooms, staff, roles, creche profile, invoice details |

### Parent portal (`/portal`, phone-first, bottom tabs)

| Tab | Content |
| --- | --- |
| Today | The child's diary for today (meals, naps, photos), attendance status. Switcher if more than one child |
| Messages | Conversations with staff, read receipts |
| Fees | Balance, invoices, PDF download, payment history |
| Forms | Pending forms with a signing screen (finger-drawn signature), signed history |
| Account | Profile, passkeys, notification settings, report an absence |

## 8. Mobile and low-code choices

- **Server-rendered first.** Each page is a Thymeleaf template. htmx swaps only the parts that change (`hx-get`, `hx-post`, `hx-swap`). Check-in buttons, message send and invoice actions return small HTML fragments.
- **`hx-boost` on links and forms** gives an app-like feel without client routing.
- **PWA basics:** `manifest.webmanifest` with icons, standalone display mode, a small service worker that caches the shell and shows an offline page. Web Push (VAPID) for new messages and invoices. iOS supports push for installed home-screen apps.
- **Touch targets at least 44px**, bottom navigation on phones, sidebar from 900px up.
- **Camera and photos:** a plain `<input type="file" accept="image/*" capture>` works on phones. Resize server-side before storing.
- **Signatures:** the `signature_pad` library on a canvas. Store the PNG and a rendered PDF of the signed form.
- **No client state management.** The server is the source of truth.

## 9. Compliance and safety

This system holds children's personal and medical data, so treat it as high-risk from the start.

- **GDPR:** EU hosting, a data processing agreement template for creches, a documented lawful basis per data type, and parental consent recorded for photos.
- **Access control:** parents see only linked children. Staff see only their creche. Test this with automated tests that try to cross boundaries.
- **Retention and erasure:** configurable retention periods, export per child, and anonymise on request where the law allows. Confirm retention rules for Irish childcare records with a professional.
- **Encryption:** TLS everywhere, encrypted database volumes and object storage, and secrets held outside the repo.
- **Backups:** daily, encrypted, restore tested.
- **Photos:** private storage with short-lived signed URLs. Never public links.

I am not a lawyer, so get a review of the data protection setup before you take real child data.

## 10. Build plan

| Step | Deliverable |
| --- | --- |
| 1 | Project skeleton, Flyway, tenancy, layout templates, Tailwind tokens |
| 2 | Sign-up, login (password, magic link, passkey), invites, roles, profile |
| 3 | Rooms, children, guardians, parent linking |
| 4 | Attendance and diary, with the parent Today tab |
| 5 | Fee items, subventions, invoice generation, PDF, payments, the Fees screen |
| 6 | Messaging with read receipts, SSE updates, Web Push |
| 7 | Forms with signatures |
| 8 | Reports, audit log, GDPR export, hardening, PWA polish |

**Testing:** Testcontainers with Postgres for repository and integration tests, Spring Security test support for role checks, and a handful of Playwright end-to-end runs on a phone viewport (sign up, invite a parent, check in a child, send an invoice, sign a form).

## 11. Decisions to confirm

1. **Market:** Ireland only at first? That decides currency, subvention types (ECCE, NCS) and inspection reports.
2. **Payments:** record-only in the MVP, or add SEPA direct debit early? Direct debit needs a payment partner and its own onboarding.
3. **Native apps:** is a PWA acceptable, or must the parent app be in the app stores? A thin wrapper can come later without rewriting.
4. **Staff device:** are classroom staff on shared tablets? If so, add a fast staff PIN switch on a shared session.
5. **Migration:** do you need CSV import for children and families on day one? It is cheap to add and helps onboarding.
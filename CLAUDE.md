# Grove — Creche Management MVP

Childcare management SaaS (manager, staff, parent surfaces) built with modern Spring Boot 4 and Java 25.

Source of truth, read before building:
- `docs/spec.md` — full build spec: scope, stack, data model, screens, security, build plan (§10), open decisions (§11).
- `docs/design.md` — UX guide for the fees screen: tokens, components, states.

Conflict rule: `docs/spec.md` owns architecture. `docs/design.md`'s "React + shadcn" build note is stale — take its tokens and components, not its stack.

## Stack
Java 25 LTS, Spring Boot 4.x (confirm exact versions on start.spring.io at scaffold time). Thymeleaf + htmx, Alpine.js only for menus/dialogs. Tailwind standalone CLI, no Node. **SQLite** (xerial `sqlite-jdbc` + Hibernate community `SQLiteDialect`), not Postgres — local single-file demo DB; Flyway if its SQLite support resolves cleanly, else `schema.sql` via `spring.sql.init` (documented deviation, upgrade path is Postgres + Flyway untouched). Spring Security 7: form login, one-time-token magic link, WebAuthn passkeys. OpenPDF for invoices/signed forms. htmx SSE for live updates; Web Push deferred until HTTPS (see Interview context). PWA (manifest, service worker). Runs as one local process; Dockerise later.

## Architecture invariants
- Modular monolith, one package per feature: `account`, `creche`, `child`, `attendance`, `billing`, `messaging`, `forms`, `portal`, `shared`.
- Multi-tenant: every business table has `creche_id`, enforced with Hibernate `@TenantId`. No query crosses creches. Tenant/role boundaries covered by automated tests.
- Money stored as integer cents. Invoice calc per spec §6: fee items minus subventions (prorated), grouped into one invoice per family by billing contact.
- Roles: `OWNER`, `MANAGER`, `STAFF`, `PARENT`. Parents see only linked children; staff only their creche.
- Deactivate users, never delete. Audit log on child/invoice/form views and changes.
- Server-rendered first: htmx swaps fragments. No client state management, no SPA, no build step beyond Tailwind CLI.
- Photos and signed PDFs in private storage with short-lived signed URLs.

## Security invariants
- CSRF on; htmx carries the token via meta tag + `hx-headers`.
- Session cookie `HttpOnly`, `SameSite=Lax` (add `Secure` when served over HTTPS). Sessions in-memory for the MVP — a restart logs users out, acceptable for a local demo; upgrade path Spring Session JDBC.
- Magic-link tokens: single use, 10-minute expiry, stored hashed.
- Rate-limit login/magic-link per IP and per email; same response whether the email exists or not.
- Passwords: delegating encoder (bcrypt/Argon2), min length 12.

## Design tokens (docs/design.md)
Canvas `#FAFAF8` · Panel `#FFFFFF` · Ink `#232321` · Muted `#6E6E69` · Line `#ECE9E2` · Active `#EDE8DA` · Mint `#C3E4D3` · Due yellow `#F4D24B` · Paid green `#7ED48E` · Peach `#F0916B`.
Radius 10 (controls) / 14 (cards) / 28 (window). 4px spacing base. 44px touch targets. Sentence case. Summaries `€34,599` no decimals; decimals in row-level amounts. Tabular numerals for totals. Color never the only signal.

## Verification
- Integration tests against SQLite (`jdbc:sqlite::memory:`); Spring Security test support for role checks.
- Playwright E2E on phone viewport: sign up, invite parent, check in child, send invoice, sign form.
- Non-trivial logic ships with a runnable check.

## Commands
- Build + test: `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew test`
- Run: same env, `./gradlew bootRun` → http://localhost:8080 (DB: `grove.db` in repo root)
- Dependency freshness check: `./gradlew dependencyUpdates`
- Native image build: `./gradlew nativeCompile`
- CSS: `bin/tailwindcss -i src/main/resources/static/css/input.css -o src/main/resources/static/css/app.css --minify` (`--watch` during template work)
- Stop the app by port: `lsof -ti:8080 | xargs kill -9` (pattern-kills leave orphaned java processes)
- Prod-shaped deploy demo: `docker compose up --build` → http://localhost:8080 via nginx (app + Postgres, `prod` profile + `db/migration-postgres`; stop bootRun first — port clash)

## Workflow
- Backend chunks (Java, migrations, services, security, tests) go to the `backend-dev` agent (.claude/agents/backend-dev.md, Sonnet) — spawn via Agent tool, one coherent chunk per spawn, each chunk ends `./gradlew test` green.
- UX/template work stays in the main session, then passes the `ux-reviewer` agent gate.
- Agent topology map: docs/agents.md.

## Domain & Local Execution
Irish domain vocabulary: ECCE/NCS subventions, TUSLA inspections, GDPR for children's data.

**Design fidelity:** Every template/fragment/CSS change goes through the `ux-reviewer` agent (`.claude/agents/ux-reviewer.md`) before completion.

**Local execution:** Runs locally on `http://localhost` — passkeys and magic links work directly (localhost is a secure context). Dev mail prints magic links/invites to console.

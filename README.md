# Grove — Creche Management Platform (Proof of Concept)

> [!IMPORTANT]
> **Proof of Concept / Technical Demo**: This repository is a technical demonstration of a multi-tenant childcare management platform built with modern 2026 Spring Boot 4 and Java 25 architecture. It is designed for evaluation and reference, not for direct production deployment without production infrastructure.

Grove demonstrates how to build a high-performance, lightweight, multi-tenant SaaS application with minimal complexity: server-side rendering with Thymeleaf + htmx, modern Spring Security 7 with WebAuthn passkeys and magic links, and Java 25 Virtual Threads.

---

## Technical Stack & Architecture

- **Language & Runtime**: Java 25 LTS with Project Loom Virtual Threads enabled (`spring.threads.virtual.enabled: true`).
- **Framework**: Spring Boot 4.1.x with Spring Security 7, Spring Data JPA, and Flyway database migrations.
- **Build System**: Gradle 9.8 (Kotlin DSL) with GraalVM Native Build Tools (`nativeCompile` ready).
- **Database**: SQLite with Write-Ahead Logging (`PRAGMA journal_mode=WAL`), `PRAGMA busy_timeout=5000`, and tuned HikariCP connection pooling.
- **Authentication**:
  - Form login with BCrypt password hashing (minimum length 12).
  - Passwordless authentication via single-use, hashed one-time tokens (magic links).
  - WebAuthn / FIDO2 passkeys (biometric / hardware key support).
- **Frontend**: Server-driven UI using Thymeleaf, htmx for dynamic fragment swaps, Alpine.js for interactive controls, and Tailwind CSS.
- **Multi-Tenancy**: Tenant isolation enforced at the data layer via Hibernate `@TenantId` on all business entities (`creche_id`).
- **Pipelines & Security**:
  - CI pipeline running automated test suites on Java 25.
  - Automated dependency freshness checks via Ben Manes Versions plugin.
  - Static security analysis via GitHub CodeQL.
  - Container & filesystem vulnerability scanning via Trivy.
  - Automated dependency updates via Dependabot.

---

## Prerequisites

- **Java 25**: OpenJDK 25 or compatible distribution (e.g. Temurin, Oracle JDK).
- **Environment**: Set `JAVA_HOME` pointing to your Java 25 installation:
  ```bash
  export JAVA_HOME=/path/to/openjdk-25
  export PATH="$JAVA_HOME/bin:$PATH"
  ```

---

## Build & Test

```bash
# Run tests (34 unit & integration tests with AOT test processing)
./gradlew test

# Check for latest dependency updates
./gradlew dependencyUpdates

# Build GraalVM native binary (optional, requires GraalVM with native-image)
./gradlew nativeCompile
```

---

## Running the Application

Start the application with Gradle:

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`.

On first startup, Flyway automatically creates and seeds the demo database (`grove.db`) in the root directory.

---

## Sample Logins & Demo Credentials

The database comes pre-seeded with a demonstration creche (**Grove Family Creche**):

### 1. Creche Owner / Manager (Pre-seeded)
- **Login URL**: `http://localhost:8080/login`
- **Email**: `demo@grove.ie`
- **Password**: `grove`

### 2. Passwordless Magic Link Login
- Visit `http://localhost:8080/login/link`
- Enter `demo@grove.ie`
- In development mode, the magic link is logged directly to the server terminal:
  ```
  INFO ... DevMagicLinkSender : Magic link for demo@grove.ie (dev only, not emailed): http://localhost:8080/login/ott?token=...
  ```
- Click or copy the URL to authenticate instantly without a password.

### 3. WebAuthn Passkeys
- After logging in, navigate to `http://localhost:8080/profile`.
- Click **Register Passkey** to add a Touch ID, Face ID, Windows Hello, or YubiKey passkey for one-tap biometric login.

### 4. New Creche Registration
- Visit `http://localhost:8080/signup` to register a brand-new creche and administrative account.

---

## Key Demo Screens

- `/admin/dashboard` — Overview of classrooms, enrollment, and attendance.
- `/admin/fees` — Billing management, ECCE/NCS subventions, and family invoice tracking.
- `/admin/attendance` — Real-time classroom attendance check-in / check-out.
- `/admin/diary` — Daily child activity logs (meals, naps, notes).
- `/portal/today` — Parent portal view showing child activity and updates.

---

## License

This project is licensed under the Apache License 2.0.

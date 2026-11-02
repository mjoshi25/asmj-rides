# ASMJ Ride — Integrated Phases 1–7 + Phase 8 deployment preparation

**Stack:** Java 17, Spring Boot 3.3.5, Thymeleaf, Spring Security, MongoDB Atlas, Docker, Render.

This is a development/testing foundation. Phase 8 adds Render deployment configuration, production-profile settings, an unauthenticated health endpoint for platform checks, safer container execution, and deployment/runbook documentation. It does **not** make the unfinished workflows listed below production-grade.

## Included phases

- **Phase 1:** customer, individual driver and fleet-owner onboarding; password login; development OTP; partner/vehicle admin approval.
- **Phase 2:** immediate/scheduled ride request foundation; vehicle category selection; driver acceptance; fleet assignment; cancellation.
- **Phase 3:** browser GPS while driver page is open; customer location polling; pickup OTP; start/complete trip.
- **Phase 4:** admin-configurable fare inputs, category multipliers, waiting/night/cancellation charges.
- **Phase 5:** cash collection confirmation; UPI reference submission/manual verification; commission ledger; manual settlement/refund records.
- **Phase 6:** admin users, rides, fare rules, payments, operations totals and status management.
- **Phase 7:** ratings, support tickets, SOS/safety and lost-item report records.
- **Phase 8:** Render Blueprint, Docker hardening, production profile, health endpoint and deployment runbook.

## Deploy to Render

### 1. Prepare MongoDB Atlas

1. Create a database user with read/write access to the `asmj_ride` database.
2. Copy the Atlas connection string. Use the database name in the URI, for example `mongodb+srv://.../asmj_ride?retryWrites=true&w=majority`.
3. In Atlas Network Access, allow connections from Render. For a quick initial test, Atlas may require `0.0.0.0/0` when using Render instances without fixed outbound IPs. This permits connection attempts from any IP, so use a strong database password, least-privilege database user, and remove/restrict this rule when you have a fixed-egress/private-network solution.

### 2. Push the project to GitHub

1. Extract this ZIP into a clean folder.
2. Create a new GitHub repository, for example `asmj-ride`.
3. Commit and push the **contents** of the extracted folder (the `pom.xml`, `Dockerfile`, `render.yaml`, and `src` folder should be at repository root).
4. Do not commit `.env` files, passwords, MongoDB URIs, Cloudinary secrets, or real customer data. `.dockerignore` excludes common local secrets and build folders.

### 3. Create the Render service

1. In Render, choose **New → Blueprint** and connect the GitHub repository.
2. Render should detect `render.yaml` and create the `asmj-ride` Docker web service in Singapore.
3. The Blueprint selects the Free plan for initial testing. Free services can spin down after inactivity; the first request after idle may take time. You can change the plan in Render if you need always-on testing.
4. In the service's **Environment** page, set these values:

| Variable | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `MONGODB_URI` | Your complete Atlas URI including `/asmj_ride` |
| `ADMIN_USERNAME` | Admin email, e.g. `admin@yourdomain.com` |
| `ADMIN_PASSWORD` | Unique strong password, minimum 12 characters |
| `OTP_DEV_MODE` | `false` |
| `SESSION_COOKIE_SECURE` | `true` |

`render.yaml` marks the three secrets as `sync: false`, so enter them in Render. Do not put secret values in GitHub or the Blueprint file.

5. Save changes and deploy. Render builds the Docker image using the included Dockerfile. The app listens on the Render-provided `PORT` (default 8080 locally).
6. Wait for the health check at `/actuator/health` to pass. Open the Render service URL over HTTPS.

### 4. First login

- The initial admin is seeded only on application startup when `ADMIN_USERNAME` and `ADMIN_PASSWORD` are present and no user with that email already exists.
- Use the exact admin email and password to sign in.
- Keep `OTP_DEV_MODE=false` in Render. Real SMS/OTP delivery has not been integrated; new users cannot complete the current OTP step in production until an SMS provider and delivery workflow are implemented. For safe initial smoke testing, use a controlled test account that has been verified through a secure, deliberate database/admin process; do not enable public OTP display on the live service.
- Changing `ADMIN_PASSWORD` in Render does not automatically reset an already-created admin account. Use a controlled password-reset procedure before production.

## Local run

Requirements: JDK 17 and Maven 3.9+.

```bash
# Windows PowerShell example
$env:MONGODB_URI = "mongodb://localhost:27017/asmj_ride"
$env:ADMIN_USERNAME = "admin@localhost.test"
$env:ADMIN_PASSWORD = "Use-A-Unique-Password-123!"
$env:OTP_DEV_MODE = "true" # local development only
mvn clean package
mvn spring-boot:run
```

Open `http://localhost:8080`. Local `application.properties` defaults to localhost MongoDB and OTP development mode off; explicitly set `OTP_DEV_MODE=true` only when testing locally.

## Render smoke-test checklist

1. `/actuator/health` returns `{"status":"UP"}`. Do not expose other actuator endpoints.
2. Home page loads over HTTPS and login works.
3. Admin can open `/admin`, `/admin/operations`, `/admin/rides`, `/admin/users`, `/admin/fares`, `/payments`, `/support`.
4. Create test customer/driver/fleet accounts only after OTP delivery is implemented or a controlled test verification is arranged.
5. Approve a test driver and vehicle; create a test ride; accept it; test pickup OTP, trip status and fare.
6. Test cash and manual UPI reference flows using test amounts only; verify the payment status and commission ledger.
7. Test ratings, support ticket, SOS test report, admin response and account suspension.
8. Review Render logs for startup exceptions, MongoDB connection failures, 4xx/5xx errors and unexpected sensitive data. Never share unredacted logs containing credentials or personal information.

## Important limitations — not production-ready

- **OTP:** no SMS provider, delivery, rate limiting or robust brute-force protection. OTP display is only for local development and must remain disabled on Render.
- **Documents:** Cloudinary is present as a dependency but private driver/vehicle document upload, signed access, validation and malware scanning are not implemented.
- **Dispatch/scheduling:** no production proximity/radius dispatch, background scheduled ride reminders, automatic future assignment or concurrency/locking guarantee.
- **GPS/maps:** location works only while the driver's browser page remains open; no background tracking, routing/map matching or independently verified distance calculation.
- **Payments:** UPI is manual reference verification; no payment gateway, QR, screenshot upload, automatic bank reconciliation, actual payout or automatic refund.
- **Safety:** SOS creates an internal report only; it does not call emergency services or notify responders. Immediate danger requires contacting local emergency services directly.
- **Security/operations:** no independent penetration test, rate limits, comprehensive audit/event trail, fraud detection, operational alerting, backup/restore drill or privacy-retention workflow. Review role authorization and CSRF behavior during end-to-end testing.
- **Testing:** a successful Docker build, Maven test suite, Atlas runtime test and full end-to-end test must be completed before public use. Do not use real payment details or identity documents during initial testing.

## Troubleshooting

- **Health check fails:** inspect Render logs; confirm the Docker build completed and that MongoDB is reachable.
- **Mongo timeout/server selection:** verify `MONGODB_URI`, Atlas database user/password, URI-encoded special characters, and Atlas Network Access.
- **Admin login fails:** confirm `ADMIN_USERNAME` is an email and `ADMIN_PASSWORD` is at least 12 characters; if the account already exists, startup seeding will not overwrite its password.
- **OTP cannot be requested:** this is expected when production OTP delivery is not configured. Do not enable development OTP output on the public service.
- **Service sleeps:** the free Render plan may spin down; use a paid always-on instance for continuous testing.

## Rollback

Render keeps prior deploys. If a deployment breaks, use the Render dashboard to roll back to the previous successful deploy, then inspect logs and fix the GitHub commit before redeploying.

# ASMJ Ride — Integrated Phases 1–8 + Cloudinary upload workflows

**Stack:** Java 17, Spring Boot 3.3.5, Thymeleaf, Spring Security, MongoDB Atlas, Docker, Render.

This is a development/testing foundation. Phase 8 adds Render deployment configuration, production-profile settings, an unauthenticated health endpoint for platform checks, safer container execution, and deployment/runbook documentation. It does **not** make the unfinished workflows listed below production-grade.

## Included phases

- **Phase 1:** customer, individual driver and fleet-owner onboarding; password login; MSG91 SMS OTP verification (with local-only development OTP fallback); partner/vehicle admin approval.
- **Phase 2:** immediate/scheduled ride request foundation; vehicle category selection; driver acceptance; fleet assignment; cancellation.
- **Phase 3:** browser GPS while driver page is open; customer location polling; pickup OTP; start/complete trip.
- **Phase 4:** admin-configurable fare inputs, category multipliers, waiting/night/cancellation charges.
- **Phase 5:** cash collection confirmation; UPI reference submission/manual verification; commission ledger; manual settlement/refund records.
- **Phase 6:** admin users, rides, fare rules, payments, operations totals and status management.
- **Phase 7:** ratings, support tickets, SOS/safety and lost-item report records.
- **Phase 8:** Render Blueprint, Docker hardening, production profile, health endpoint and deployment runbook.
- **Cloudinary upload update:** profile photos; partner identity/licence/business documents; multiple vehicle photos with add/remove during edit; RC/insurance/permit/fitness/PUC files; UPI payment proof; support and SOS evidence attachments; admin document review with per-file verification status.

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
| `MSG91_AUTHKEY` | MSG91 account Authkey (secret) |
| `MSG91_OTP_TEMPLATE_ID` | Approved MSG91 OTP template ID containing `##OTP##` |
| `MSG91_COUNTRY_CODE` | `91` for India |
| `SESSION_COOKIE_SECURE` | `true` |
| `CLOUDINARY_CLOUD_NAME` | Your Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | Your Cloudinary API key |
| `CLOUDINARY_API_SECRET` | Your Cloudinary API secret (keep private) |
| `UPLOAD_MAX_BYTES` | `8388608` (8 MB per file) |

`render.yaml` marks credential values as `sync: false`, so enter them in Render. Do not put secret values in GitHub or the Blueprint file.

5. Save changes and deploy. Render builds the Docker image using the included Dockerfile. The app listens on the Render-provided `PORT` (default 8080 locally).
6. Wait for the health check at `/actuator/health` to pass. Open the Render service URL over HTTPS.

### 4. First login

- The initial admin is seeded only on application startup when `ADMIN_USERNAME` and `ADMIN_PASSWORD` are present and no user with that email already exists.
- Use the exact admin email and password to sign in.
- Keep `OTP_DEV_MODE=false` in Render. Configure `MSG91_AUTHKEY` and `MSG91_OTP_TEMPLATE_ID` before accepting registrations. The OTP template must be created in the MSG91 dashboard, contain the `##OTP##` variable, and be approved for Indian SMS delivery (including applicable DLT/sender/template requirements). The app sends a 6-digit OTP with a 5-minute expiry; MSG91 validates the code.
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
4. Configure MSG91 credentials and an approved OTP template; create a test customer, request an OTP, verify it, then sign in. Driver/fleet accounts must still be approved by admin after mobile verification.
5. Set Cloudinary environment variables, then upload a profile photo, partner ID/licence, multiple vehicle images, RC/insurance, UPI proof and support attachments. Confirm each asset appears in the relevant dashboard.
6. Open `/admin/uploads`, verify the required partner and vehicle documents, and confirm approval is blocked before verification and permitted after all required documents are verified.
7. Approve a test driver and vehicle; create a test ride; accept it; test pickup OTP, trip status and fare.
8. Test cash and manual UPI reference flows using test amounts only; verify the payment status and commission ledger.
9. Test ratings, support ticket, SOS test report, admin response and account suspension.
10. Review Render logs for startup exceptions, MongoDB connection failures, 4xx/5xx errors and unexpected sensitive data. Never share unredacted logs containing credentials or personal information.

## Cloudinary upload workflow

- Set all three Cloudinary credentials in Render. The app does not need a local `.env` file.
- Public image uploads are used for profile and vehicle photos. Identity, licence, vehicle documents, payment proofs and support/safety attachments are uploaded as Cloudinary authenticated assets.
- The app checks the logged-in user's ownership or admin role, retrieves authenticated Cloudinary files server-side, and streams them through the protected `/files/...` route with private/no-store headers. The signed Cloudinary URL is not exposed to the browser.
- Allowed formats: JPG, PNG, WEBP and PDF. Maximum file size defaults to 8 MB; support/safety submissions allow up to five attachments each.
- Admin: open `/admin/uploads` to view partner/vehicle documents, payment proofs and attachments. Mark each required document `VERIFIED` before approving the partner or vehicle.
- Driver approval requires verified `IDENTITY` and `DRIVING_LICENCE`. Fleet-owner approval requires verified `IDENTITY` and `BUSINESS_REGISTRATION`. Vehicle approval requires at least one vehicle photo and verified `RC` and `INSURANCE`; `PERMIT`, `FITNESS` and `PUC` can also be uploaded and reviewed as applicable.
- Pending partners can sign in after mobile verification to complete their onboarding uploads. Ride acceptance remains blocked until the partner and vehicle are approved.

## Important limitations — not production-ready
- **OTP:** MSG91 integration is implemented, but it is not live until valid credentials, an approved template, Indian DLT/sender configuration and account balance are set in MSG91. A 30-second resend cooldown is enforced per account; add broader abuse monitoring and CAPTCHA before large-scale public launch. OTP display is only for local development and must remain disabled on Render.
- **Dispatch/scheduling:** no production proximity/radius dispatch, background scheduled ride reminders, automatic future assignment or concurrency/locking guarantee.
- **GPS/maps:** location works only while the driver's browser page remains open; no background tracking, routing/map matching or independently verified distance calculation.
- **Payments:** UPI is manual reference and screenshot submission for admin review; no payment gateway, QR, automatic bank reconciliation, actual payout or automatic refund.
- **Safety:** SOS creates an internal report only; it does not call emergency services or notify responders. Immediate danger requires contacting local emergency services directly.
- **Security/operations:** no independent penetration test, malware scanning, comprehensive audit/event trail, fraud detection, operational alerting, backup/restore drill or privacy-retention workflow. Review role authorization and CSRF behavior during end-to-end testing.
- **Testing:** a successful Docker build, Maven test suite, Atlas runtime test and full end-to-end test must be completed before public use. Do not use real payment details or identity documents during initial testing.

## Troubleshooting

- **Health check fails:** inspect Render logs; confirm the Docker build completed and that MongoDB is reachable.
- **Mongo timeout/server selection:** verify `MONGODB_URI`, Atlas database user/password, URI-encoded special characters, and Atlas Network Access.
- **Admin login fails:** confirm `ADMIN_USERNAME` is an email and `ADMIN_PASSWORD` is at least 12 characters; if the account already exists, startup seeding will not overwrite its password.
- **OTP cannot be requested:** confirm `MSG91_AUTHKEY`, `MSG91_OTP_TEMPLATE_ID`, template approval, DLT registration/sender configuration, account balance and that the mobile number is valid. Check MSG91 OTP logs and Render application logs; do not share the Authkey. Do not enable development OTP output on the public service.
- **Service sleeps:** the free Render plan may spin down; use a paid always-on instance for continuous testing.

## Rollback

Render keeps prior deploys. If a deployment breaks, use the Render dashboard to roll back to the previous successful deploy, then inspect logs and fix the GitHub commit before redeploying.

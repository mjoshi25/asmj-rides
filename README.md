# ASMJ Ride — Integrated Phases 1–8 + Cloudinary upload workflows

**Stack:** Java 17, Spring Boot 3.3.5, Thymeleaf, Spring Security, MongoDB Atlas, Docker, Render.

This is a development/testing foundation. Phase 8 adds Render deployment configuration, production-profile settings, an unauthenticated health endpoint for platform checks, safer container execution, and deployment/runbook documentation. It does **not** make the unfinished workflows listed below production-grade.

## Included phases

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
- Changing `ADMIN_PASSWORD` in Render does not automatically reset an already-created admin account. Use a controlled password-reset procedure before production.

## Local run

Requirements: JDK 17 and Maven 3.9+.

```bash
# Windows PowerShell example
$env:MONGODB_URI = "mongodb://localhost:27017/asmj_ride"
$env:ADMIN_USERNAME = "admin@localhost.test"
$env:ADMIN_PASSWORD = "Use-A-Unique-Password-123!"
mvn clean package
mvn spring-boot:run
```


## Render smoke-test checklist

1. `/actuator/health` returns `{"status":"UP"}`. Do not expose other actuator endpoints.
2. Home page loads over HTTPS and login works.
3. Admin can open `/admin`, `/admin/operations`, `/admin/rides`, `/admin/users`, `/admin/fares`, `/payments`, `/support`.
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
- **Service sleeps:** the free Render plan may spin down; use a paid always-on instance for continuous testing.

## Rollback

Render keeps prior deploys. If a deployment breaks, use the Render dashboard to roll back to the previous successful deploy, then inspect logs and fix the GitHub commit before redeploying.

## Manual mobile verification

ASMJ Ride uses an administrator-issued one-time security code instead of an SMS gateway. In the admin console, open **Mobile verification**, review the registered account details, generate a code, and share it manually with the account holder after checking their identity. The six-digit code expires after 24 hours, allows up to five incorrect attempts, is stored only as a BCrypt hash, and is displayed to the administrator once. The user enters their registered email and code at `/verify-mobile`. No Fast2SMS, MSG91, API key, DLT registration, or OTP template is required.

The admin verification page records account name, email, mobile, role, account status, registration time, code issue time, issuing administrator, expiry, failed attempts, and verification time/method. Never share the code in a public channel.

## Support chat update
See [SUPPORT-CHAT-IMPLEMENTATION.md](SUPPORT-CHAT-IMPLEMENTATION.md) for the admin support conversation workflow, mandatory mobile number, optional private attachments, role-based access, and deployment notes.

## Phases 5–8 update
See `PHASE-5-8-IMPLEMENTATION.md` for fleet-owner dashboard and trip assignment, admin trip status/audit controls, round-trip payment allocation and independent settlements, analytics filters, and CSV export.


## Release update: route estimates, vehicle search, notifications and document review

### Configure route distances before taking bookings
The application intentionally does not guess road distances from city names. Admin users should open **Admin → Locations** and add an estimated one-way road distance for each supported city pair. You may enter optional source/destination PIN codes for a more specific route; a city-to-city route without PIN codes acts as the fallback. Reverse-direction estimates are also supported. If no route is configured, the customer can enter an approximate one-way distance and the fare preview will clearly identify it as manual.

The customer booking form previews total distance and estimated fare (including round-trip distance) before submission and requires the customer to confirm the exact displayed estimate. The server recalculates the fare at submission and rejects a stale/mismatched accepted amount; the customer must refresh and agree again if fare rules changed. The final fare remains based on actual trip data.

### Vehicle search and request notifications
Customers can search only approved, active vehicles whose configured pickup city/PIN, drop-off city/PIN, explicit route pair, journey type and booking time match the request. Vehicles without configured coverage are intentionally excluded until a fleet owner or admin configures and approves their coverage. Availability checks include vehicle overlaps and approved, mobile-verified, available drivers with no overlapping outward or return assignment. Until a route-duration/ETA field is configured, each leg uses a four-hour availability window plus a one-hour turnaround buffer; this is a fallback, not live traffic ETA, and should be tuned for long-distance routes. A selected vehicle is attached to the request; the driver or fleet owner must still accept/assign it. MongoDB-backed short-lived locks serialize vehicle, driver and ride assignment operations to reduce double-booking races. Fleet owners see open requests matching their approved fleet categories and route coverage. New ride requests generate in-app notifications for admins, customers and relevant vehicle owners/drivers; assignment generates a customer notification. Open **Notifications** from the role dashboard to review them. These are in-app notifications, not SMS or email.

### Document review
Admin → Documents now has individual approve/reject/pending controls for partner documents, vehicle documents and vehicle photos. Rejection requires a review note, which is shown to the partner. Overall vehicle approval requires at least one verified photo plus verified RC and insurance documents.

## Demo fleet and booking search

For the optional staging-only demo dataset (2 fleet owners, 4 drivers, 3 customers and 5 approved vehicles with fictional watermarked documents), see [DEMO-DATA-SETUP.md](DEMO-DATA-SETUP.md). Demo seeding is disabled by default and requires `DEMO_SEED_ENABLED=true` plus a strong `DEMO_SEED_PASSWORD`.

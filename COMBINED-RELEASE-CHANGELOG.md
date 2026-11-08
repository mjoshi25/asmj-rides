# ASMJ Ride — Combined release changes

This release is based on the earlier Phase 5–8 / Next Release project archive and retains its existing Spring Boot, Thymeleaf, MongoDB, Cloudinary, manual mobile-verification, payments, support, and round-trip flows.

## Vehicle service coverage

- Vehicles now persist multiple permitted pickup city/PIN combinations.
- Vehicles now persist multiple permitted drop-off city/PIN combinations.
- Vehicle owners can explicitly configure permitted city-to-city route pairs.
- One-way and round-trip support are separate vehicle settings. Round-trip search requires the reverse route and both endpoints to be configured for pickup and drop-off.
- Fleet owners can configure coverage while adding/editing a vehicle. Admins can configure it from Admin → Uploads → vehicle coverage.
- Changing coverage returns the vehicle to PENDING for re-approval.

## Customer vehicle search and booking

Search requires category, pickup city/PIN, destination city/PIN, journey type and travel time. Results exclude vehicles that are not approved, operationally active, photo/document verified, route-covered, owner-approved/mobile-verified, or free for the selected journey window. Assigned-driver availability is checked; fleet-owner vehicles with no free assigned driver can still be shown as awaiting fleet-owner driver assignment.

The booking endpoint revalidates the selected vehicle. A MongoDB per-vehicle lock is acquired during customer booking and driver/fleet/admin assignment, then availability is rechecked while the lock is held. The saved requested vehicle ID is retained so later assignment must use the vehicle the customer selected.

## Demo seed

Optional seed creates 2 fleet owners, 4 drivers, 3 customers and 5 approved vehicles, with verified fictional sample partner documents, vehicle illustrations, RC/insurance sample PDFs, service locations and clearly labelled sample route distances. Demo seeding is disabled by default. It requires `DEMO_SEED_ENABLED=true` and `DEMO_SEED_PASSWORD` (at least 12 characters). Never enable it on the production database.

All sample PDFs are watermarked and explicitly state that they are not valid government, identity, registration, licence or insurance documents. Sample road distances are approximate placeholders and must be replaced with verified values before production use.

## Admin review

The previous release's individual partner-document and vehicle-photo/document review controls are retained. Rejection requires an admin reason. Vehicle approval requires a verified photo, verified RC and insurance, and configured route coverage.

## Validation performed for this archive

- ZIP integrity check
- POM XML and Render YAML parsing
- HTML parse for all templates, viewport declarations and nested forms
- Inline JavaScript syntax checks with Node
- Java delimiter scan and compiler parser scan for syntax diagnostics
- Demo PDF signatures, SVG XML and seed asset-reference checks

A full Maven build, MongoDB integration test and live Render/browser test have not been run in this environment. Render's Docker build stage runs Maven during deployment.

## Fleet driver management and driver-confirmed dispatch (2026-11-08)
- Added fleet-owner self-driver mode using the existing fleet-owner account. Enabling it requires an approved account, verified mobile and an admin-verified driving licence; the fleet owner can use their own approved fleet vehicles from the driver dashboard.
- Added fleet-owner creation of driver accounts. Created drivers remain unavailable until manual mobile verification, required document verification and admin approval are complete.
- Added mobile-number search for existing approved, mobile-verified, unlinked platform drivers. Fleet owners can request association; the driver must explicitly accept or decline before being linked.
- Added mobile-number filtering to outward and return driver dropdowns and displays driver mobile numbers in options.
- Fleet-owner outward ride assignment now enters ASSIGNED and notifies the driver. The driver must accept or decline; customer assignment notification is sent only after acceptance. Declining releases the assignment back to REQUESTED.
- Added starting and ending odometer capture for outward and return legs. Actual distance is derived from ending minus starting KM and is used for fare calculation.
- Added assignment-response authorization for drivers and enabled fleet-owner driver mode routes only for fleet owners who explicitly enabled the mode.
- Applied the same explicit accept/decline confirmation to fleet-owner return-leg assignments; declined return assignments are released for reassignment.

## Admin-configurable document requirements (November 2026)
- Added a MongoDB-backed document requirement configuration for drivers, fleet owners, customers and vehicles.
- Added `/admin/document-requirements` with separate save controls for each role/category.
- Approval checks now use configured mandatory documents and require their verification; optional documents do not block approval.
- Vehicle search and fleet assignment document checks now use the same vehicle requirements, while keeping the existing verified vehicle-photo requirement intact.
- Customer document upload is available for identity and address proof when the administrator enables those requirements.
- Rejected optional documents do not automatically reset account/vehicle approval; rejected mandatory documents do.
- Defaults preserve the existing rules: driver identity + driving licence; fleet-owner identity + business registration; vehicle photo + RC + insurance; no mandatory customer documents.
- Existing approved accounts are not automatically downgraded when an administrator changes requirements; new approval decisions enforce the current configuration.

## Document Master enhancement (2026-11-08)
- Added configurable document definitions for DRIVER, FLEET_OWNER, CUSTOMER and VEHICLE.
- Admin can add custom document types, edit display names/descriptions, set mandatory/optional and expiry-date rules, enable/disable types, and delete custom types.
- Built-in document codes are protected from permanent deletion; disable them instead.
- Upload dropdowns for partner, customer and vehicle documents now use active configured document definitions.
- Server-side upload validation rejects inactive/unknown document types and enforces configured future expiry dates.
- Approval continues to require verification only for active mandatory document types; vehicle photo verification remains mandatory.
- Existing legacy mandatoryByRole configuration is migrated into document definitions on first load.
- Document requirement updates retain the latest 100 admin change entries.
- Existing uploaded document metadata is retained when a custom type is deleted.

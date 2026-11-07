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

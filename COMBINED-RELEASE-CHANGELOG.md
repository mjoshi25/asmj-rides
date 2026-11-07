# ASMJ Ride — Combined release changes

This release is based on the earlier Phase 5–8 / Next Release project archive and retains its existing Spring Boot, Thymeleaf, MongoDB, Cloudinary, manual mobile-verification, payments, support, and round-trip flows.

## Vehicle service coverage

- Vehicles now persist multiple permitted pickup city/PIN combinations.
- Vehicles now persist multiple permitted drop-off city/PIN combinations.
- Vehicle owners can explicitly configure permitted city-to-city route pairs.
- One-way and round-trip support are separate vehicle settings. Round-trip search requires the reverse route and both endpoints to be configured for pickup and drop-off.
- Fleet owners can configure coverage while adding/editing a vehicle. Admins can configure it from Admin → Uploads → vehicle coverage.
- Changing coverage returns the vehicle to PENDING for re-approval. Round-trip configuration is rejected unless each permitted route has its reverse route and both endpoints are present in pickup and drop-off coverage.

## Customer vehicle search and booking

Search requires category, pickup city/PIN, destination city/PIN, journey type and travel time. Results exclude vehicles that are not approved, operationally active, photo/document verified, route-covered, owner-approved/mobile-verified, or free for the selected journey window. A fleet-owned vehicle is shown only when at least one approved, mobile-verified fleet driver is available for that journey window; if the driver is not pre-assigned to that vehicle, the result clearly indicates that the fleet owner must confirm the driver.

The booking endpoint revalidates the selected vehicle. The customer must accept the displayed estimate, and the submitted accepted amount is checked against a fresh server-side fare calculation. MongoDB-backed ride, vehicle and driver locks serialize customer booking and driver/fleet/admin assignment. Availability, approval, route coverage and assignment state are rechecked while the relevant locks are held. Driver availability considers overlapping outward and return legs, not only a simple online toggle. The current fallback reserves four hours per leg plus a one-hour turnaround buffer; it is not live traffic ETA and should be tuned for long-distance routes. Lock records are TTL-cleaned after release/expiry. The saved requested vehicle ID is retained so later assignment must use the vehicle the customer selected.

## Demo seed

Optional seed creates 2 fleet owners, 4 drivers, 3 customers and 5 approved vehicles, with verified fictional sample partner documents, vehicle illustrations, RC/insurance sample PDFs, service locations and clearly labelled sample route distances. Demo seeding is disabled by default. It requires `DEMO_SEED_ENABLED=true` and `DEMO_SEED_PASSWORD` (at least 12 characters), and the seeder is excluded under the `prod` and `production` Spring profiles. Render also explicitly sets the flag to false. Never enable it on the production database.

All sample PDFs are watermarked and explicitly state that they are not valid government, identity, registration, licence or insurance documents. Sample road distances are approximate placeholders and must be replaced with verified values before production use.

## Admin review

The previous release's individual partner-document and vehicle-photo/document review controls are retained. Rejection requires an admin reason. Vehicle approval requires a verified photo, verified RC and insurance, and configured route coverage.

## Mobile usability

The responsive layout and mobile navigation toggle are present across all Thymeleaf pages, including the admin locations and notifications pages. Forms, review controls, multi-select coverage inputs, tables and booking cards are sized to fit narrow screens.

## Validation performed for this archive

- Original and Next Release ZIP archives were checked; the working source was extracted and compared.
- POM XML and Render YAML parsed successfully.
- All Thymeleaf templates were checked for viewport metadata, navigation toggle/script coverage and nested forms.
- All inline JavaScript blocks passed `node --check`.
- Java compiler parser scan reported no Java syntax diagnostics. A full type-check could not complete because Spring/Maven dependencies are not present locally.
- Demo PDF signatures/text, SVG XML and referenced seed asset files were checked.
- Final release ZIP passed `unzip -t` integrity validation.

A full Maven build, MongoDB integration test and live Render/browser test have not been run in this environment: Maven and Docker are not installed, and outbound package access is unavailable. Render's Docker build stage uses Maven 3.9.9 with Java 17 and will perform the actual Maven package step during deployment.

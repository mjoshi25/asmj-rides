# Phase 19 — Admin mobility category configuration

## Delivered
- Added `/admin/mobility-config`, protected by the existing `/admin/**` ADMIN role rule.
- Admin can create/edit/activate/deactivate vehicle categories.
- Category code is normalized to uppercase, unique, validated, and read-only on edits to protect existing references.
- Categories support `PASSENGER`, `GOODS`, and `BOTH`, passenger capacity, maximum payload in kg, display order, description, and active state.
- Existing `supportedRentalModes` are preserved when editing a category; this phase does not alter rental eligibility.
- Admin can enable/disable passenger and goods service switches.
- Admin can configure a default commission and per-category commission percentages (0–50%). The saved platform policy remains `CATEGORY`.
- Admin can configure allowed active categories per service city. Old city records with no explicit category selection retain unrestricted-by-category behavior. A saved empty selection means no category is enabled for that city.
- Added an Admin Control Center link to the new configuration page.
- Seeder assigns GOODS service type to newly created GOODS, HEAVY_COMMERCIAL, PICKUP, and TRUCK categories; newly seeded passenger categories receive PASSENGER. Seeder does not overwrite existing Admin edits.

## Compatibility and safety
- Existing category codes are not renamed or deleted.
- Existing city documents without the new fields remain readable. `categorySelectionConfigured=false` preserves the prior category behavior.
- Existing ride documents continue to default to PASSENGER when `serviceType` is absent.
- Existing rental models, rental collections, rental controllers, and rental pricing are not modified by this phase.
- City/category settings and commission settings are configuration data only. They are **not yet enforced by customer category discovery, booking validation, fare calculation, dispatch, or payment settlement**; those integrations must be implemented and tested in the subsequent booking/dispatch/commission phases before production launch.
- The Phase 18 commission values are placeholders. Admin must enter approved commercial rates. No commission is charged or calculated by this Admin page.

## Validation performed
- Source-level review of existing Admin controller, security, category seeder, city model/repository, and platform config patterns.
- XML/POM and template structural checks are performed during packaging.
- Maven compile/tests and MongoDB/browser integration tests could not be run in this environment because Maven is not installed and no connected test database/browser session is available.

## Manual verification after download
1. Run `mvn clean test` in the project root.
2. Start the app with a test MongoDB database and sign in as an Admin.
3. Open Admin Control Center → Passenger & goods categories / commission.
4. Create a unique test category, edit it, deactivate/reactivate it, and verify it persists after restart.
5. Set different commission rates for two categories and a fallback rate; save and reopen the page.
6. Toggle passenger/goods service switches and verify persistence.
7. Select categories for one test city; save an empty selection and a non-empty selection; verify `categorySelectionConfigured` and `enabledCategoryCodes` in the test database.
8. Verify legacy city documents without those fields still load, and test existing rental search/booking flows unchanged.

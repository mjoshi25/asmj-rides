# ASMJ Ride demo data setup

Demo data is **disabled by default**. Enable it only on a non-production/staging database.

## Render environment variables

Set these values on the staging service:

- `DEMO_SEED_ENABLED=true`
- `DEMO_SEED_PASSWORD` = a strong password of at least 12 characters, chosen by the administrator

Restart/deploy the service. The seed is idempotent: existing demo email accounts and vehicle registration numbers are not overwritten. After the initial seed, set `DEMO_SEED_ENABLED=false` and redeploy. Disabling the seeder does not delete already-created demo records; remove them manually from the test database when finished.

## Demo accounts

New demo accounts use the password supplied in `DEMO_SEED_PASSWORD`. Existing demo accounts are not password-reset on later runs; use the password originally set when those accounts were first created.

| Role | Email |
|---|---|
| Fleet owner 1 | `demo.fleetowner1@example.invalid` |
| Fleet owner 2 | `demo.fleetowner2@example.invalid` |
| Driver 1 | `demo.driver1@example.invalid` |
| Driver 2 | `demo.driver2@example.invalid` |
| Driver 3 | `demo.driver3@example.invalid` |
| Driver 4 | `demo.driver4@example.invalid` |
| Customer 1 | `demo.customer1@example.invalid` |
| Customer 2 | `demo.customer2@example.invalid` |
| Customer 3 | `demo.customer3@example.invalid` |

The seeder marks only these fictional test accounts as mobile-verified and approved. It does not create or change the administrator account.

## Sample vehicles

Five vehicles are seeded with approved status, active operational status, a fictional SVG vehicle illustration, and verified sample RC/insurance PDFs. Sample route coverage is explicitly configured for selected pickup/drop city pairs and reverse routes for round-trip tests.

The sample PDFs are watermarked **SAMPLE — NOT VALID** and contain fictional data. They are not real identity, driving licence, registration, permit or insurance documents.

## Demo route distances

The route distances seeded for demonstration are approximate sample values, visibly tagged in the admin location page as requiring verification. Replace them with current, verified road distances before production bookings rely on them.

## Suggested test sequence

1. Sign in as a fleet owner and inspect vehicle service coverage and documents.
2. Sign in as a customer and search a configured route such as Bhawanipatna (766001) to Brahmapur (760004) with category CAR.
3. Confirm the estimate, select one of the returned vehicles and submit a booking.
4. Check the fleet-owner/admin in-app notifications.
5. Try searching the same vehicle/time again; it should not appear while the earlier booking remains active/requested.
6. Try a route not included in that vehicle's route list; it should not appear.
7. Try round-trip search; the vehicle must have both outward and reverse route permissions.

Do not use these sample accounts or documents for live customer operations.

## Seeded vehicle registrations and initial route coverage

| Registration | Category | Initial permitted route pairs |
|---|---|---|
| `OD08DEMO001` | CAR | Bhawanipatna ↔ Brahmapur; Bhawanipatna ↔ Bhubaneswar |
| `OD08DEMO002` | CAR | Bhawanipatna ↔ Brahmapur; Kesinga ↔ Sambalpur |
| `OD08DEMO003` | CAR | Junagarh ↔ Bhubaneswar; Bhawanipatna ↔ Sambalpur |
| `OD08DEMO004` | AUTO | Bhawanipatna ↔ Kesinga |
| `OD08DEMO005` | BIKE | Junagarh ↔ Kesinga; Bhawanipatna ↔ Kesinga |

The arrows indicate that both outward and reverse route pairs are configured. All sample vehicles have multiple pickup and drop city/PIN entries, but the explicit route-pair list is what determines which combinations are searchable.

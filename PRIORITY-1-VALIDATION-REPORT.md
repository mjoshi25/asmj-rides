# ASMJ Ride – Priority 1 Release Report

Baseline: `ASMJ-Ride-Admin-Document-Requirements-Release.zip` (uploaded 8 Nov 2026)

## Implemented in this release

- Dynamic Document Master for DRIVER, FLEET_OWNER, CUSTOMER and VEHICLE.
- Admin can add custom document types, edit name/description, set mandatory or optional, configure expiry-date requirement, enable/disable and delete custom types.
- Built-in document codes cannot be permanently deleted; they can be made optional and disabled.
- Upload dropdowns use active definitions for partner, customer and vehicle document uploads.
- Backend rejects unknown/inactive document types and validates configured expiry dates.
- Approval checks require verified active mandatory documents; expired documents do not satisfy approval. Vehicle photo verification remains mandatory.
- Existing `mandatoryByRole` configuration is migrated to the new definitions on first access.
- Admin changes retain up to 100 audit entries.

## Existing Priority 1 features found in the supplied baseline (source inspection only)

- Fleet owner self-driver mode and existing account reuse.
- Fleet owner can create driver accounts; new accounts remain pending until manual mobile verification, document verification and admin approval.
- Existing registered driver search by mobile number and fleet association request/driver response.
- Fleet owner assignment dropdown includes mobile numbers and client-side mobile filtering.
- Fleet ride assignment uses `ASSIGNED` state and driver accept/decline response endpoints; outward and return assignment paths exist.
- Driver journey flow records starting and ending odometer values and calculates actual distance/final fare. Return-leg odometer fields and fare calculation also exist.
- Vehicle availability diagnostics, route/coverage checks, vehicle reservation lock service, payment/settlement models, notification service and Cloudinary upload service exist in the baseline.

## Validation actually performed

- `pom.xml` parsed successfully.
- All 26 Thymeleaf templates passed a basic tag nesting/balance scan.
- 50 Java source files passed a basic brace-balance scan.
- Static assertions confirmed document-master CRUD routes and service methods, dynamic upload allow-lists and approval/expiry checks.
- ZIP archive integrity test passed (172 entries).

## Not performed

- Maven compilation or test execution: Maven and Maven Wrapper are not present in this environment.
- Spring application startup, MongoDB integration, Cloudinary integration, browser testing, Render deployment or end-to-end ride tests.

## Recommended pre-production test sequence

1. Admin adds a custom optional document for each account type; verify it appears in the relevant upload dropdown.
2. Change it to mandatory; verify an unverified/missing upload blocks approval.
3. Verify it, then approve the account; confirm optional documents do not block approval.
4. Configure expiry-date requirement; try upload without a date, with a past date, and with a future date.
5. Disable a custom optional document; confirm it disappears from new-upload dropdowns while old uploaded records remain visible.
6. Delete a custom optional document; confirm historical uploaded asset metadata remains intact.
7. Re-test fleet driver create/search/association, mobile filtering, ride accept/decline, Start KM, End KM, return leg, fare, payment proof and notifications.
8. Re-test customer vehicle search and route diagnostics, vehicle document approval and mobile layouts.

## Known considerations

- Built-in document codes are intentionally not permanently deletable to avoid breaking historical asset categories and existing approval logic. They can be made optional and disabled.
- Expiry dates are stored as ISO date strings in uploaded asset metadata.
- Vehicle photo verification remains a fixed safety requirement, separate from configurable vehicle document types.
- Route distance text in the existing customer page states that estimates depend on configured route distances; a live maps provider is not included in this release.

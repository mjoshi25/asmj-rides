# Phase 13 – Admin Control Center Redesign

## Scope

Redesigned the existing `/admin` dashboard presentation using the Phase 12 project as the baseline. No controller, repository, entity, security rule, approval workflow, or business operation was intentionally changed.

## UI changes

- Replaced the single long row of oversized action buttons with a responsive Admin Control Center.
- Added four quick-access cards for ride operations, accounts, document review, and analytics.
- Grouped existing management routes into six categories: Locations & Routes; Ride Operations; Accounts & Partners; Finance & Payments; Verification & Compliance; Safety, Reviews & Support.
- Moved partner applications, travel agency profiles, and vehicle applications into three native collapsible sections.
- Kept existing Thymeleaf data bindings, CSRF tokens, form actions, status options, and empty-state messages.
- Added responsive desktop/tablet/mobile styling scoped to `body.page-admin`.
- Updated CSS and JavaScript cache-busting version strings to Phase 13.

## Existing routes linked

`/admin/locations`, `/admin/vehicle-availability`, `/admin/rides`, `/admin/fares`, `/admin/operations`, `/admin/users`, `/admin/role-requests`, `/admin/mobile-verifications`, `/admin/uploads`, `/admin/document-requirements`, `/admin/contact-changes`, `/admin/safety`, `/admin/reviews`, `/payments`, and `/support`.

## Deliberate limitation

No Admin-only Partner Vehicle Access monitoring route exists in the Phase 12 controller mappings. This phase does not add a non-functional link or imply that Admin can override owner-authorized permissions. Existing owner/agent authorization workflows remain unchanged. A separate secured Admin monitoring page can be implemented in a future phase if required.

## Verification

- Parsed the updated Admin template with BeautifulSoup.
- Confirmed one Admin main container, six category cards, four quick-access cards, and three collapsible record sections.
- Confirmed all anchors have href values.
- Checked CSS brace balance.
- Confirmed route strings exist in the project source for all linked `/admin/...` pages.
- ZIP archive integrity checked after packaging.
- Full Maven build, MongoDB integration, and browser/device testing were not performed in this environment.

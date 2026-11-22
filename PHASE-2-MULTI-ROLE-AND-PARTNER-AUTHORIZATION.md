# ASMJ Vehicle Rental Marketplace – Phase 2

## Implemented in this update

### Multi-role accounts
- Added `roleStatuses` to `UserAccount` for approval state per additional role.
- Kept the legacy primary `role` field and legacy role access behavior intact.
- Added `getGrantedRoles()` so additional roles are not exposed as Spring Security authorities until approved.
- Added `/account` for account overview, role requests, role-specific document upload and dashboard-role selection.
- Added `accountRoleRequests` MongoDB collection and Admin review at `/admin/role-requests`.
- Added a regression test for pending-versus-approved additional role authority.
- Updated key fleet, ride, payment, support and asset checks to recognize additional granted roles.
- A newly approved role requires sign-out/sign-in so Spring Security refreshes the session authorities.

### Owner–travel agent vehicle authorization
- Added agent request workflow for approved active partner-owned vehicles.
- Owner can approve with selected permissions, reject, or revoke an approved authorization.
- Authorization is bound to owner, agent and vehicle IDs.
- Server-side service validates agent profile approval, owner/vehicle eligibility and permission allow-list.
- `MANAGE_LISTING` authorization is enforced in existing vehicle update and document-upload operations for partner-owned vehicles.
- Permissions include view listing, manage listing, enquiries, bookings, pricing, availability and driver assignment. Not every permission is wired to a corresponding operational feature yet; later booking/pricing milestones must call the same authorization service before acting.

## Routes
- `/account`
- `/admin/role-requests`
- `/agent/authorizations`
- `/fleet/authorizations`

## Remaining work
- Complete permission enforcement in each future booking, pricing, availability and driver-assignment endpoint.
- Build agent's partner vehicle management UI using only approved authorization records.
- Complete admin role removal/revocation and account audit history.
- Add transactional notifications for role decisions and partner-authorization events.
- Full Maven build, MongoDB integration and browser workflow tests must be run in the developer environment.

## Safety and compatibility
- Existing ride booking collection and fields are not renamed or deleted.
- Existing primary role remains unchanged when another role is approved.
- Additional roles are only added by Admin approval; self-service requests do not grant authority.
- Vehicle owner approval is required before an agent receives the permissions selected by the owner.
- Production deployment is not authorized by this update.

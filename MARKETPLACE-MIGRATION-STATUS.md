# ASMJ Vehicle Rental Marketplace – Repository Enhancement Status

This branch is based on the supplied `asmj-rides` source archive. Existing ride workflows and legacy MongoDB fields are retained. Do not deploy this branch to the production Render service until regression and migration checks have passed.

## Incorporated in this working update

- Added a backward-compatible `roles` list to `UserAccount`; the existing primary `role` field remains intact for legacy records and existing controller logic.
- Added `VEHICLE_OWNER` as a registration alias mapped to the existing `FLEET_OWNER` authority. Existing fleet-owner accounts continue to work.
- Added `TRAVEL_AGENT` registration, Spring Security authority, protected `/agent` entry point, and partner onboarding view.
- Added travel-agent document requirements for identity and business registration.
- Added a travel-agency profile with own-fleet and partner-fleet operating preferences. Both can be selected.
- Added the `travelAgentProfiles` collection model/repository.
- Added `agentVehicleAuthorizations` model/repository with owner, agent, vehicle, permission, status and decision fields; Phase 2 adds owner approval screens and listing-management enforcement.
- Added `rentalBookings` model/repository, separate from legacy `rides`, to avoid changing existing ride records.
- Added rental-mode flags to vehicles. Existing vehicles default to with-driver support and self-drive disabled.
- Added dynamic vehicle-category model/repository and idempotent starter category seeding, preserving existing `CAR`, `AUTO`, `BIKE`, `OTHER` codes.
- Added rental pricing rule model/repository.
- Added JUnit/Spring Boot test dependency and role compatibility tests.

## Phase 2 incorporated

- Multi-role account page, additional role requests and Admin approval workflow.
- Role-specific approval status and Spring Security authority gating for additional roles.
- Role-specific document uploads for pending role requests.
- Dashboard role selection; users must sign out and back in after Admin approval to refresh authorities.
- Owner-to-agent partner vehicle authorization request, owner approval/rejection and revocation.
- Permission allow-list and server-side listing-update/document-upload checks for authorized agents.
- See `PHASE-2-MULTI-ROLE-AND-PARTNER-AUTHORIZATION.md` for route and implementation details.

## Not yet implemented

- Full partner vehicle management experience for agents across every module; authorization enforcement must be wired into future booking, pricing, availability and driver-assignment endpoints.
- Dynamic category administration and conversion of all existing category dropdowns/validators to database-driven categories.
- Public vehicle marketplace search and listing pages.
- Rental booking creation, quotation, acceptance and concurrency-safe date-range reservation service.
- Self-drive eligibility, licence verification, rental agreement, deposit, handover, return and damage workflow.
- Rental payment, refund, owner settlement, agent commission and configurable platform revenue execution.
- Paid advertisements, scheduling, moderation, reviews and booking-linked chat.

## Compatibility and rollout notes

- Legacy `role` values remain readable; `getRoles()` returns the primary role even when the new `roles` array is absent in older MongoDB documents.
- The new collections are additive. No existing `rides`, `users`, `vehicles`, or payment records are deleted or renamed.
- The category seeder only inserts missing codes; it does not overwrite Admin edits.
- New rental records must use `rentalBookings`; do not repurpose the existing `rides` collection.
- Run `mvn clean test` and manually test legacy login, customer booking, driver acceptance, fleet assignment, payment, document review and mobile verification before any deployment.

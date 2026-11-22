# Phase 16A – Guided vehicle setup and availability

## Goal
Make the Vehicle Owner and Travel Agent vehicle-management workflow easier to understand and safer to operate. This milestone is the first part of the rental marketplace workflow; it does not claim that customer rental booking, driver journey execution, invoicing, payment verification or reviews are complete.

## Changes
- Replaced the difficult multi-select listboxes for pickup locations, drop-off locations and city-to-city routes with collapsible, searchable-by-browser checkbox panels. Existing request parameter names and server-side coverage validation are retained.
- Added explicit rental-mode choices: with-driver and self-drive. At least one must be selected; values are persisted on the vehicle.
- Added per-vehicle availability controls: Available for booking, Under maintenance, Temporarily unavailable, plus an optional note.
- Availability changes have a dedicated endpoint and do not reset the vehicle's admin approval status.
- Travel Agents see their own vehicles plus partner vehicles for which they have an active `VIEW_LISTING` authorization.
- Travel Agents can assign a driver to an owner-authorized partner vehicle only when the owner granted `ASSIGN_DRIVER`. Owners can assign drivers to their own vehicles. Existing driver approval and mobile-verification checks remain required.
- Partner vehicle availability changes require `MANAGE_AVAILABILITY` when the vehicle belongs to another owner.
- Responsive styling improves readability of the guided setup, route selectors and availability controls.

## Preserved
- Legacy `RideBooking` workflows and existing ride data.
- Existing vehicle approval process and document verification.
- Owner-controlled partner authorization and its permission allow-list.
- Existing route coverage validation and allowed route semantics.
- Existing direct-to-provider rental payment decision: customer pays Vehicle Owner / Travel Agent directly by UPI, cash or bank transfer. Payment collection and verification are a later milestone.

## Next milestones
- 16B: customer route/date/mode search, eligible vehicle results and rental booking request.
- 16C: owner/agent booking acceptance, quoted fare, vehicle confirmation and driver assignment/notifications.
- 16D: driver assignment acceptance, journey start/end, odometer capture and completion.
- 16E: invoice, direct payment reporting/verification, disputes, receipts and reviews/ratings.

## Validation performed
- HTML structural parse and expected form-control checks.
- CSS brace-balance check.
- Source-level checks for the dedicated availability endpoint and authorization permission checks.
- ZIP integrity check.

Full Maven compilation, MongoDB integration tests, Thymeleaf runtime rendering and authenticated browser/device tests are still required. Do not deploy to production until those pass.

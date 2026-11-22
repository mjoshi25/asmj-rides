# ASMJ Rides Phase 16B–16E

This development release implements the next rental marketplace workflow milestones on top of Phase 16A.

Read `PHASE-16B-16E-RENTAL-WORKFLOW.md` for detailed scope, lifecycle statuses, direct-payment rules and limitations.

## Main routes
- Customer: `/rentals/search`, `/rentals/my-bookings`
- Vehicle Owner / Travel Agent: `/rentals/provider`
- Driver: `/rentals/driver`

## Direct payment model
Customers pay the Vehicle Owner / Travel Agent directly by UPI, cash or bank transfer. Provider payment instructions are configured on the vehicle and snapshotted when a booking is accepted. Customer payment reporting does not mark payment verified. The provider must verify receipt.

## Local test
1. Extract this ZIP into a new development folder; keep Phase 16A and Phase 15 backups.
2. Configure the existing MongoDB Atlas and Cloudinary environment values as documented in `README.md`.
3. Run `mvn clean test` and fix any environment-specific failures before browser testing.
4. Run `mvn spring-boot:run`.
5. Follow the manual test plan in `PHASE-16B-16E-RENTAL-WORKFLOW.md`.

Do not deploy to Render until full Maven tests, MongoDB tests and role-by-role browser/mobile tests pass and the owner explicitly approves deployment.

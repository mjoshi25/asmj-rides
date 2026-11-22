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

## Phase 16F – Provider discounts and vouchers

- Fleet Owners and Travel Agents can publish provider-managed voucher codes at `/rentals/vouchers`; no Admin approval workflow is used.
- Vouchers support percentage/fixed discounts, minimum booking amount, maximum discount cap, start/expiry, global/per-customer usage limits, and optional vehicle scope.
- Publishing sends in-app notifications to approved customers. The customer search results show matching active vouchers; customers can enter a code in the booking request.
- Voucher eligibility is rechecked server-side at booking request and provider acceptance. The accepted booking snapshots original quote, voucher code, and discount. Provider may also add a percentage/fixed quote discount without Admin approval. The combined discount is deducted from the quoted fare and reflected in invoice details.
- Providers can disable their own vouchers. A disabled/expired voucher cannot be newly applied.
- Scope note: notifications are broadcast to approved customer accounts, while actual eligibility is filtered by vehicle/provider at search and booking time. Delivery is in-app only; no SMS/email campaign is configured.

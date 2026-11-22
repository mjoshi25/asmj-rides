# Phase 16B–16E – Rental marketplace booking, journey, invoice and direct payment

## Scope and compatibility
This release extends the separate `rentalBookings` collection. Existing `RideBooking` records, ride routes, driver workflows and payment records are not migrated or rewritten. Rental fares are paid directly by the customer to the Vehicle Owner or authorized Travel Agent; ASMJ Ride does not collect the rental fare.

## 16B – Customer route search and request
- Customer selects pickup and destination city/PIN, pickup and return date/time, one-way or round-trip, and with-driver or self-drive.
- Search includes only Admin-approved, operationally active vehicles whose configured coverage and rental mode match.
- Search excludes overlapping requested/accepted/in-progress rental reservations.
- Booking request revalidates the vehicle, route, mode and dates server-side and acquires the existing Mongo-backed per-vehicle lock before checking overlap and saving.
- Owner and authorized agents with `MANAGE_BOOKINGS` receive in-app notifications.
- A request is not a confirmed booking until provider acceptance.

## 16C – Provider acceptance and assignment
- Vehicle Owner can manage their own vehicle bookings.
- Travel Agent can manage a partner vehicle only with active owner authorization for `MANAGE_BOOKINGS`; with-driver assignment additionally requires `ASSIGN_DRIVER`.
- Provider enters a quoted fare. For with-driver bookings, provider selects an approved, mobile-verified, available driver associated with that provider/vehicle owner. The vehicle and driver assignment is stored and the driver is notified.
- Driver can accept or decline. Declined assignments return to provider for reassignment.
- Customer is notified of acceptance and quoted fare.
- Provider can decline a pending request; customer can cancel a pending request.

## 16D – Journey and odometer
- Assigned driver accepts before starting.
- Driver records start odometer at/after scheduled pickup.
- Driver records end odometer greater than start (maximum 5,000 km difference per booking).
- The agreed provider quote becomes the initial final fare; the driver does not set or alter the fare.
- Self-drive rentals are closed by the provider with handover/return odometer readings and a final fare.

## 16E – Invoice, direct payment, verification and reviews
- Invoice number and invoice data are stored on the rental booking; customer sees invoice and provider payment instructions.
- Vehicle Owner/Travel Agent configures direct payment instructions on the vehicle (UPI ID, bank transfer details or cash collection instructions). Instructions are snapshotted when the provider accepts the booking.
- Customer reports payment by UPI, cash or bank transfer. UPI and bank transfer require a UTR/reference. This is a report only; it does not mark the invoice paid.
- Vehicle Owner or authorized Travel Agent verifies receipt, or marks it disputed. Disputed customers can submit a fresh payment report after resolution/clarification.
- Only after provider verification does booking status become `COMPLETED` and review become available.
- One review per booking supports vehicle, provider and (when applicable) driver ratings.

## Statuses
Booking: `REQUESTED`, `REJECTED`, `CANCELLED`, `ACCEPTED`, `DRIVER_ASSIGNED`, `DRIVER_DECLINED`, `DRIVER_ACCEPTED`, `IN_PROGRESS`, `COMPLETED_PENDING_PAYMENT`, `COMPLETED`.
Payment: `PENDING`, `REPORTED`, `DISPUTED`, `VERIFIED`.
Invoice: `NOT_ISSUED`, `ISSUED`.

## Important limitations / follow-up work
- The invoice is currently rendered in the authenticated customer booking page; a downloadable PDF invoice is not generated in this milestone.
- Payment proof image upload is not implemented; UPI/bank transfer UTR/reference and customer note are captured. Cash can be reported without a reference.
- Provider payment instructions are free-text per vehicle and are shown after invoice issue; a provider may override them for a specific booking when accepting it. Validate and keep them current before production.
- Quote/final fare is entered by the provider. Automated distance/hour/day pricing, taxes, cancellation fees, deposits, refunds and partial payments are not calculated in this milestone.
- Self-drive journey closure is performed by the provider; a customer-side handover/return OTP flow and driverless automated completion are future hardening items.
- Reviews are stored in `rentalReviews`; public aggregate ratings and moderation/reporting UI remain future work.
- Mongo transaction semantics are not assumed. The existing distributed vehicle lock reduces concurrent booking collisions, but production concurrency and index tests remain required.
- Complete Maven build, MongoDB integration, Thymeleaf runtime, browser and mobile testing are required before deployment.

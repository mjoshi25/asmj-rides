# ASMJ Ride — Phases 5–8 implementation

This release builds on the Phase 1–4 integrated source ZIP.

## Phase 5 — Fleet-owner dashboard and trip operations
- Added `/fleet`, restricted to `FLEET_OWNER` accounts.
- Dashboard is scoped to the fleet owner's linked drivers, owned vehicles, associated rides and payment allocations.
- Shows driver availability, vehicle approval, active/completed trips, allocated gross, commission, net settlement totals, driver performance and vehicle performance.
- Fleet owners can assign an approved/available fleet driver and compatible approved fleet vehicle to a requested outward trip.
- When assigning an outward round trip, the same driver/vehicle is stored as the preferred return assignment.
- After the outward leg completes, the fleet owner can assign the same driver or an alternate fleet driver/vehicle to the return leg.

## Phase 6 — Admin trip operations
- Admin can assign/reassign outward drivers and vehicles before a trip starts.
- Admin can assign/reassign return drivers and vehicles.
- Admin can update permitted trip statuses with a mandatory reason.
- Trip status history stores from/to status, actor, reason and timestamp.
- Driver outward and return OTP start/completion events are added to the same history.
- Admin cannot mark a trip completed through the manual status control; drivers must complete it using the existing OTP and distance workflow.

## Phase 7 — Payment status, allocation and settlement
- Round-trip payment submission is blocked until both outward and return legs are completed.
- Customer payment total remains the full booking fare.
- Outward and return gross fares, commission rates, commission amounts and driver/fleet net allocations are stored separately.
- Return allocation points to the actual return driver and their fleet owner, including when a different driver handles the return.
- Driver and fleet-owner payment lists include records where they are assigned to either leg.
- Admin can verify/reject UPI, confirm cash, record refunds and settle outward and return allocations independently.
- Payment status history records customer submission, driver/admin confirmation, UPI review and settlement actions.

## Phase 8 — Analytics and reporting
- Admin analytics supports from/to date, city and journey-type filters.
- Added one-way/round-trip counts, pending return-leg count, paid gross, combined platform commission and unsettled driver/fleet net.
- Added pickup/destination city activity counts.
- Added filtered CSV export at `/admin/operations/export`.
- Driver dashboard now shows paid net allocation, settled net and amount awaiting settlement.
- Fleet dashboard includes driver-wise and vehicle-wise activity/earnings summaries.

## Data compatibility
- New MongoDB fields are additive. Existing `rides` and `ride_payments` records remain readable; legacy payment records with no outward allocation display the existing gross amount as the outward amount.
- Existing one-way trips keep `returnLegStatus=NOT_APPLICABLE` semantics.
- New history arrays are additive and initialized for new records; existing documents may have no history until their next update.

## Important operational notes
- Fleet owners only see trips linked through their fleet drivers or fleet-owned vehicles.
- Admin controls and fleet controls are role-protected by Spring Security.
- Settlements are internal records; reconcile actual bank/UPI transfers separately.
- No automatic bank payout or payment gateway has been added.
- Round-trip outward and return allocations are separate, but the customer submits one combined payment for the completed booking.
- This update does not change the MongoDB connection string. Fix the previously identified extra `}` in the Render `MONGODB_URI` write concern before testing writes.

## Validation performed
- ZIP integrity check.
- POM XML parse.
- Render YAML parse.
- HTML structural scan of Thymeleaf templates.
- Java syntax scan with `javac` parser; dependency resolution/compilation was not possible because Maven and project dependencies are not available in this environment.
- No live MongoDB Atlas, Render, browser or end-to-end test was performed.

## Round-trip customer and driver handoff
- Customer sees a separate return pickup OTP after the return driver accepts/confirm the return leg.
- The preselected outward driver can confirm the preferred return assignment after the outward leg is completed and the return time has arrived.
- A preferred return assignment is not broadcast as an unassigned open request to every driver.
- Fleet owner/admin can replace the preferred driver with another eligible driver when needed.

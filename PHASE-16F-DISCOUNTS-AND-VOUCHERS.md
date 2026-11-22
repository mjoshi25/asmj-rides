# Phase 16F – Provider discounts and vouchers

## Included
- Provider quote-level percentage or fixed discount, no Admin approval.
- Provider-created voucher codes, no Admin approval.
- Optional vehicle scope; percentage/fixed amount; minimum amount; maximum discount cap; start/end; max total and per-customer usage.
- In-app customer notifications on publication.
- Matching active vouchers shown in customer search results; code can be entered before booking.
- Server-side eligibility checks and immutable booking snapshot fields for voucher code, original quote and discount.
- Invoice displays original fare, discount and final fare.

## Operational notes
- Voucher usage is counted when the provider accepts the booking, not when the customer merely requests it.
- Direct payments to the Vehicle Owner / Travel Agent remain unchanged.
- Customer notifications are in-app only. They are sent to approved customer accounts; actual redemption eligibility remains scoped to provider and vehicle.
- This release does not add automatic vehicle-wide sale pricing; the provider can set an additional discount while quoting each booking and can publish reusable vouchers for selected/all managed vehicles.
- Full Maven build, database integration and browser/device testing must be run in the user's Windows development environment before deployment.

# ASMJ Ride – Next Release Implementation

## Included changes

### Partner and vehicle document approval
- Admin can review each partner document independently as Pending, Verified, or Rejected.
- Rejection requires an explanation (maximum 500 characters).
- Rejection notes are displayed to the partner.
- Admin can review each vehicle photo and each vehicle document independently.
- Overall vehicle approval requires at least one verified photo and verified RC and insurance documents.

### Route distance and fare preview
- Admin can configure one-way road distances in Admin → Locations.
- Routes can be city-to-city (leave both PIN fields blank) or PIN-to-PIN (enter both PINs).
- Exact PIN routes take priority over city defaults; reverse direction is supported.
- Round-trip estimate doubles the one-way distance and estimates each leg separately.
- If a route is not configured, the customer can enter an approximate one-way distance.
- Customer must confirm the displayed estimate before submitting.
- The final fare continues to use actual trip distance/duration/waiting data.

### Vehicle search and request dispatch
- New vehicle registrations require an operating/pickup city.
- Vehicles without explicit pickup PIN, drop-off PIN and permitted route coverage are excluded from customer search until a fleet owner or admin configures and re-approves them.
- Customer search filters approved vehicles by category, exact pickup/drop city+PIN coverage, explicit permitted city-pair route, one-way/round-trip support, booking time and available approved/mobile-verified drivers.
- Search checks active/scheduled vehicle and driver overlaps, including round-trip return reservations. The current fallback reserves four hours per leg plus a one-hour turnaround buffer; it is not a live traffic ETA and should be tuned for long-distance routes. MongoDB-backed locks serialize ride, vehicle and driver assignment operations; each assignment rechecks state while holding the locks.
- Selected vehicle is stored with the request; it is not considered assigned until the driver/fleet owner/admin completes the existing assignment workflow.
- Fleet owners can see open requests matching their approved fleet categories and pickup city.
- Admins and customers receive in-app booking notifications; relevant fleet owners/drivers receive request alerts. Customers receive assignment and return-assignment notifications.
- Notifications are in-app only; no SMS/email provider has been configured.

### Display fixes
- Unassigned return drivers display “Driver will be assigned shortly” rather than `null`.
- Customer, admin and fleet screens display estimated fare/distance and selected vehicle information where applicable.

## Operational setup after deployment
1. Open Admin → Locations.
2. Add one-way road distances for the city pairs served by ASMJ Ride. Enter both PINs for a PIN-specific route or leave both blank for a city-wide default.
3. Ask partners to configure each vehicle’s permitted pickup city/PINs, drop-off city/PINs, route pairs and one-way/round-trip support; admins can configure the same coverage in Admin → Uploads. Legacy vehicles with no explicit coverage remain hidden from customer search until configured and approved.
4. Review and verify vehicle photos, RC and insurance documents before approving vehicles.
5. Open Notifications from the admin/partner/driver/customer dashboard to review in-app alerts.
6. Test simultaneous requests for the same vehicle and simultaneous assignment attempts for the same driver; the database-backed locks should allow only one operation to proceed at a time.

## Compatibility notes
- New MongoDB collections: `route_distances`, `ride_notifications`, and `vehicle_booking_locks` (TTL-cleaned reservation lock rows).
- Existing vehicle and ride documents remain readable; new fields are optional for legacy records.
- Existing login, mobile verification, Cloudinary uploads, payment, support, ride status, OTP and round-trip flows are retained.

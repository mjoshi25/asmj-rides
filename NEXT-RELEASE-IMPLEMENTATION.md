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
- Existing vehicles without a configured operating city remain visible as legacy/unconfigured results; partners can add a city by editing the vehicle.
- Customer search filters approved vehicles by pickup city, category and booking time.
- Search checks active/scheduled ride overlaps and round-trip reservation windows.
- Selected vehicle is stored with the request; it is not considered assigned until the driver/fleet owner/admin completes the existing assignment workflow.
- Fleet owners can see open requests matching their approved fleet categories and pickup city.
- Admin, relevant drivers and fleet owners receive in-app notifications. Customers receive in-app notifications when a driver is assigned.
- Notifications are in-app only; no SMS/email provider has been configured.

### Display fixes
- Unassigned return drivers display “Driver will be assigned shortly” rather than `null`.
- Customer, admin and fleet screens display estimated fare/distance and selected vehicle information where applicable.

## Operational setup after deployment
1. Open Admin → Locations.
2. Add one-way road distances for the city pairs served by ASMJ Ride. Enter both PINs for a PIN-specific route or leave both blank for a city-wide default.
3. Ask partners to edit existing vehicles and set their operating city; new vehicle registrations require this field.
4. Review and verify vehicle photos, RC and insurance documents before approving vehicles.
5. Open Notifications from the admin/partner/driver/customer dashboard to review in-app alerts.

## Compatibility notes
- New MongoDB collections: `route_distances`, `ride_notifications`.
- Existing vehicle and ride documents remain readable; new fields are optional for legacy records.
- Existing login, mobile verification, Cloudinary uploads, payment, support, ride status, OTP and round-trip flows are retained.

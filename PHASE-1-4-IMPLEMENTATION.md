# ASMJ Ride – Phases 1–4 implementation

## Included

### Phase 1 – Admin location master
- New MongoDB collection: `service_cities`.
- Admin page: `/admin/locations`.
- Add/edit city, district, state and comma/space-separated six-digit PIN codes.
- Activate/deactivate a city. Inactive cities are excluded from new booking selectors.
- Existing ride records keep their saved city/PIN text even if a city is later deactivated.

### Phase 2 – Booking location validation
- Customer selects pickup city + PIN and destination city + PIN.
- PIN dropdown loads only the configured PINs for the selected active city.
- Server revalidates both city and PIN combinations; client-side selections are not trusted.
- Pickup and destination street/landmark text remains available.

### Phase 3 – One-way and round-trip booking
- `journeyType` is separate from `tripType` (IMMEDIATE/SCHEDULED).
- Round-trip requires a return date/time, 30 minutes to 30 days in the future.
- Scheduled round-trip return must be after the scheduled outward pickup.
- The first assigned driver and vehicle are copied as the preferred return assignment.
- A return leg has its own assignment, OTP, status, distance, fare and timestamps.
- Return driver can be the same driver or another approved driver. Admin can assign/reassign an available approved driver and compatible approved vehicle before the return leg starts.
- Return leg cannot start until outward leg is complete and the return time has arrived.
- Customer payment submission is held until both legs are complete. Combined fare is then used by the existing payment submission flow.

### Phase 4 – Individual driver dashboard
- Shows completed outward trip count and recorded earnings.
- Shows return requests and assigned return trips separately.
- Driver can accept a return leg, verify its pickup OTP, start it and complete it with actual distance.
- Earnings calculation separates outward and return fares when the return is handled by a different driver.

## Admin setup after deployment
1. Log in as admin.
2. Open Admin → Manage cities & PIN codes (`/admin/locations`).
3. Add the cities and six-digit PIN codes where service is available. No cities are auto-created; customer booking requires configured active locations.
4. Approve drivers and vehicles as usual. Drivers need to be mobile-verified and online to accept rides/return legs.

## Compatibility / important limitations
- Existing MongoDB ride documents are backward-compatible: missing `journeyType` is treated as legacy by Java field defaults only when loaded into the model; newly created bookings explicitly save `ONE_WAY` or `ROUND_TRIP`.
- Route distance is still manually entered/estimated; no maps/geocoding provider was added.
- Admin can reassign a return leg before it starts. Return trip fare is calculated from the return leg's entered actual distance using the existing estimate calculator. The existing payment record remains one payment per booking; detailed per-leg driver settlement allocation and fleet-owner dashboards/analytics are outside Phases 1–4 and should be addressed in the later settlement/fleet phases.
- No Maven compile, Atlas integration test, browser test or Render test was available in this build environment. Validate with Maven and a staging Render deployment before production.

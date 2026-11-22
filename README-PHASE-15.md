# ASMJ Rides – Phase 15

This release extends the Phase 14 dashboard readability work to all primary portals: Customer, Driver, Travel Agent and Admin. It retains the Vehicle Owner dashboard redesign.

Extract this ZIP into a new development folder, then run:

```bash
mvn clean test
mvn spring-boot:run
```

Verify at minimum:
- Customer `/customer`: booking form, ride list, documents/profile sections and mobile layout.
- Driver `/driver`: availability, incoming requests, assigned rides, OTP/odometer forms, SOS and location sharing.
- Travel Agent `/partner`: profile/documents, vehicle add/edit and partner-vehicle operations.
- Vehicle Owner `/fleet`: overview, finances, drivers, vehicles and trips.
- Admin `/admin`: quick links, category cards, expandable record tables and role-specific actions.
- Shared header/menu on desktop, tablet and Android-sized viewport.

No production deployment is performed by this release. Do not replace the live Render deployment until the full Maven build and manual workflow checks pass.

# ASMJ Ride Phase 18 – Architecture and Compatibility Foundation

## Confirmed launch policies

- Service coverage: **CITY**. A city must be explicitly enabled in `service_cities`; PIN-code coverage is maintained by the existing `ServiceCity` model.
- Trip assignment: **BOTH**. A trip may be offered to an eligible driver for self-acceptance or assigned by an authorized vehicle owner/fleet operator. Authorization checks remain mandatory.
- Platform commission: **CATEGORY**. Each vehicle category has a configurable commission percentage, with a default fallback. Initial default values are 10% placeholders and must be reviewed by Admin before launch.

## Existing architecture retained

- Java 17, Spring Boot 3.3.5, Thymeleaf, Spring Security and MongoDB Atlas remain the application foundation.
- Existing `users`, `vehicles`, `vehicleCategories`, `rides`, `ride_payments`, `fare_rules`, `service_cities`, rental collections and their IDs are retained.
- Existing rental marketplace records and routes are not migrated or renamed.
- Existing `RideBooking` documents remain readable. New `serviceType` is backward-compatible and resolves to `PASSENGER` when absent/blank.
- Existing `VehicleCategory` documents remain readable. New `serviceType` defaults to `PASSENGER`; capacity values default to zero (unspecified), so existing category behavior is not silently restricted.
- Existing `RideBooking.category` and `Vehicle.category` remain the category identifiers. Do not change existing IDs/codes such as BIKE, AUTO, CAR and OTHER without a migration plan.
- Existing `PaymentRecord` remains the legacy ride-payment record. Do not repurpose it for rental or new goods payment records without a separate compatibility review.

## Phase 18 data foundation added

### `mobility_platform_config` (`MobilityPlatformConfig`)

Singleton ID `default` stores rollout and dispatch policy:

- `serviceCoverageMode`: `CITY`
- `tripAssignmentMode`: `BOTH`
- `commissionPolicy`: `CATEGORY`
- `defaultCategoryCommissionPercent`: 10.0 placeholder
- `categoryCommissionPercent`: map keyed by category code
- `passengerServiceEnabled`, `goodsServiceEnabled`

`MobilityPlatformConfigService.getCurrent()` returns in-memory defaults when no singleton document exists and does not write defaults to MongoDB. This avoids silently seeding or overwriting Admin configuration. `commissionForCategory()` resolves category-specific percentage with the configured fallback.

### `vehicleCategories` additions

- `serviceType`: `PASSENGER`, `GOODS`, or `BOTH` (existing documents default to `PASSENGER`).
- `passengerCapacity`: zero means unspecified, not zero seats.
- `maximumPayloadKg`: zero means unspecified, not zero payload.

### `rides` addition

- `serviceType`: `PASSENGER` by default for legacy records; intended values are `PASSENGER` and `GOODS`.

## Rollout and operational rules

1. City activation is controlled by `ServiceCity.active`; do not enable nationwide service implicitly.
2. A trip must resolve to an active service city before new city-scoped booking/dispatch is enabled.
3. `BOTH` assignment mode is a policy declaration, not permission to bypass existing owner/agent authorization or driver/vehicle approval.
4. Category commission values are configuration only in Phase 18. Existing payment calculations are deliberately not switched over until the dedicated fare/commission phases add snapshots and regression coverage.
5. Goods-specific booking fields, cargo matching, dispatch offers, atomic trip locks and GPS history are later phases. Phase 18 does not claim these workflows are live.
6. Existing ride and rental records must remain untouched. Any future collection migration must be additive, repeatable and backed up.

## Planned service boundaries (within the current modular monolith)

- Identity and approvals: existing `UserAccount`, role requests and document requirements.
- City and coverage: existing `ServiceCity` plus future city/category availability rules.
- Fleet and authorization: existing `Vehicle`, `AgentVehicleAuthorization` and owner/agent services.
- Passenger trips: existing `RideBooking`/`RideController`, extended compatibly.
- Goods trips: dedicated cargo details and goods dispatch model in a later phase.
- Fare policy: existing `FareRule` plus category-specific policy; trip snapshots to be added before charging.
- Payments: existing payment evidence workflow, extended with explicit trip type and ledger isolation in later phases.
- Rental marketplace: existing `RentalBooking` and rental controllers remain separate.

## Validation still required

- Compile with the project's Maven wrapper or Maven 3.9.9 and Java 17.
- Run existing automated tests.
- Test reads of legacy Mongo documents with absent new fields.
- Verify Admin configuration persistence and category commission overrides in a staging MongoDB database.
- Regression-test ride booking, rental booking, payment proof upload and role/ownership authorization.

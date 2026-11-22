# Phase 17C — Travel Agent document requirements and approval messaging

## Fixes

- Travel Agent identity, business-registration and address documents are optional by default. Admin can explicitly mark any active document mandatory from the document-requirements screen.
- Legacy document configurations created by the old built-in defaults are migrated so they do not silently impose mandatory Travel Agent documents when no Admin configuration exists.
- When an explicit Admin-saved mandatory list exists for `TRAVEL_AGENT`, that list remains authoritative, including an explicitly empty list.
- The Travel Agent vehicle-registration gate now checks agency profile status, Travel Agent role approval and whether own-fleet operation is enabled. It does not independently require documents; Admin approval still validates only the configured mandatory documents.
- Vehicle-registration warning messages now explain the specific blocking reason (profile not submitted, profile pending/rejected, role pending, or own fleet disabled) instead of claiming documents are mandatory in every case.

## Expected behavior

1. If Admin configures no mandatory Travel Agent documents, `verifiedUserDocuments(user, "TRAVEL_AGENT")` succeeds without uploaded documents.
2. Admin may still require profile review and approval before a Travel Agent can add own-fleet vehicles.
3. If Admin configures mandatory documents, each configured document must be verified (and valid where expiry is required) before the profile can be approved.
4. Existing vehicle, booking, authorization and payment workflows are unchanged.

## Validation

- `pom.xml` parsed successfully.
- All 36 Thymeleaf templates parsed with an HTML parser.
- Full Maven compilation and Spring/Thymeleaf runtime tests were not run in this environment because Maven is not installed.

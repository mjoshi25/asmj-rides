# ASMJ Ride UI refresh

This update refreshes the shared presentation layer while retaining the existing Spring Boot controllers, Thymeleaf model bindings, routes, forms, CSRF fields, and ride/payment workflows.

## Included
- Unified navy, teal and white visual system with responsive page backgrounds, cards, form controls, buttons, tables, alerts and typography.
- Consistent ASMJ Ride brand treatment and responsive header navigation.
- Collapsible mobile navigation for public, customer, driver/partner and admin headers.
- Clear, consistently styled POST logout controls on authenticated navigation, including the support page.
- Shared footer on all HTML pages and branded top navigation on sign-in, registration, verification and error pages.
- Small accessible navigation toggle script; no new JavaScript framework or backend dependency.

## Deliberately unchanged
- Authentication and authorization rules, CSRF protection, MongoDB models/repositories, ride booking, dispatch, payments, uploads, Cloudinary and manual mobile verification logic.
- Render environment variables and MongoDB connection URI.

## Deployment
Replace the project files with this ZIP's contents, preserving the local `.git` directory if deploying from an existing clone. Commit and push; Render can then deploy the update.

The previously observed MongoDB error (`No write concern mode named 'majority}'`) is an environment/configuration issue and is not changed by this UI-only update. Correct the extra `}` in the Render `MONGODB_URI` separately before retesting writes such as issuing mobile-verification codes.

## Validation
- ZIP integrity checked after packaging.
- POM XML and Render YAML syntax parsed successfully.
- Template structure and shared footer/style/script references checked.
- Maven compilation and live Render/browser testing were not run in this environment.

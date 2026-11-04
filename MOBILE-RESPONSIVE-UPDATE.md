# ASMJ Ride – Mobile Responsive Update

This update is based on the supplied Phase 5–8 Thymeleaf Fix project.

## Scope
- Adds a mobile-first responsive layer for customer, driver, fleet-owner, admin, payments, support, location and verification pages.
- Keeps the desktop layout while improving tablet and phone breakpoints (1024px, 900px, 640px and 380px).
- Prevents horizontal overflow at the document level; wide data tables scroll within their own content area.
- Makes dashboard statistic cards two-column on phones and four-column on wider screens.
- Stacks ride cards, action groups, filters and forms on narrow screens.
- Improves mobile navigation sizing/scrolling, form control readability, footer wrapping and support chat layout.
- Updates stylesheet cache keys to `mobile-responsive-v1`.

## Functional scope
CSS/template cache references only. No Java controllers, repositories, domain models, authentication, booking, trip, payment, Cloudinary or MongoDB behaviour was changed.

## Validation
Static checks and ZIP integrity were performed. No Maven compilation, browser automation, physical-device testing or live Render testing was performed in this environment. Please validate on real devices after deployment.

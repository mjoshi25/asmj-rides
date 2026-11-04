# ASMJ Ride UI Redesign v2

This update is a more substantial visual redesign than the previous UI refresh.

## Visual changes
- Replaced the previous light-blue styling with a high-contrast midnight navy, teal and mint visual identity.
- Reworked the public landing page into a dark gradient hero with route illustration, primary calls to action and three feature cards.
- Redesigned application headers, navigation links, user chips and logout controls.
- Added a responsive collapsible navigation menu for smaller screens.
- Reworked form controls, booking forms, buttons, alerts, ride cards, data tables and page panels.
- Redesigned the footer with a branded dark treatment.
- Added dashboard-style metric tiles on the admin analytics page.
- Added page-specific body classes for future page-level refinements.
- Updated stylesheet cache-busting version to `20261104-ui-redesign-v2`.

## Functionality preservation
- Existing Spring MVC routes, Thymeleaf expressions, form actions, CSRF fields, inputs, booking logic, authentication, manual mobile verification, Cloudinary integration and MongoDB models are retained.
- No Java backend or database model files were changed as part of this UI-only update.

## Validation
- ZIP integrity checked after packaging.
- Template checks performed for stylesheet references, navigation toggle hooks, CSRF form fields and presence of existing route/action references.
- Maven compile and live Render browser testing were not performed in this environment. Please verify the deployed UI in desktop and mobile browsers.

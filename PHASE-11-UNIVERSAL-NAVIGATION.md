# Phase 11 — Universal Navigation Fix

## Scope
- Correct the public homepage menu shown on phones: navigation links must remain hidden until the hamburger button is activated.
- Use the same click-to-open dropdown pattern at desktop, tablet and mobile widths.
- Keep the authenticated shared header menu behaviour consistent across responsive breakpoints.
- Preserve role-aware navigation links and existing destinations.
- Add accessible menu labelling, controlled-element relationship and visible keyboard focus.
- Refresh CSS/JS query versions across templates to avoid stale browser caches.

## Implementation notes
- Public homepage header now has a stable `home-nav-header` class and a named navigation region.
- The hamburger is visible at all viewport widths; the menu is hidden by default and appears only while the header has `nav-open`.
- Existing `app-ui.js` toggle logic updates `aria-expanded`, closes on outside click, Escape and navigation selection.
- Authenticated pages continue using `fragments/navigation.html` and the unified header styles.

## Verification
- HTML templates parsed with BeautifulSoup.
- CSS selectors and public menu state rules inspected.
- JavaScript syntax checked with Node.js when available.
- ZIP archive integrity checked.
- Full Maven build and real-device browser tests are not available in this environment and remain required before deployment.

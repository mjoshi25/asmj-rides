# Phase 9 – Account and Support Navigation Fix

## Changes
- The shared authenticated navigation now labels the support destination **Chat with Admin** and links directly to `/support`.
- The shared profile/avatar and Account & Settings menu link now target `/account` as the canonical account page.
- `/profile` remains a backward-compatible route and redirects to `/account` instead of rendering the account template through an alias. The account and profile routes are explicitly protected by authentication.
- Profile-photo upload success now returns to `/account`, keeping the user in the canonical account area.
- The support chat's registered mobile number is displayed as read-only. The help text directs users to Account & Settings for the Admin-verified contact-change workflow.
- The support-ticket creation handler ignores the submitted mobile value and always records the mobile number stored on the authenticated account. This prevents tampering with the read-only field using browser developer tools or a crafted request.
- Support navigation includes a direct Chat with Admin entry and support-history anchor.

## Verification performed
- Reviewed the account/profile route mappings and security matchers.
- Confirmed the shared navigation contains the hamburger control, Chat with Admin, and Account & Settings links.
- Confirmed support form mobile field is read-only and the server derives ticket mobile from the authenticated account.
- Full Maven and browser testing must still be run in the user's development environment.

## Test checklist
1. Sign in as a customer and open the hamburger menu on Customer, Support, and Account pages.
2. Select Chat with Admin; confirm `/support` opens and the new chat form is visible.
3. Confirm registered mobile is read-only.
4. Attempt to alter the mobile field through browser developer tools; create a support ticket and verify its saved mobile remains the account's registered mobile.
5. Select Account & Settings or the avatar; confirm `/account` opens rather than `/`.
6. Upload a profile photo; confirm success returns to `/account` and the header avatar updates after refresh.
7. Visit the legacy `/profile` URL; confirm it redirects to `/account`.

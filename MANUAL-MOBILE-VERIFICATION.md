# ASMJ Ride — Manual Mobile Verification

SMS gateway integration has been removed. ASMJ Ride now uses an administrator-issued security code. No Fast2SMS/MSG91 account, API key, DLT entity, sender ID, content template, wallet balance, or SMS configuration is required.

## Admin workflow

1. Sign in with an administrator account.
2. Open **Mobile verification** in the admin navigation or visit `/admin/mobile-verifications`.
3. Review the user's name, email, mobile number, role, account status, and registration date. Verify the requester's identity and that they control the registered mobile number using your normal manual process.
4. Click **Generate security code**. The page displays the six-digit code once, along with the account details and expiry.
5. Share the code privately with that account holder using a contact channel you have independently verified. Do not post it publicly or send it to someone other than the account holder.
6. The user opens `/verify-mobile`, enters their registered email and the code, and submits the form.
7. The admin page will show the account as VERIFIED, with verification timestamp and method.

## Security behavior

- Code length: six numeric digits.
- Expiry: 24 hours from generation.
- Attempts: five incorrect attempts; the code is invalidated after the fifth failure.
- Reissue: generating a new code invalidates the previous code and resets the attempt counter.
- Storage: only a BCrypt hash is stored in MongoDB; plaintext code is shown to the admin once using a one-time redirect flash message and is never logged.
- Admin accounts are excluded from this customer/partner verification flow.
- Driver and fleet-owner account approval rules remain separate from mobile verification.
- Suspended or rejected accounts cannot verify until their status is resolved.

## Deployment

1. Replace the project files with this release and push to the existing GitHub `main` branch.
2. Render should deploy automatically. No new OTP environment variables are required.
3. You may remove obsolete `OTP_PROVIDER`, `FAST2SMS_API_KEY`, `FAST2SMS_OTP_ID`, and `OTP_DEV_MODE` variables from Render; the application no longer reads them.
4. Keep `MONGODB_URI`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`, and Cloudinary variables as before.
5. After deployment, sign in as admin, open Mobile verification, issue a code for a test account, share it privately, and verify the customer login.

## Important operational note

This is a manual review process, not automatic proof of SIM ownership. Before issuing a code, the admin must independently confirm the person's identity and access to the registered mobile number. Keep the code private.

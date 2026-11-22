# Phase 8 – Secure Account Contact Change

## Workflow
1. Only users whose account status is `APPROVED` may request a registered email/mobile change.
2. The user submits a new email, mobile, or both. The old verified contact values remain active while the request is pending.
3. The application checks email/mobile uniqueness and creates a `PENDING_ADMIN_REVIEW` record in MongoDB (`contact_change_requests`).
4. Admins receive an in-app notification and review requests at `/admin/contact-changes`.
5. Admin approves by generating one six-digit code. Only a BCrypt hash is persisted; the plaintext is shown once to the Admin after issuing.
6. Because this repository has no configured SMS gateway or outbound email provider, Admin must securely deliver that same code manually to BOTH proposed channels (SMS/WhatsApp/email outside the application). The page explicitly states this limitation.
7. User enters the code from Account/Profile. On success, both proposed contact values are committed, the request is marked `COMPLETED`, the mobile remains verified, the session is invalidated, and the user must sign in again using the new email.

## Security controls
- One active request per user (`PENDING_ADMIN_REVIEW` or `VERIFICATION_PENDING`).
- Only approved accounts can submit and only Admin can issue/reject.
- Email and mobile uniqueness checked at request and again before commit.
- Six-digit code generated with `SecureRandom`.
- BCrypt hash only is stored; plaintext is not persisted.
- Code expires after 10 minutes; maximum five attempts; locked/expired requests cannot verify.
- Existing contacts are not changed until successful verification.
- Audit fields record request, Admin review, code issue, verification, completion, actors, attempts and status.
- Admin and user in-app notifications record submission/completion/rejection.

## Important delivery limitation
No SMS or email sending integration exists in this project. Phase 8 therefore implements Admin-mediated manual delivery of the one-time code. It does not claim automatic SMS/email delivery. A production SMS/email provider can be integrated later without changing the request/verification workflow.

## Test checklist
- Approved user requests email only, mobile only, and both.
- Pending request prevents a second open request.
- Admin notification links to contact-change queue.
- Admin issue action displays one-time code once and marks request `VERIFICATION_PENDING`.
- Correct code updates contacts and forces fresh login; incorrect, expired and sixth attempts do not update contacts.
- Duplicate email/mobile is rejected.
- Rejected requests preserve existing contact details.
- Confirm non-approved users cannot submit requests.

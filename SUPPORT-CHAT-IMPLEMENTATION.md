# ASMJ Ride – Admin Support Chat

## Included
- Authenticated customers, drivers and fleet partners can start a support conversation with the ASMJ Ride admin team.
- A mobile number is mandatory when creating a conversation and is prefilled from the signed-in account. The submitted contact number is stored with the support ticket and shown in the admin inbox.
- Subject, category and first message are required. A ride can optionally be associated with the conversation.
- Attachments are optional: up to five JPG, PNG, WEBP or PDF files per message, with the existing 8 MB per-file limit.
- Follow-up messages are stored as an ordered conversation thread. Admins can reply, attach files and set OPEN, IN_PROGRESS, RESOLVED or CLOSED status.
- Existing support ticket documents remain readable; older `message`, `adminReply` and `attachments` fields are retained.
- Attachments use the existing Cloudinary authenticated/private delivery flow. Downloads are served through `/files/support/{ticketId}/{assetId}` and are restricted to the ticket owner or an admin.
- Customer/partner navigation labels the entry point **Chat with Admin**; admin navigation labels it **Support inbox**.
- Footer links now use Spring Security's Thymeleaf dialect: authenticated sessions see Dashboard and Logout, while anonymous sessions see Sign in and Create account.

## Routes
- `GET /support` – user's conversations or admin inbox.
- `POST /support/create` – create a conversation; requires mobile, category, subject and first message.
- `POST /support/{id}/message` – customer/driver/fleet partner reply; ticket ownership is checked.
- `POST /admin/support/{id}/message` – admin reply and optional status update; admin role is checked.
- Existing `POST /admin/support/{id}/reply` is retained for compatibility.

## Deployment notes
- No new environment variables are required. Existing MongoDB and Cloudinary configuration is reused.
- The new `messages` and `userMobile` fields are added to the existing MongoDB `support_tickets` documents; no collection migration or destructive database operation is required.
- The UI is a threaded support chat, not a websocket/live-push chat. Users refresh/reopen Support to see a newly posted reply.
- Build with Java 17 and Maven before deploying. Maven compilation and live Render testing were not possible in the authoring environment because Maven is not installed.

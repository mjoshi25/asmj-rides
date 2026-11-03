# ASMJ Ride — MSG91 SMS OTP Setup

## 1. Configure MSG91

1. Sign in to your MSG91 account.
2. Open **OTP** and create an OTP template.
3. Put the required OTP placeholder `##OTP##` in the message body and save the template.
4. Complete the applicable Indian DLT registration, sender ID and template approval requirements in MSG91 before sending production SMS.
5. Copy the MSG91 **Authkey** and the OTP **Template ID**. Keep the Authkey private.

MSG91's SendOTP API generates and stores the OTP on its side. ASMJ Ride asks MSG91 to send a 6-digit code with a 5-minute expiry and calls MSG91's Verify OTP endpoint when the customer submits the code.

## 2. Configure Render

In Render → ASMJ Ride service → Environment, add:

| Environment variable | Value |
|---|---|
| `MSG91_AUTHKEY` | Your MSG91 Authkey (secret) |
| `MSG91_OTP_TEMPLATE_ID` | The OTP template ID from MSG91 |
| `MSG91_COUNTRY_CODE` | `91` for India |
| `OTP_DEV_MODE` | `false` |

Save and deploy. Do not put credentials in GitHub, README files, source code, screenshots or support messages.

## 3. Customer verification flow

1. Register with a 10-digit Indian mobile number.
2. On the Verify Mobile page, choose **Send / Resend OTP**.
3. Enter the SMS code and choose **Verify mobile**.
4. After successful verification, sign in using the registered email and password.
5. Driver and fleet-owner accounts still require admin approval before ride-partner operations are available.

The resend button has a 30-second per-account cooldown. MSG91 remains responsible for OTP generation and verification; the application does not store the production OTP in MongoDB.

## 4. Troubleshooting

- If the Send OTP button is disabled, the service did not receive both MSG91 environment variables. Confirm the Render environment settings and active production deployment.
- If MSG91 rejects the send request, check the template ID, account status/balance, country settings, DLT registration, sender ID and template approval in MSG91.
- If the SMS arrives but verification fails, check that the user enters the latest code before its expiry and inspect MSG91 OTP logs.
- Render logs contain only application-level error messages; never log or share the Authkey or OTP.

## 5. Local development

For local-only testing, set `OTP_DEV_MODE=true` and leave MSG91 credentials unset. The local development code is shown on the OTP page. Never enable this mode on the public Render service.

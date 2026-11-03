package in.asmj.ride.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Server-side MSG91 OTP integration. MSG91 auth credentials never reach the browser. */
@Service
public class Msg91OtpService {
    private final String authKey;
    private final String templateId;
    private final String countryCode;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public Msg91OtpService(
            @Value("${app.otp.msg91.authkey:}") String authKey,
            @Value("${app.otp.msg91.template-id:}") String templateId,
            @Value("${app.otp.msg91.country-code:91}") String countryCode,
            ObjectMapper mapper) {
        this.authKey = authKey == null ? "" : authKey.trim();
        this.templateId = templateId == null ? "" : templateId.trim();
        this.countryCode = countryCode == null || countryCode.isBlank() ? "91" : countryCode.replaceAll("\\D", "");
        this.mapper = mapper;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public boolean isConfigured() {
        return !authKey.isBlank() && !templateId.isBlank();
    }

    public void sendOtp(String mobile) {
        requireConfiguration();
        String internationalMobile = normalizeMobile(mobile);
        String url = "https://control.msg91.com/api/v5/otp?template_id=" + encode(templateId)
                + "&mobile=" + encode(internationalMobile)
                + "&otp_length=6&otp_expiry=5&realTimeResponse=1";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("authkey", authKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
        JsonNode response = execute(request, "MSG91 could not send the OTP. Check your MSG91 template and account configuration.");
        String type = response.path("type").asText("");
        if (!"success".equalsIgnoreCase(type)) {
            throw new IllegalStateException("MSG91 did not accept the OTP request. Check the OTP template, DLT approval and MSG91 balance.");
        }
    }

    public boolean verifyOtp(String mobile, String otp) {
        requireConfiguration();
        if (otp == null || !otp.matches("[0-9]{4,9}")) return false;
        String url = "https://control.msg91.com/api/v5/otp/verify?mobile=" + encode(normalizeMobile(mobile))
                + "&otp=" + encode(otp.trim());
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("authkey", authKey)
                .GET()
                .build();
        JsonNode response = execute(request, "MSG91 verification is temporarily unavailable. Please try again.");
        String type = response.path("type").asText("");
        String message = response.path("message").asText("").toLowerCase();
        return "success".equalsIgnoreCase(type)
                || (message.contains("otp") && message.contains("verified") && !message.contains("not verified"));
    }

    private JsonNode execute(HttpRequest request, String fallbackMessage) {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode body = mapper.readTree(response.body() == null ? "{}" : response.body());
            if (body == null) body = mapper.createObjectNode();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(fallbackMessage);
            }
            return body;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(fallbackMessage);
        } catch (IOException | RuntimeException ex) {
            if (ex instanceof IllegalStateException state) throw state;
            throw new IllegalStateException(fallbackMessage);
        }
    }

    private void requireConfiguration() {
        if (!isConfigured()) {
            throw new IllegalStateException("Mobile verification is not configured yet. Please contact ASMJ Ride support.");
        }
    }

    private String normalizeMobile(String mobile) {
        String digits = mobile == null ? "" : mobile.replaceAll("\\D", "");
        if (digits.length() == 10) return countryCode + digits;
        if (digits.length() >= 11 && digits.length() <= 15) return digits;
        throw new IllegalArgumentException("Please enter a valid mobile number with country code.");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

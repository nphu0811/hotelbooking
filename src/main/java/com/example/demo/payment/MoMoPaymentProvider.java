package com.example.demo.payment;

import com.example.demo.entity.PaymentStatus;
import com.example.demo.service.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class MoMoPaymentProvider implements PaymentProvider {
    private static final String REQUEST_TYPE = "captureWallet";

    private final String partnerCode;
    private final String accessKey;
    private final String secretKey;
    private final String createUrl;
    private final String returnUrl;
    private final String ipnUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public MoMoPaymentProvider(@Value("${momo.partner-code:}") String partnerCode,
                               @Value("${momo.access-key:}") String accessKey,
                               @Value("${momo.secret-key:}") String secretKey,
                               @Value("${momo.create-url:}") String createUrl,
                               @Value("${momo.return-url:}") String returnUrl,
                               @Value("${momo.ipn-url:}") String ipnUrl) {
        this.partnerCode = partnerCode;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.createUrl = createUrl;
        this.returnUrl = returnUrl;
        this.ipnUrl = ipnUrl;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public PaymentIntent createPaymentIntent(PaymentIntentRequest request) {
        requireConfigured();
        String orderId = momoOrderId(request.idempotencyKey());
        String requestId = orderId;
        long amountValue = request.amount().setScale(0, RoundingMode.HALF_UP).longValueExact();
        String amount = Long.toString(amountValue);
        String orderInfo = "HotelBooking " + request.bookingCode();
        String extraData = "";
        String signature = hmacSha256(secretKey, createSignature(amount, extraData, orderId, orderInfo, requestId));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partnerCode", partnerCode);
        payload.put("accessKey", accessKey);
        payload.put("requestId", requestId);
        payload.put("amount", amountValue);
        payload.put("orderId", orderId);
        payload.put("orderInfo", orderInfo);
        payload.put("redirectUrl", returnUrl);
        payload.put("ipnUrl", ipnUrl);
        payload.put("extraData", extraData);
        payload.put("requestType", REQUEST_TYPE);
        payload.put("signature", signature);
        payload.put("lang", "vi");

        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(createUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("MoMo provider rejected the payment request");
            }
            JsonNode responseBody = objectMapper.readTree(response.body());
            String payUrl = text(responseBody, "payUrl");
            if (payUrl.isBlank()) {
                String message = text(responseBody, "message");
                throw new BusinessException(message.isBlank() ? "MoMo provider did not return a payment URL" : message);
            }
            return new PaymentIntent(getProviderName(), orderId, payUrl, requestBody);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("Unable to create MoMo payment request");
        }
    }

    @Override
    public boolean verifyWebhook(Map<String, String> headers, String rawPayload) {
        requireConfigured();
        Map<String, String> params = parsePayload(rawPayload);
        String receivedSignature = params.get("signature");
        if (receivedSignature == null || receivedSignature.isBlank()) {
            return false;
        }
        String expectedSignature = hmacSha256(secretKey, callbackSignature(params));
        return expectedSignature.equalsIgnoreCase(receivedSignature);
    }

    @Override
    public PaymentWebhookPayload parseWebhook(Map<String, String> headers, String rawPayload) {
        Map<String, String> params = parsePayload(rawPayload);
        String orderId = value(params, "orderId");
        if (orderId.isBlank()) {
            throw new BusinessException("MoMo webhook is missing orderId");
        }
        String resultCode = value(params, "resultCode");
        PaymentStatus status = "0".equals(resultCode) ? PaymentStatus.PAID : PaymentStatus.FAILED;
        String transId = value(params, "transId");
        String eventId = "momo:" + orderId + ":" + transId + ":" + resultCode;
        return new PaymentWebhookPayload(
                getProviderName(),
                eventId,
                orderId,
                null,
                new java.math.BigDecimal(value(params, "amount").isBlank() ? "0" : value(params, "amount")),
                "VND",
                status,
                transId,
                rawPayload
        );
    }

    @Override
    public RefundResult refund(RefundCommand command) {
        requireConfigured();
        return new RefundResult(true, false, null,
                "MoMo refund request adapter is configured; settlement must be completed by provider operations");
    }

    @Override
    public String getProviderName() {
        return "momo";
    }

    private void requireConfigured() {
        if (partnerCode.isBlank() || accessKey.isBlank() || secretKey.isBlank()
                || createUrl.isBlank() || returnUrl.isBlank() || ipnUrl.isBlank()) {
            throw new BusinessException("MoMo provider is not configured");
        }
    }

    private String createSignature(String amount, String extraData, String orderId, String orderInfo, String requestId) {
        return "accessKey=" + accessKey
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + returnUrl
                + "&requestId=" + requestId
                + "&requestType=" + REQUEST_TYPE;
    }

    private String callbackSignature(Map<String, String> params) {
        return "accessKey=" + accessKey
                + "&amount=" + value(params, "amount")
                + "&extraData=" + value(params, "extraData")
                + "&message=" + value(params, "message")
                + "&orderId=" + value(params, "orderId")
                + "&orderInfo=" + value(params, "orderInfo")
                + "&orderType=" + value(params, "orderType")
                + "&partnerCode=" + value(params, "partnerCode")
                + "&payType=" + value(params, "payType")
                + "&requestId=" + value(params, "requestId")
                + "&responseTime=" + value(params, "responseTime")
                + "&resultCode=" + value(params, "resultCode")
                + "&transId=" + value(params, "transId");
    }

    private Map<String, String> parsePayload(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return Map.of();
        }
        String trimmed = rawPayload.trim();
        if (trimmed.startsWith("{")) {
            try {
                JsonNode root = objectMapper.readTree(trimmed);
                Map<String, String> params = new LinkedHashMap<>();
                root.fields().forEachRemaining(entry -> params.put(entry.getKey(), entry.getValue().asText("")));
                return params;
            } catch (Exception ex) {
                throw new BusinessException("Invalid MoMo webhook payload");
            }
        }
        return Arrays.stream(trimmed.split("&"))
                .map(pair -> pair.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(parts -> decode(parts[0]), parts -> decode(parts[1]), (left, right) -> right, LinkedHashMap::new));
    }

    private String momoOrderId(String idempotencyKey) {
        String normalized = idempotencyKey.replaceAll("[^0-9A-Za-z_.-]", "-").toLowerCase(Locale.ROOT);
        if (!normalized.isBlank() && Character.isLetterOrDigit(normalized.charAt(0))) {
            return normalized.length() <= 50 ? normalized : normalized.substring(0, 41) + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return "hb-" + UUID.randomUUID().toString().replace("-", "");
    }

    private String hmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new BusinessException("Unable to sign MoMo payload");
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field == null || field.isNull() ? "" : field.asText("");
    }

    private String value(Map<String, String> params, String key) {
        return params.getOrDefault(key, "");
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unused")
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

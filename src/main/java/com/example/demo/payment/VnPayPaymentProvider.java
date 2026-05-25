package com.example.demo.payment;

import com.example.demo.entity.PaymentStatus;
import com.example.demo.service.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class VnPayPaymentProvider implements PaymentProvider {
    private static final DateTimeFormatter VNPAY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final String tmnCode;
    private final String hashSecret;
    private final String payUrl;
    private final String returnUrl;
    private final String ipnUrl;
    private final Clock clock;

    public VnPayPaymentProvider(@Value("${vnpay.tmn-code:}") String tmnCode,
                                @Value("${vnpay.hash-secret:}") String hashSecret,
                                @Value("${vnpay.pay-url:}") String payUrl,
                                @Value("${vnpay.return-url:}") String returnUrl,
                                @Value("${vnpay.ipn-url:}") String ipnUrl,
                                Clock clock) {
        this.tmnCode = tmnCode;
        this.hashSecret = hashSecret;
        this.payUrl = payUrl;
        this.returnUrl = returnUrl;
        this.ipnUrl = ipnUrl;
        this.clock = clock;
    }

    @Override
    public PaymentIntent createPaymentIntent(PaymentIntentRequest request) {
        requireConfigured();
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", request.amount().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString());
        params.put("vnp_CurrCode", request.currency());
        params.put("vnp_TxnRef", request.idempotencyKey());
        params.put("vnp_OrderInfo", "HotelBooking " + request.bookingCode());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_IpAddr", request.clientIpAddress());
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpnUrl", ipnUrl);
        params.put("vnp_CreateDate", VNPAY_TIME_FORMAT.format(Instant.now(clock)));
        String query = canonical(params);
        String secureHash = hmacSha512(hashSecret, query);
        String redirectUrl = payUrl + "?" + query + "&vnp_SecureHash=" + encode(secureHash);
        return new PaymentIntent(getProviderName(), request.idempotencyKey(), redirectUrl, toJsonLike(params));
    }

    @Override
    public boolean verifyWebhook(Map<String, String> headers, String rawPayload) {
        requireConfigured();
        Map<String, String> params = parseForm(rawPayload);
        String receivedHash = params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");
        if (receivedHash == null || receivedHash.isBlank()) {
            return false;
        }
        String expectedHash = hmacSha512(hashSecret, canonical(new TreeMap<>(params)));
        return expectedHash.equalsIgnoreCase(receivedHash);
    }

    @Override
    public PaymentWebhookPayload parseWebhook(Map<String, String> headers, String rawPayload) {
        Map<String, String> params = parseForm(rawPayload);
        String txnRef = params.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            throw new BusinessException("VNPay webhook is missing vnp_TxnRef");
        }
        BigDecimal amount = new BigDecimal(params.getOrDefault("vnp_Amount", "0"))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        String responseCode = params.getOrDefault("vnp_ResponseCode", "");
        String transactionStatus = params.getOrDefault("vnp_TransactionStatus", "");
        PaymentStatus status = "00".equals(responseCode) && "00".equals(transactionStatus)
                ? PaymentStatus.PAID
                : PaymentStatus.FAILED;
        String transactionNo = params.getOrDefault("vnp_TransactionNo", "");
        String eventId = "vnpay:" + txnRef + ":" + transactionNo + ":" + responseCode + ":" + transactionStatus;
        UUID bookingId = parseBookingIdFromIdempotencyKey(txnRef);
        return new PaymentWebhookPayload(
                getProviderName(),
                eventId,
                txnRef,
                bookingId,
                amount,
                params.getOrDefault("vnp_CurrCode", "VND"),
                status,
                transactionNo,
                rawPayload
        );
    }

    @Override
    public RefundResult refund(RefundCommand command) {
        requireConfigured();
        return new RefundResult(true, false, null,
                "VNPay refund request adapter is configured; settlement must be completed by provider webhook/reconciliation");
    }

    @Override
    public String getProviderName() {
        return "vnpay";
    }

    private void requireConfigured() {
        if (tmnCode.isBlank() || hashSecret.isBlank() || payUrl.isBlank() || returnUrl.isBlank() || ipnUrl.isBlank()) {
            throw new BusinessException("VNPay provider is not configured");
        }
    }

    private UUID parseBookingIdFromIdempotencyKey(String idempotencyKey) {
        String raw = idempotencyKey;
        int separator = raw.indexOf(':');
        if (separator > 0) {
            raw = raw.substring(0, separator);
        }
        return UUID.fromString(raw);
    }

    private Map<String, String> parseForm(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return Map.of();
        }
        return java.util.Arrays.stream(rawPayload.split("&"))
                .map(pair -> pair.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(parts -> decode(parts[0]), parts -> decode(parts[1]), (left, right) -> right, TreeMap::new));
    }

    private String canonical(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String hmacSha512(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new BusinessException("Unable to sign VNPay payload");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String toJsonLike(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\":\"" + entry.getValue() + "\"")
                .collect(Collectors.joining(",", "{", "}"));
    }
}

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class TestMoMo {
    public static void main(String[] args) throws Exception {
        String accessKey = "klm0567shj9qRrA";
        String secretKey = "at67qH6mk8w5Y1nAyMoYKMWACiEi2bsa";
        String amount = "2000";
        String extraData = "";
        String ipnUrl = "https://hotelbooking-production-57a9.up.railway.app/payments/momo/webhook";
        String orderId = "38a7e0d6-77db-44bf-ace2-738d91e8de14-momo";
        String orderInfo = "HotelBooking HB-5A6CA7BA";
        String partnerCode = "MOMOBKUN20180529";
        String redirectUrl = "https://hotelbooking-production-57a9.up.railway.app/payments/momo/return";
        String requestId = "38a7e0d6-77db-44bf-ace2-738d91e8de14-momo";
        String requestType = "captureWallet";

        String rawData = "accessKey=" + accessKey +
                "&amount=" + amount +
                "&extraData=" + extraData +
                "&ipnUrl=" + ipnUrl +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + partnerCode +
                "&redirectUrl=" + redirectUrl +
                "&requestId=" + requestId +
                "&requestType=" + requestType;
        
        System.out.println("Raw Data: " + rawData);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(rawData.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        System.out.println("Signature: " + hex.toString());
    }
}

package com.check24.internetcomparison.api;

import com.check24.internetcomparison.model.Address;
import com.check24.internetcomparison.model.InternetOffer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Component
public class PingPerfectClient implements ProviderClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${providers.pingPerfect.clientId}")
    private String clientId;

    @Value("${providers.pingPerfect.signatureSecret}")
    private String signatureSecret;

    private final String endpoint = "https://pingperfect.gendev7.check24.fun/api/products";

    @Override
    public List<InternetOffer> fetchOffers(Address address) {
        List<InternetOffer> offers = new ArrayList<>();

        try {
            // Build request payload
            Map<String, Object> payload = Map.of(
                    "street", address.street(),
                    "houseNumber", "12",
                    "city", address.city(),
                    "plz", address.zip(),
                    "countryCode", "DE"
            );
            String jsonBody = objectMapper.writeValueAsString(payload);

            // Generate timestamp and signature
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String toSign = timestamp + ":" + jsonBody;
            String signature = hmacSha256(toSign, signatureSecret);

            // Build headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Client-Id", clientId);
            headers.set("X-Timestamp", timestamp);
            headers.set("X-Signature", signature);
            System.out.println("PingPerfect Payload: " + jsonBody);
            System.out.println("Timestamp: " + timestamp);
            System.out.println("Signature: " + signature);
            System.out.println("ClientId: " + clientId);

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            // Call API
            ResponseEntity<Map[]> response = restTemplate.exchange(endpoint, HttpMethod.POST, request, Map[].class);
            Map[] results = response.getBody();
            ResponseEntity<String> raw = restTemplate.exchange(endpoint, HttpMethod.POST, request, String.class);
            System.out.println("PingPerfect RAW-RESPONSE: " + raw);
            System.out.println("Antwort von PingPerfect (roh): " + Arrays.toString(results));

            if (results == null) return offers;

            for (Map offer : results) {
                String name = (String) offer.get("productName");
                Object priceObj = offer.get("monthlyCostInCent");
                if (name != null && priceObj instanceof Integer priceCent) {
                    double price = priceCent / 100.0;
                    offers.add(new InternetOffer("PingPerfect", name, price));
                }
            }
            System.out.println("Antwort von PingPerfect (roh): " + Arrays.toString(results));

        } catch (Exception e) {
            System.out.println("Fehler bei PingPerfect: " + e.getMessage());
        }

        return offers;
    }

    private String hmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    @Override
    public String getProviderName() {
        return "PingPerfect";
    }
}

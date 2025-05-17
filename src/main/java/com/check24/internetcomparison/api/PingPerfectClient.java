package com.check24.internetcomparison.api;

import com.check24.internetcomparison.model.Address;
import com.check24.internetcomparison.model.InternetOffer;
import com.check24.internetcomparison.model.PingPerfectDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class PingPerfectClient implements ProviderClient {

    private static final Logger log = LoggerFactory.getLogger(PingPerfectClient.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${providers.pingPerfect.clientId}")
    private String clientId;

    @Value("${providers.pingPerfect.signatureSecret}")
    private String secret;

    @Override
    public List<InternetOffer> fetchOffers(Address address) {
        List<InternetOffer> offers = new ArrayList<>();

        PingPerfectDto.CompareProductsRequestData requestBody = new PingPerfectDto.CompareProductsRequestData(
                address.street(),
                address.zip(),
                address.houseNumber(),
                address.city(),
                true // wantsFiber
        );

        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String toSign = timestamp + ":" + jsonBody;

            String signature = calculateHmacSHA256(toSign, secret);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Client-Id", clientId);
            headers.set("X-Timestamp", timestamp);
            headers.set("X-Signature", signature);

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            String url = "https://pingperfect.gendev7.check24.fun/internet/angebote/data";

            log.info("PingPerfect Anfrage an: {}", url);

            ResponseEntity<List<PingPerfectDto.InternetProduct>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            List<PingPerfectDto.InternetProduct> body = response.getBody();
            if (body != null) {
                for (PingPerfectDto.InternetProduct entry : body) {
                    String name = entry.providerName();
                    int price = entry.pricingDetails().monthlyCostInCent();
                    double priceEuro = price / 100.0;

                    offers.add(new InternetOffer("PingPerfect", name, priceEuro));
                }
            }

        } catch (Exception e) {
            log.error("Fehler bei Anfrage an PingPerfect: {}", e.getMessage(), e);
        }

        return offers;
    }

    @Override
    public String getProviderName() {
        return "PingPerfect";
    }

    private String calculateHmacSHA256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : rawHmac) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

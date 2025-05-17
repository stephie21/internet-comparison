package com.check24.internetcomparison.api;

import com.check24.internetcomparison.model.Address;
import com.check24.internetcomparison.model.InternetOffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ServusSpeedClient implements ProviderClient {

    private static final Logger log = LoggerFactory.getLogger(ServusSpeedClient.class);
    private static final String BASE_URL = "https://servus-speed.gendev7.check24.fun";
    private final RestTemplate restTemplate;
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;

    @Value("${providers.servusspeed.username}")
    private String username;

    @Value("${providers.servusspeed.password}")
    private String password;

    public ServusSpeedClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);  // 15 Sekunden Connect-Timeout
        factory.setReadTimeout(30000);     // 30 Sekunden Read-Timeout
        this.restTemplate = new RestTemplate(factory);
    }

    private <T> ResponseEntity<T> executeWithRetry(String url, HttpMethod method, HttpEntity<?> request, Class<T> responseType) {
        int retryCount = 0;
        while (true) {
            try {
                return restTemplate.exchange(url, method, request, responseType);
            } catch (ResourceAccessException e) {
                retryCount++;
                if (retryCount >= MAX_RETRIES) {
                    log.error("ServusSpeed: Maximale Anzahl an Wiederholungen erreicht nach {} Versuchen", MAX_RETRIES);
                    throw e;
                }
                log.warn("ServusSpeed: Timeout bei Anfrage an {}, Wiederholung {}/{}", url, retryCount, MAX_RETRIES);
                try {
                    Thread.sleep(RETRY_DELAY_MS * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Wiederholung unterbrochen", ie);
                }
            }
        }
    }

    @Override
    public List<InternetOffer> fetchOffers(Address address) {
        List<InternetOffer> offers = new ArrayList<>();

        // Auth header (Basic)
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            // Step 1: POST request for available products
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, String> addressMap = new HashMap<>();
            addressMap.put("strasse", address.street());
            addressMap.put("hausnummer", address.houseNumber());
            addressMap.put("postleitzahl", address.zip());
            addressMap.put("stadt", address.city());
            addressMap.put("land", "DE");
            requestBody.put("address", addressMap);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.info("ServusSpeed: Sende Anfrage für verfügbare Produkte an {}", BASE_URL + "/api/external/available-products");
            log.debug("ServusSpeed: Request Body: {}", requestBody);

            ResponseEntity<Map> productListResponse = executeWithRetry(
                    BASE_URL + "/api/external/available-products",
                    HttpMethod.POST,
                    request,
                    Map.class);

            log.info("ServusSpeed: Response Status: {}", productListResponse.getStatusCode());
            log.debug("ServusSpeed: Response Headers: {}", productListResponse.getHeaders());

            if (productListResponse.getBody() == null) {
                log.warn("ServusSpeed: Keine Antwort erhalten");
                return offers;
            }

            List<String> availableProducts = (List<String>) productListResponse.getBody().get("availableProducts");
            if (availableProducts == null) {
                log.warn("ServusSpeed: Keine verfügbaren Produkte gefunden");
                return offers;
            }

            log.info("ServusSpeed: {} verfügbare Produkte gefunden", availableProducts.size());

            // Step 2: POST request for each product's details
            for (String productId : availableProducts) {
                log.info("ServusSpeed: Hole Details für Produkt {}", productId);
                
                ResponseEntity<Map> detailResponse = executeWithRetry(
                        BASE_URL + "/api/external/product-details/" + productId,
                        HttpMethod.POST,
                        request,
                        Map.class);

                log.debug("ServusSpeed: Detail Response Status: {}", detailResponse.getStatusCode());

                Map details = detailResponse.getBody();
                if (details == null) {
                    log.warn("ServusSpeed: Keine Details für Produkt {} erhalten", productId);
                    continue;
                }

                Map<String, Object> servusSpeedProduct = (Map<String, Object>) details.get("servusSpeedProduct");
                if (servusSpeedProduct == null) {
                    log.warn("ServusSpeed: Keine Produktdaten für ID {} gefunden", productId);
                    continue;
                }

                String providerName = (String) servusSpeedProduct.get("providerName");
                Map<String, Object> productInfo = (Map<String, Object>) servusSpeedProduct.get("productInfo");
                Map<String, Object> pricingDetails = (Map<String, Object>) servusSpeedProduct.get("pricingDetails");
                Integer discount = (Integer) servusSpeedProduct.get("discount");

                if (productInfo == null || pricingDetails == null) {
                    log.warn("ServusSpeed: Unvollständige Produktdaten für ID {}", productId);
                    continue;
                }

                Integer speed = (Integer) productInfo.get("speed");
                Integer priceCent = (Integer) pricingDetails.get("monthlyCostInCent");
                
                String name = String.format("%s %d Mbit/s", providerName, speed);
                double finalPrice = Math.abs((priceCent - (discount != null ? discount : 0)) / 100.0);

                log.info("ServusSpeed: Angebot gefunden - {} für {}€", name, finalPrice);
                offers.add(new InternetOffer("ServusSpeed", name, finalPrice));
            }

        } catch (Exception e) {
            log.error("ServusSpeed: Fehler bei API-Aufruf: {}", e.getMessage(), e);
        }

        return offers;
    }

    @Override
    public String getProviderName() {
        return "ServusSpeed";
    }
}

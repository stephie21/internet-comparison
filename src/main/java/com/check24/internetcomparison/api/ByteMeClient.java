package com.check24.internetcomparison.api;

import com.check24.internetcomparison.model.Address;
import com.check24.internetcomparison.model.InternetOffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ByteMeClient implements ProviderClient {

    private static final Logger log = LoggerFactory.getLogger(ByteMeClient.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${providers.byteMe.apiKey}")
    private String apiKey;

    @Override
    public List<InternetOffer> fetchOffers(Address address) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("ByteMe API-Key fehlt");
            return List.of();
        }

        String url = String.format(
                "https://byteme.gendev7.check24.fun/app/api/products/data?street=%s&houseNumber=%s&city=%s&plz=%s",
                address.street(), address.houseNumber(), address.city(), address.zip());

        log.info("ByteMe API-Anfrage: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", apiKey);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("ByteMe API-Fehler: Status {}", response.getStatusCode());
                return List.of();
            }
        } catch (Exception e) {
            log.error("ByteMe API-Fehler: {}", e.getMessage(), e);
            return List.of();
        }

        List<InternetOffer> offers = new ArrayList<>();
        Set<String> seenProducts = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(response.getBody()))) {
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length >= 5) {
                    String providerName = parts[1].trim().replaceAll("\"", "");
                    String speed = parts[2].trim().replaceAll("\"", "");
                    String productName = (providerName + " " + speed).trim();

                    double price = normalizePrice(parts[3].trim());

                    if (price > 0.0 && seenProducts.add(productName)) {
                        offers.add(new InternetOffer("ByteMe", productName, price));
                        log.debug("ByteMe Angebot gefunden: {} - {}€", productName, price);
                    }
                }
            }
        } catch (Exception e) {
            log.error("ByteMe CSV-Parsing-Fehler: {}", e.getMessage(), e);
        }

        log.info("ByteMe: {} Angebote gefunden", offers.size());
        return offers;
    }

    private double normalizePrice(String priceCandidate) {
        try {
            double raw = Double.parseDouble(priceCandidate);

            if (raw > 1000) {
                return raw / 100.0;
            } else if (raw >= 10 && raw <= 500 && raw % 5 == 0) {
                return raw;
            } else if (raw >= 5.0 && raw <= 150.0) {
                return raw;
            } else {
                log.warn("ByteMe: Unklarer Preiswert: {}", raw);
                return 0.0;
            }
        } catch (NumberFormatException e) {
            log.warn("ByteMe: Fehler beim Parsen von Preis: {}", priceCandidate);
            return 0.0;
        }
    }

    @Override
    public String getProviderName() {
        return "ByteMe";
    }
}

package com.check24.internetcomparison.api;

import com.check24.internetcomparison.model.Address;
import com.check24.internetcomparison.model.InternetOffer;
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

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${providers.byteMe.apiKey}")
    private String apiKey;

    @Override
    public List<InternetOffer> fetchOffers(Address address) {
        String url = String.format(
                "https://byteme.gendev7.check24.fun/app/api/products/data?street=%s&houseNumber=%s&city=%s&plz=%s",
                address.street(), "12", address.city(), address.zip());

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", apiKey);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        } catch (Exception e) {
            System.out.println("Fehler bei ByteMe API: " + e.getMessage());
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
                    }

                }
            }
        } catch (Exception e) {
            System.out.println("Fehler beim Parsen der CSV von ByteMe: " + e.getMessage());
        }

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
                System.out.println("Unklarer Preiswert: " + raw + " → setze 0.0");
                return 0.0;
            }
        } catch (NumberFormatException e) {
            System.out.println("Fehler beim Parsen von Preis: " + priceCandidate);
            return 0.0;
        }
    }

    @Override
    public String getProviderName() {
        return "ByteMe";
    }
}

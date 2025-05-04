package com.check24.internetcomparison.api;

import com.check24.internetcomparison.model.Address;
import com.check24.internetcomparison.model.InternetOffer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class ServusSpeedClient implements ProviderClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${providers.servusSpeed.username}")
    private String username;

    @Value("${providers.servusSpeed.password}")
    private String password;


    @Override
    public List<InternetOffer> fetchOffers(Address address) {
        List<InternetOffer> offers = new ArrayList<>();
        String baseUrl = "https://servusspeed.gendev7.check24.fun/api/external";

        // Auth header (Basic)
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.set("Accept", "application/json");
        System.out.println("ServusSpeed URL: " + baseUrl);
        System.out.println("Authorization: Basic " + encodedAuth);


        // Step 1: GET available products
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/available-products")
                .queryParam("countryCode", "DE")
                .queryParam("street", address.street())
                .queryParam("houseNumber", "12")
                .queryParam("plz", address.zip())
                .queryParam("city", address.city())
                .toUriString();

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map[]> productListResponse = restTemplate.exchange(
                    url, HttpMethod.GET, request, Map[].class);

            if (productListResponse.getBody() == null) return offers;

            for (Map product : productListResponse.getBody()) {
                String productId = product.get("productId").toString();

                // Step 2: GET product details
                String detailUrl = baseUrl + "/product-details/" + productId;
                ResponseEntity<Map> detailResponse = restTemplate.exchange(
                        detailUrl, HttpMethod.GET, request, Map.class);

                Map details = detailResponse.getBody();
                if (details == null) continue;

                String name = (String) details.get("name");
                int priceCent = (int) details.getOrDefault("monthlyCostInCent", 0);
                double price = priceCent / 100.0;

                offers.add(new InternetOffer("ServusSpeed", name, price));
            }

        } catch (Exception e) {
            System.out.println("Fehler bei ServusSpeed: " + e.getMessage());
        }

        return offers;
    }

    @Override
    public String getProviderName() {
        return "ServusSpeed";
    }
}

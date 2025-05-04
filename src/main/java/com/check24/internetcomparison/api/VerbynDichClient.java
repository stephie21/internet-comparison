package com.check24.internetcomparison.api;

import com.check24.internetcomparison.model.Address;
import com.check24.internetcomparison.model.InternetOffer;
import com.check24.internetcomparison.service.ComparisonServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class VerbynDichClient implements ProviderClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger log = LoggerFactory.getLogger(ComparisonServiceImpl.class);


    @Value("${providers.verbynDich.apiKey}")
    private String apiKey;

    @Override
    public List<InternetOffer> fetchOffers(Address address) {
        String baseUrl = "https://verbyndich.gendev7.check24.fun/check24/data";
        String addressString = String.format("%s;%s;%s;%s",
                address.street(), "12", address.city(), address.zip());
        Set<String> seenProducts = new HashSet<>();
        List<InternetOffer> offers = new ArrayList<>();


        int page = 0;
        boolean hasMore = true;

        while (hasMore) {
            String url = baseUrl + "?apiKey=" + apiKey + "&page=" + page;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            HttpEntity<String> request = new HttpEntity<>(addressString, headers);

            ResponseEntity<VerbynDichResponse> response;

            try {
                 response = restTemplate.postForEntity(
                        url, request, VerbynDichResponse.class);
                VerbynDichResponse body = response.getBody();

                if (body == null) {
                    log.warn("Keine Antwort von VerbynDich (null body) auf Seite {}", page);
                    break;
                }

                String description = body.getDescription();
                if (body.valid() && seenProducts.add(body.getProduct())) {
                    double price = extractPrice(description);
                    offers.add(new InternetOffer("VerbynDich", body.getProduct(), price));
                }

                hasMore = !body.last();
                page++;

            } catch (Exception e) {
                log.error("Fehler beim Abrufen von VerbynDich: {}", e.getMessage());
                break;
            }
        }
            return offers;
    }

    @Override
    public String getProviderName() {
        return "VerbynDich";
    }
    private double extractPrice(String description) {
        if (description == null || description.isBlank()) return 0.0;

        Pattern pattern = Pattern.compile("(\\d{1,3}[,.]\\d{2}|\\d{1,3})");
        Matcher matcher = pattern.matcher(description);

        if (matcher.find()) {
            String priceStr = matcher.group(1).replace(",", ".");
            priceStr = priceStr.replaceAll("[^\\d.]", ""); // filter alles außer Zahlen & Punkt
            try {
                return Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                System.out.println("Fehler beim Parsen von Preis: " + priceStr);
            }
        } else {
            System.out.println("Kein Preis gefunden in Beschreibung: " + description);
        }

        return 0.0;
    }



    // Interne DTO-Klasse für JSON-Antwort
    public static class VerbynDichResponse {
        private String product;
        private String description;
        private boolean last;
        private boolean valid;

        public String getProduct() {
            return product;
        }

        public void setProduct(String product) {
            this.product = product;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean last() {
            return last;
        }

        public void setLast(boolean last) {
            this.last = last;
        }

        public boolean valid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }
    }
}

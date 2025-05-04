package com.check24.internetcomparison.service;

import com.check24.internetcomparison.api.ProviderClient;
import com.check24.internetcomparison.model.Address;
import com.check24.internetcomparison.model.InternetOffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ComparisonServiceImpl implements ComparisonService {

    private final List<ProviderClient> clients;
    private static final Logger log = LoggerFactory.getLogger(ComparisonServiceImpl.class);


    public ComparisonServiceImpl(List<ProviderClient> clients) {
        this.clients = clients;
    }

    @Override
    public List<InternetOffer> getOffers(String zip, String city, String street, String houseNumber) {
        Address address = new Address(zip, city, street,houseNumber);
        List<InternetOffer> allOffers = new ArrayList<>();

        for (ProviderClient client : clients) {
            try {
                log.info("Frage Angebote von {} ab...", client.getProviderName());
                List<InternetOffer> offers = client.fetchOffers(address);
                allOffers.addAll(offers);
                log.info("Empfangen von {}: {} Angebote", client.getProviderName(), offers.size());
            } catch (Exception e) {
                log.error("Fehler bei {}: {}", client.getProviderName(), e.getMessage());
            }
        }

        return allOffers;
    }

}

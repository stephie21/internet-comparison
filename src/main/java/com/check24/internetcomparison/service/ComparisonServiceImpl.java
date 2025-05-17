package com.check24.internetcomparison.service;

import com.check24.internetcomparison.api.ProviderClient;
import com.check24.internetcomparison.model.Address;
import com.check24.internetcomparison.model.InternetOffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class ComparisonServiceImpl implements ComparisonService {

    private final List<ProviderClient> clients;
    private final ExecutorService executorService;
    private final Map<String, CacheEntry> asyncCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> loadedProviderCount = new ConcurrentHashMap<>();
    private final Set<String> loadingKeys = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<String>> finishedProviders = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 5 * 60 * 1000; // 5 Minuten TTL

    private static final Logger log = LoggerFactory.getLogger(ComparisonServiceImpl.class);

    public ComparisonServiceImpl(List<ProviderClient> clients) {
        this.clients = clients;
        this.executorService = Executors.newFixedThreadPool(clients.size());
    }

    @Override
    public List<InternetOffer> getOffers(String zip, String city, String street, String houseNumber) {
        Address address = new Address(street, houseNumber, zip, city);
        String resultId = String.format("%s-%s-%s-%s", zip, city, street, houseNumber);
        log.info("[getOffers] zip={}, city={}, street={}, houseNumber={}, resultId={}", zip, city, street, houseNumber, resultId);
        // Starte asynchrones Laden im Hintergrund
        startAsyncLoading(address, resultId);
        // Gib eine leere Liste zurück, damit das Frontend nachlädt
        return Collections.emptyList();
    }

    public void startAsyncLoading(Address address, String resultId) {
        log.info("[startAsyncLoading] resultId={}, address=[street={}, houseNumber={}, zip={}, city={}]", resultId, address.street(), address.houseNumber(), address.zip(), address.city());
        loadingKeys.add(resultId);
        loadedProviderCount.put(resultId, 0);
        asyncCache.putIfAbsent(resultId, new CacheEntry(new java.util.concurrent.CopyOnWriteArrayList<>()));
        finishedProviders.put(resultId, ConcurrentHashMap.newKeySet());
        for (ProviderClient client : clients) {
            executorService.submit(() -> {
                try {
                    log.info("[ASYNC] Frage Angebote von {} ab...", client.getProviderName());
                    List<InternetOffer> offers = client.fetchOffers(address);
                    CacheEntry entry = asyncCache.get(resultId);
                    if (entry != null) {
                        entry.offers.addAll(offers);
                    }
                    log.info("{} liefert {} Angebote", client.getProviderName(), offers.size());
                } catch (Exception e) {
                    log.error("[ASYNC] Fehler bei {}: {}", client.getProviderName(), e.getMessage());
                } finally {
                    Set<String> finished = finishedProviders.get(resultId);
                    if (finished != null && finished.add(client.getProviderName())) {
                        int count = finished.size();
                        log.info("Provider {} fertig für {} (aktuell fertig: {}/{})", client.getProviderName(), resultId, count, clients.size());
                        if (count >= clients.size()) {
                            loadingKeys.remove(resultId);
                            log.info("Alle Provider fertig für {}", resultId);
                            finishedProviders.remove(resultId);
                        }
                    }
                }
            });
        }
    }

    public List<InternetOffer> getOffersBatch(String zip, String city, String street, String houseNumber, int page) {
        zip = zip.replace("\"", "");
        city = city.replace("\"", "");
        street = street.replace("\"", "");
        houseNumber = houseNumber.replace("\"", "");
        String resultId = String.format("%s-%s-%s-%s", zip, city, street, houseNumber);
        log.info("[getOffersBatch] zip={}, city={}, street={}, houseNumber={}, resultId={}", zip, city, street, houseNumber, resultId);
        CacheEntry entry = asyncCache.get(resultId);
        if (entry == null || entry.isExpired()) {
            log.info("getOffersBatch: Kein Cache für Key {}", resultId);
            return Collections.emptyList();
        }
        log.info("getOffersBatch: Key={}, Angebote={}", resultId, entry.offers.size());
        return entry.offers;
    }

    public boolean isLoading(String resultId) {
        return loadingKeys.contains(resultId);
    }

    public int getLoadedProvidersCount(String resultId) {
        return loadedProviderCount.getOrDefault(resultId, 0);
    }

    // Cache-Bereinigung alle 5 Minuten
    @Scheduled(fixedRate = 300000)
    public void cleanupCache() {
        asyncCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private static class CacheEntry {
        final List<InternetOffer> offers;
        final long timestamp;
        
        CacheEntry(List<InternetOffer> offers) {
            this.offers = (offers instanceof java.util.concurrent.CopyOnWriteArrayList)
                ? offers
                : new java.util.concurrent.CopyOnWriteArrayList<>(offers);
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL;
        }
    }
}

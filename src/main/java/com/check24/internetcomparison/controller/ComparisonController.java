package com.check24.internetcomparison.controller;

import com.check24.internetcomparison.model.Address;
import com.check24.internetcomparison.model.InternetOffer;
import com.check24.internetcomparison.service.ComparisonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Controller
    public class ComparisonController {

    private final ComparisonService comparisonService;
    private static final Logger log = LoggerFactory.getLogger(ComparisonController.class);

    public ComparisonController(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @GetMapping
    public String showForm(Model model) {
        return "compare";
    }

    @PostMapping
    public String compare(@RequestParam String zip,
                          @RequestParam String city,
                          @RequestParam String street,
                          @RequestParam String houseNumber,
                          Model model) {
        String resultId = String.format("%s-%s-%s-%s", zip, city, street, houseNumber);

        // 1. Lade synchron das erste Angebot (nur zur Anzeige – damit Nutzer sofort was sieht)
        List<InternetOffer> initialOffers = comparisonService.getOffers(zip, city, street, houseNumber);

        // 2. Async-Loading für alle Anbieter im Hintergrund starten
        Address address = new Address(street, houseNumber, zip, city);
        comparisonService.startAsyncLoading(address, resultId);

        // 3. Angebote im Cache speichern (auch wenn erst 1 vorhanden ist)
        model.addAttribute("resultId", resultId);
        model.addAttribute("offers", initialOffers);
        model.addAttribute("zip", zip);
        model.addAttribute("city", city);
        model.addAttribute("street", street);
        model.addAttribute("houseNumber", houseNumber);
        model.addAttribute("currentPage", 0);
        model.addAttribute("hasMore", true);
        model.addAttribute("totalProviders", 5);

        return "result";
    }

    @GetMapping("/compare/batch")
    @ResponseBody
    public Map<String, Object> getNextBatch(
        @RequestParam String resultId,
        @RequestParam String zip,
        @RequestParam String city,
        @RequestParam String street,
        @RequestParam String houseNumber,
        @RequestParam(defaultValue = "0") int fromIndex
    ) {
        List<InternetOffer> allOffers = comparisonService.getOffersBatch(zip, city, street, houseNumber, 0);
        int endIndex = allOffers.size();
        List<InternetOffer> batch = fromIndex < endIndex
            ? allOffers.subList(fromIndex, endIndex)
            : Collections.emptyList();

        boolean isLoading = comparisonService.isLoading(resultId);
        boolean hasMore = isLoading || endIndex > fromIndex;

        return Map.of(
            "offers", batch,
            "hasMore", hasMore,
            "currentIndex", endIndex,
            "loading", isLoading,
            "totalOffers", allOffers.size()
        );
    }

    @GetMapping("/compare/loading-status")
    @ResponseBody
    public Map<String, Object> getLoadingStatus(@RequestParam String resultId,
                                              @RequestParam String zip,
                                              @RequestParam String city,
                                              @RequestParam String street,
                                              @RequestParam String houseNumber) {
        boolean isLoading = comparisonService.isLoading(resultId);
        int loadedProviders = comparisonService.getLoadedProvidersCount(resultId);
        List<InternetOffer> allOffers = comparisonService.getOffersBatch(zip, city, street, houseNumber, 0);

        log.info("Loading Status: isLoading={}, loadedProviders={}, totalOffers={}", 
                isLoading, loadedProviders, allOffers.size());

        return Map.of(
            "loading", isLoading,
            "loadedProviders", loadedProviders,
            "totalProviders", 5,
            "totalOffers", allOffers.size()
        );
    }

    @GetMapping("/compare/filter")
    @ResponseBody
    public Map<String, Object> filterOffers(@RequestParam String resultId,
                                          @RequestParam String zip,
                                          @RequestParam String city,
                                          @RequestParam String street,
                                          @RequestParam String houseNumber,
                                          @RequestParam(required = false) Double minPrice,
                                          @RequestParam(required = false) Double maxPrice) {
        // Hole IMMER alle aktuellen Angebote aus dem Cache (fromIndex=0)
        List<InternetOffer> allOffers = comparisonService.getOffersBatch(zip, city, street, houseNumber, 0);
        log.info("Filtere {} Angebote", allOffers.size());

        // Filtere die Angebote nach Preis
        Predicate<InternetOffer> priceFilter = offer ->
                (minPrice == null || offer.getPrice() >= minPrice) &&
                        (maxPrice == null || offer.getPrice() <= maxPrice);

        List<InternetOffer> filteredOffers = allOffers.stream()
                .filter(priceFilter)
                .toList();

        log.info("Nach Filterung: {} Angebote", filteredOffers.size());

        return Map.of(
            "offers", filteredOffers,
            "hasMore", false,
            "currentPage", 0,
            "loading", comparisonService.isLoading(resultId),
            "totalOffers", filteredOffers.size()
        );
    }

    @GetMapping("/compare/stream/{resultId}")
    public SseEmitter streamOffers(@PathVariable String resultId) {
        SseEmitter emitter = new SseEmitter();

        // Starte einen neuen Thread für das Senden der Angebote
        new Thread(() -> {
            try {
                String[] parts = resultId.split("-");
                List<InternetOffer> offers = comparisonService.getOffersBatch(
                    parts[0], // zip
                    parts[1], // city
                    parts[2], // street
                    parts[3], // houseNumber
                    0
                );
                
                if (offers != null) {
                    for (InternetOffer offer : offers) {
                        emitter.send(SseEmitter.event()
                            .data(offer)
                            .build());
                        Thread.sleep(100); // Kleine Verzögerung zwischen den Angeboten
                    }
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }
}
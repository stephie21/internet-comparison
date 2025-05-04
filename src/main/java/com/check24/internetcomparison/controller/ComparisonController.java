package com.check24.internetcomparison.controller;

import com.check24.internetcomparison.model.InternetOffer;
import com.check24.internetcomparison.service.ComparisonService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Controller
@RequestMapping("/compare")
public class ComparisonController {

    private final ComparisonService comparisonService;
    private final Map<String, List<InternetOffer>> sharedResults = new ConcurrentHashMap<>();

    public ComparisonController(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @GetMapping
    public String showForm(Model model) {
        model.addAttribute("offers", null);
        return "compare"; // templates/compare.html
    }

    @PostMapping
    public String compare(@RequestParam String zip,
                          @RequestParam String city,
                          @RequestParam String street,
                          @RequestParam String houseNumber,
                          Model model) {

        List<InternetOffer> offers = comparisonService.getOffers(zip, city, street,houseNumber);
        offers.sort(Comparator.comparingDouble(InternetOffer::getPrice));

        String uuid = UUID.randomUUID().toString();
        sharedResults.put(uuid, offers);

        model.addAttribute("offers", offers);
        model.addAttribute("shareLink", "/compare/shared/" + uuid);

        return "result"; // templates/compare.html
    }

    @GetMapping("/shared/{id}")
    public String getShared(@PathVariable String id, Model model) {
        List<InternetOffer> offers = sharedResults.get(id);
        if (offers == null) {
            model.addAttribute("error", "Kein geteiltes Ergebnis gefunden.");
            return "error";
        }
        offers.sort(Comparator.comparingDouble(InternetOffer::getPrice));
        model.addAttribute("offers", offers);
        return "compare";
    }
}
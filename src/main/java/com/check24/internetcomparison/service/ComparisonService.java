package com.check24.internetcomparison.service;

import com.check24.internetcomparison.model.InternetOffer;
import java.util.List;

public interface ComparisonService {
    List<InternetOffer> getOffers(String zip, String city, String street, String hauseNumber);
}

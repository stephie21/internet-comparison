package com.check24.internetcomparison.service;

import com.check24.internetcomparison.model.Address;
import com.check24.internetcomparison.model.InternetOffer;
import java.util.List;

public interface ComparisonService {
    List<InternetOffer> getOffers(String zip, String city, String street, String houseNumber);
    List<InternetOffer> getOffersBatch(String zip, String city, String street, String houseNumber, int page);
    boolean isLoading(String cacheKey);
    int getLoadedProvidersCount(String cacheKey);
    void startAsyncLoading(Address address, String resultId);

}

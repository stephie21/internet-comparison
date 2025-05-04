package com.check24.internetcomparison.api;

import com.check24.internetcomparison.model.Address;
import com.check24.internetcomparison.model.InternetOffer;

import java.util.List;

public interface ProviderClient {
    List<InternetOffer> fetchOffers(Address address);
    String getProviderName();
}

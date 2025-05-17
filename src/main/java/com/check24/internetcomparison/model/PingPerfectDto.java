package com.check24.internetcomparison.model;

public class PingPerfectDto {

    public record CompareProductsRequestData(
            String street,
            String plz,
            String houseNumber,
            String city,
            boolean wantsFiber
    ) {}

    public record PricingDetails(
            int monthlyCostInCent,
            String installationService
    ) {}

    public record ProductInfo(
            int speed,
            int contractDurationInMonths,
            String connectionType,
            String tv,
            Integer limitFrom,
            Integer maxAge
    ) {}

    public record InternetProduct(
            String providerName,
            ProductInfo productInfo,
            PricingDetails pricingDetails
    ) {}

}

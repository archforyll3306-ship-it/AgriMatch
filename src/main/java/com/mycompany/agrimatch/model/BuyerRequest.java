package com.mycompany.agrimatch.model;

public record BuyerRequest(int id, int buyerId, int cropId, String cropName,
                           double quantity, Double maxPricePerKg, String status) {}

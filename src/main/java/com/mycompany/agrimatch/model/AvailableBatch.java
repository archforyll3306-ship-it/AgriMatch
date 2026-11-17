package com.mycompany.agrimatch.model;

import java.time.LocalDate;

/** One row of v_available_batches. */
public record AvailableBatch(int batchId, int farmerId, int cropId, String cropName,
                             double pricePerKg, LocalDate harvestDate, LocalDate expiryDate,
                             int daysLeft, double remainingQty) {}

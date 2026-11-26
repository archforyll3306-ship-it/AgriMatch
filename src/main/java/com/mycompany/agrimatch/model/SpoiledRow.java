package com.mycompany.agrimatch.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A batch that went SPOILED, with how much of it was sold before it did. */
public record SpoiledRow(int batchId, String farmer, String crop, BigDecimal quantity,
                         BigDecimal soldQty, BigDecimal pricePerKg, LocalDate expiry) {

    /** Kg that were never sold: the harvest minus what reached COMPLETED sales. */
    public BigDecimal lostQty() {
        return quantity.subtract(soldQty);
    }

    /** What the lost kg would have been worth at the farmer's asking price. */
    public BigDecimal lostValue() {
        return lostQty().multiply(pricePerKg);
    }
}

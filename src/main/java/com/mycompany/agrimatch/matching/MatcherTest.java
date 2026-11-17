package com.mycompany.agrimatch.matching;

import com.mycompany.agrimatch.model.*;
import java.time.LocalDate;
import java.util.*;

public class MatcherTest {
    public static void main(String[] args) {
        Buyer buyer = new Buyer(1, "Sample Buyer One", "0917", "Matina", 7.06, 125.57);
        BuyerRequest req = new BuyerRequest(1, 1, 1, "Tomato", 50.0, 50.0, "OPEN");

        Map<Integer, Farmer> farmers = new HashMap<>();
        farmers.put(1, new Farmer(1, "Farmer One",   "0", "Calinan", 7.19,  125.455));
        farmers.put(2, new Farmer(2, "Farmer Two",   "1", "Tugbok",  7.11,  125.50));
        farmers.put(3, new Farmer(3, "Farmer Three", "2", "Toril",   7.015, 125.495));

        LocalDate today = LocalDate.now();
        List<AvailableBatch> batches = List.of(
            new AvailableBatch(1, 1, 1, "Tomato",  45.0, today.minusDays(3), today.plusDays(4), 4, 100.0),
            new AvailableBatch(4, 3, 1, "Tomato",  42.0, today.minusDays(6), today.plusDays(1), 1,  60.0),
            new AvailableBatch(5, 2, 1, "Tomato",  48.0, today.minusDays(1), today.plusDays(6), 6,  90.0),
            new AvailableBatch(6, 2, 1, "Tomato",  55.0, today.minusDays(1), today.plusDays(6), 6,  90.0), // too expensive
            new AvailableBatch(2, 2, 4, "Cabbage", 35.0, today.minusDays(4), today.plusDays(10), 10, 80.0) // wrong crop
        );

        for (MatchResult r : Matcher.rank(req, buyer, batches, farmers)) {
            System.out.printf("Batch %d | %s | P%.2f | %.1f km | days left %d | urgency %.2f | dist %.2f | price %.2f | TOTAL %.3f | suggest %.0f kg%n",
                r.batch().batchId(), r.farmer().name(), r.batch().pricePerKg(), r.distanceKm(),
                r.batch().daysLeft(), r.urgency(), r.distanceScore(), r.priceScore(), r.total(), r.suggestedQty());
        }
    }
}
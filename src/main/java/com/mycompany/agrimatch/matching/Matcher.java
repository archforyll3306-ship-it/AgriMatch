package com.mycompany.agrimatch.matching;

import com.mycompany.agrimatch.model.*;
import java.util.*;

/** Finds and ranks the batches that can fill a buyer request. */
public final class Matcher {

    private Matcher() {}

    public static List<MatchResult> rank(BuyerRequest request, Buyer buyer,
                                         List<AvailableBatch> batches,
                                         Map<Integer, Farmer> farmersById) {
        List<MatchResult> results = new ArrayList<>();

        for (AvailableBatch b : batches) {
            // ---- filters: a batch must pass ALL of these ----
            if (b.cropId() != request.cropId()) continue;          // wrong crop
            if (b.remainingQty() <= 0) continue;                   // nothing left
            if (b.daysLeft() < 0) continue;                        // already expired
            if (request.maxPricePerKg() != null
                    && b.pricePerKg() > request.maxPricePerKg()) continue; // too expensive

            // ---- distance (needs coordinates on both sides) ----
            Farmer farmer = farmersById.get(b.farmerId());
            Double km = null;
            double distanceScore = 0.0;
            if (farmer != null && farmer.lat() != null && farmer.lon() != null
                    && buyer.lat() != null && buyer.lon() != null) {
                km = DistanceCalculator.km(farmer.lat(), farmer.lon(), buyer.lat(), buyer.lon());
                distanceScore = MatchScorer.distanceScore(km);
            }

            // ---- the other two scores ----
            int shelfLife = (int) java.time.temporal.ChronoUnit.DAYS.between(b.harvestDate(), b.expiryDate());
            double urgency    = UrgencyScorer.score(b.daysLeft(), shelfLife);
            double priceScore = MatchScorer.priceScore(b.pricePerKg(), request.maxPricePerKg());
            double total      = MatchScorer.total(urgency, distanceScore, priceScore);

            double qty = Math.min(request.quantity(), b.remainingQty());
            results.add(new MatchResult(b, farmer, km, urgency, distanceScore, priceScore, total, qty));
        }

        // best score first; if tied, the one closer to expiry goes first
        results.sort(Comparator.comparingDouble(MatchResult::total).reversed()
                .thenComparingInt(r -> r.batch().daysLeft()));
        return results;
    }
}

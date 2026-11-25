package com.mycompany.agrimatch.matching;

import com.mycompany.agrimatch.db.Repository;
import com.mycompany.agrimatch.model.*;
import java.util.*;

/** Reads everything from the database and prints ranked matches for every open request. */
public class MatchRunner {

    public static void main(String[] args) throws Exception {
        // 1. load data from MariaDB
        List<BuyerRequest> requests = Repository.openRequests();
        List<AvailableBatch> batches = Repository.availableBatches();

        Map<Integer, Farmer> farmers = new HashMap<>();
        for (Farmer f : Repository.farmers()) farmers.put(f.id(), f);

        Map<Integer, Buyer> buyers = new HashMap<>();
        for (Buyer b : Repository.buyers()) buyers.put(b.id(), b);

        // 2. match each open request
        for (BuyerRequest req : requests) {
            Buyer buyer = buyers.get(req.buyerId());
            System.out.printf("%nRequest #%d: %s wants %.0f kg %s (max P%.2f/kg)%n",
                    req.id(), buyer.name(), req.quantity(), req.cropName(),
                    req.maxPricePerKg() == null ? 0.0 : req.maxPricePerKg());

            List<MatchResult> matches = Matcher.rank(req, buyer, batches, farmers);
            if (matches.isEmpty()) {
                System.out.println("   No matching batches.");
                continue;
            }
            int rank = 1;
            for (MatchResult m : matches) {
                System.out.printf("   %d. Batch #%d from %s | P%.2f/kg | %s km | %d days left | score %.3f | suggest %.0f kg%n",
                        rank++, m.batch().batchId(), m.farmer().name(), m.batch().pricePerKg(),
                        m.distanceKm() == null ? "?" : String.format("%.1f", m.distanceKm()),
                        m.batch().daysLeft(), m.total(), m.suggestedQty());
            }
        }
    }
}

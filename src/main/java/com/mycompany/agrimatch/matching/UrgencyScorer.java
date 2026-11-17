package com.mycompany.agrimatch.matching;

/** Scores how urgently a batch must be sold: 0 = just harvested, 1 = about to spoil. */
public final class UrgencyScorer {

    private UrgencyScorer() {}

    public static double score(int daysLeft, int shelfLifeDays) {
        if (shelfLifeDays <= 0) return 1.0;           // bad data: treat as most urgent
        double urgency = 1.0 - ((double) daysLeft / shelfLifeDays);
        return Math.max(0.0, Math.min(1.0, urgency)); // keep the result between 0 and 1
    }
}
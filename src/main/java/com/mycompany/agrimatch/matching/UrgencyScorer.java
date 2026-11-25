package com.mycompany.agrimatch.matching;

public final class UrgencyScorer {
    private UrgencyScorer() {}
    public static double score(int daysLeft, int shelfLifeDays) {
        if (shelfLifeDays <= 0) return 1.0;
        double urgency = 1.0 - ((double) daysLeft / shelfLifeDays);
        return Math.max(0.0, Math.min(1.0, urgency));
    }
}

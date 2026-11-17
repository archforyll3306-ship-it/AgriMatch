package com.mycompany.agrimatch.matching;

/** Turns distance, urgency and price into scores from 0 to 1, then combines them. */
public final class MatchScorer {

    // Weights: must add up to 1.0
    public static final double W_URGENCY  = 0.5;
    public static final double W_DISTANCE = 0.3;
    public static final double W_PRICE    = 0.2;

    // Distances at or beyond this get a distance score of 0
    public static final double MAX_DISTANCE_KM = 50.0;

    private MatchScorer() {}

    /** 1 = same place, 0 = MAX_DISTANCE_KM or further. */
    public static double distanceScore(double km) {
        double s = 1.0 - (km / MAX_DISTANCE_KM);
        return clamp(s);
    }

    /** The cheaper the batch is compared with the buyer's limit, the higher the score. */
    public static double priceScore(double pricePerKg, Double maxPricePerKg) {
        if (maxPricePerKg == null || maxPricePerKg <= 0) return 0.5; // no limit given: neutral
        return clamp((maxPricePerKg - pricePerKg) / maxPricePerKg);
    }

    /** Weighted total, between 0 and 1. */
    public static double total(double urgency, double distance, double price) {
        return W_URGENCY * urgency + W_DISTANCE * distance + W_PRICE * price;
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
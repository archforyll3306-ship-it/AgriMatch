package com.mycompany.agrimatch.matching;

public class ScorerTest {
    public static void main(String[] args) {
        double km = DistanceCalculator.km(7.19, 125.455, 7.06, 125.57);   // Calinan -> Matina
        double urgency  = UrgencyScorer.score(4, 7);                       // tomato
        double distance = MatchScorer.distanceScore(km);
        double price    = MatchScorer.priceScore(45.0, 50.0);              // farmer 45, buyer max 50
        System.out.printf("Distance: %.2f km -> score %.2f%n", km, distance);
        System.out.printf("Urgency score: %.2f%n", urgency);
        System.out.printf("Price score:   %.2f%n", price);
        System.out.printf("TOTAL:         %.2f%n", MatchScorer.total(urgency, distance, price));
        System.out.printf("Weights add up to: %.1f%n",
                MatchScorer.W_URGENCY + MatchScorer.W_DISTANCE + MatchScorer.W_PRICE);
    }
}
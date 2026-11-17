package com.mycompany.agrimatch.matching;

public class UrgencyTest {
    public static void main(String[] args) {
        System.out.printf("Tomato  (4 of 7 days left):   %.2f%n", UrgencyScorer.score(4, 7));
        System.out.printf("Cabbage (10 of 14 days left): %.2f%n", UrgencyScorer.score(10, 14));
        System.out.printf("Carrot  (16 of 21 days left): %.2f%n", UrgencyScorer.score(16, 21));
        System.out.printf("Fresh   (7 of 7 days left):   %.2f%n", UrgencyScorer.score(7, 7));
        System.out.printf("Expired (0 days left):        %.2f%n", UrgencyScorer.score(0, 7));
    }
}
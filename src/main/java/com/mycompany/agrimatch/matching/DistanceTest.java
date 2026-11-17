package com.mycompany.agrimatch.matching;

public class DistanceTest {
    public static void main(String[] args) {
        System.out.printf("Calinan -> Matina:  %.2f km%n", DistanceCalculator.km(7.19, 125.455, 7.06, 125.57));
        System.out.printf("Tugbok -> Buhangin: %.2f km%n", DistanceCalculator.km(7.11, 125.50, 7.10, 125.57));
        System.out.printf("Toril -> Bangkal:   %.2f km%n", DistanceCalculator.km(7.015, 125.495, 7.05, 125.55));
        System.out.printf("Same point:         %.2f km%n", DistanceCalculator.km(7.1, 125.5, 7.1, 125.5));
    }
}
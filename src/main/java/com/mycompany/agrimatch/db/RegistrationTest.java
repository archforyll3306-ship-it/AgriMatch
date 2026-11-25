package com.mycompany.agrimatch.db;

public class RegistrationTest {

    static void tryFarmer(String name, String number, int barangayId) throws Exception {
        try {
            int id = RegistrationService.addFarmer(name, number, barangayId);
            System.out.println("Farmer #" + id + " added.");
        } catch (IllegalStateException e) {
            System.out.println("Rejected: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Barangays loaded: " + Repository.barangays().size());
        System.out.println("Crops loaded: " + Repository.crops().size());

        tryFarmer("Test Farmer", "09171234567", 1);          // should work
        tryFarmer("Another Farmer", "0917 123 4567", 1);     // same number with spaces: rejected
        tryFarmer("Bad Number", "abc", 1);                   // not digits: rejected
        tryFarmer("   ", "09191234567", 1);                  // empty name: rejected
        tryFarmer("Nowhere", "09201234567", 999);            // barangay does not exist: rejected

        try {
            int bid = RegistrationService.addBuyer("Test Buyer", "09181234567", 4);
            System.out.println("Buyer #" + bid + " added.");
        } catch (IllegalStateException e) {
            System.out.println("Rejected: " + e.getMessage());
        }
    }
}

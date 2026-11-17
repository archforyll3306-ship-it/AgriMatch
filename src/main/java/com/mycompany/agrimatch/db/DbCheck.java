package com.mycompany.agrimatch.db;

/** Run this once to confirm the app can reach the database and read the data. */
public class DbCheck {
    public static void main(String[] args) throws Exception {
        System.out.println("Farmers:  " + Repository.farmers());
        System.out.println("Buyers:   " + Repository.buyers());
        System.out.println("Batches:  " + Repository.availableBatches());
        System.out.println("Requests: " + Repository.openRequests());
    }
}

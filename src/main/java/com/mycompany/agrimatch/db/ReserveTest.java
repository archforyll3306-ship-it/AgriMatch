package com.mycompany.agrimatch.db;

import com.mycompany.agrimatch.model.AvailableBatch;

public class ReserveTest {
    public static void main(String[] args) throws Exception {
        try {
            int id = ReservationService.reserve(1, 1, 50.0, 45.0, 19.23, 0.419);
            System.out.println("Reservation #" + id + " created.");
        } catch (IllegalStateException e) {
            System.out.println("Rejected: " + e.getMessage());
        }
        for (AvailableBatch b : Repository.availableBatches())
            if (b.batchId() == 1) System.out.println("Batch 1 kg still free: " + b.remainingQty());
        System.out.println("Open requests now: " + Repository.openRequests().size());
    }
}
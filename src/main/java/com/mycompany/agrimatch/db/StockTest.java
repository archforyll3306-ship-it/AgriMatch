package com.mycompany.agrimatch.db;

import com.mycompany.agrimatch.model.AvailableBatch;

public class StockTest {
    static void showBatch1(String label) throws Exception {
        for (AvailableBatch b : Repository.availableBatches())
            if (b.batchId() == 1) System.out.println(label + b.remainingQty());
    }

    public static void main(String[] args) throws Exception {
        int rid = ReservationService.reserve(1, 1, 50.0, 45.0, 19.23, 0.419);
        System.out.println("Reservation #" + rid + " created.");
        showBatch1("Batch 1 kg free after reserving:  ");

        StockService.cancel(rid);
        System.out.println("Reservation #" + rid + " cancelled.");
        showBatch1("Batch 1 kg free after cancelling: ");
        System.out.println("Open requests: " + Repository.openRequests().size());

        System.out.println("Batches marked spoiled: " + StockService.markSpoiled());
    }
}

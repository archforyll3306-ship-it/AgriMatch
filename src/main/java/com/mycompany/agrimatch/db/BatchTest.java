package com.mycompany.agrimatch.db;

import java.time.LocalDate;

/** Run this file once: it adds one batch and one request, then tries bad input. */
public class BatchTest {
    public static void main(String[] args) throws Exception {
        int batchId = BatchService.addBatch(1, 1, 80, 45, LocalDate.now().minusDays(2));
        System.out.println("Added batch #" + batchId);

        int reqId = RequestService.addRequest(1, 1, 30, 50.0);
        System.out.println("Added request #" + reqId);

        try {
            BatchService.addBatch(1, 1, -5, 45, LocalDate.now());
        } catch (IllegalStateException e) {
            System.out.println("Rejected (expected): " + e.getMessage());
        }
        try {
            BatchService.addBatch(1, 1, 10, 45, LocalDate.now().minusDays(30));
        } catch (IllegalStateException e) {
            System.out.println("Rejected (expected): " + e.getMessage());
        }

        RequestService.cancelRequest(reqId);
        System.out.println("Cancelled request #" + reqId);
        try {
            RequestService.cancelRequest(reqId);
        } catch (IllegalStateException e) {
            System.out.println("Rejected (expected): " + e.getMessage());
        }
    }
}
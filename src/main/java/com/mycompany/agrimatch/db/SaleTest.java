package com.mycompany.agrimatch.db;

public class SaleTest {
    public static void main(String[] args) throws Exception {
        try {
            int rid = ReservationService.reserve(1, 1, 50.0, 45.0, 19.23, 0.419);
            System.out.println("Reservation #" + rid + " created.");

            SaleService.confirm(rid);
            System.out.println("Reservation #" + rid + " confirmed.");

            int tid = SaleService.complete(rid, 44.00);   // buyer and farmer settled at P44/kg
            System.out.println("Sale recorded: transaction #" + tid + " (50 kg x P44.00 = P2200.00)");
        } catch (IllegalStateException e) {
            System.out.println("Rejected: " + e.getMessage());
        }
    }
}

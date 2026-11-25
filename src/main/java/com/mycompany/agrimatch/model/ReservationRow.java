package com.mycompany.agrimatch.model;
public record ReservationRow(int id, int requestId, int batchId, String buyer, String farmer, String crop,
                             double qty, double price, Double km, String status) {}
package com.mycompany.agrimatch.model;
public record RequestRow(int id, String buyer, String crop, double qty, Double maxPrice, String status, double reserved) {}
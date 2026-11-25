package com.mycompany.agrimatch.model;

/** One row of the barangays lookup table. */
public record Barangay(int id, String name, double lat, double lon) {}

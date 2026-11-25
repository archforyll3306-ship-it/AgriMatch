package com.mycompany.agrimatch.model;
import java.time.LocalDate;
public record BatchRow(int id, String farmer, String crop, double qty, double price, LocalDate harvest, String status) {}
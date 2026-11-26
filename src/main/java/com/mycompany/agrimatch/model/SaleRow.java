package com.mycompany.agrimatch.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One completed sale, already joined with the names the Reports screen needs. */
public record SaleRow(int id, LocalDateTime date, String buyer, String farmer, String crop,
                      BigDecimal qty, BigDecimal price, BigDecimal total) {}

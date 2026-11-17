package com.mycompany.agrimatch.matching;

import com.mycompany.agrimatch.model.AvailableBatch;
import com.mycompany.agrimatch.model.Farmer;

/** One ranked suggestion: a batch that could fill a buyer's request. */
public record MatchResult(AvailableBatch batch, Farmer farmer, Double distanceKm,
                          double urgency, double distanceScore, double priceScore,
                          double total, double suggestedQty) {}
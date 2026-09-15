package com.kopa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableEntity {
    private String id;
    private String tableNumber;
    private int capacity;
    private String type; // Window Table, Couple Table, Standard Table, Group Table, Outdoor Table, Counter Seat
    private String location; // Window Bay, Main Hall, Bar Counter, Garden Terrace
    private String status; // AVAILABLE, RESERVED, MAINTENANCE
    private String description;
    private int positionX;
    private int positionY;
    private String shape; // circle, square, rect
}

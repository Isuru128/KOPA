package com.kopa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tables")
public class TableEntity {
    @Id
    private String id;
    
    @Indexed(unique = true)
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

package com.kopa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {
    @Id
    private String id;
    
    private String name;
    private String description;
    
    @Indexed
    private String category;
    
    private double price;
    private String image;
    private boolean available;
    private String dietary;
    private List<String> sizeOptions;
    private List<String> milkOptions;
    private List<String> extraOptions;
}

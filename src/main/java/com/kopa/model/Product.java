package com.kopa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String id;
    private String name;
    private String description;
    private String category;
    private double price;
    private String image;
    private boolean available;
    private String dietary;
    private List<String> sizeOptions;
    private List<String> milkOptions;
    private List<String> extraOptions;
}

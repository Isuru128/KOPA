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
public class OrderItem {
    private String productId;
    private String productName;
    private Double unitPrice;
    private Integer quantity;
    private String size;
    private String milk;
    private List<String> extras;
    private Double subtotal;
}

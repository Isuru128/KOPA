package com.kopa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {
    private String id;
    private String reservationId; // e.g. KOPA-2026-00482
    private String userId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String tableId;
    private String tableNumber;
    private String tableType;
    private String tableLocation;
    private String date; // YYYY-MM-DD
    private String startTime; // e.g. "19:00" or "7:00 PM"
    private String endTime; // e.g. "20:30" or "8:30 PM"
    private Integer durationMinutes; // standard 90 min
    private Integer guestCount;
    private List<OrderItem> orderItems;
    private Double totalAmount;
    private String status; // UPCOMING, CONFIRMED, PREPARING, COMPLETED, CANCELLED
    private String specialNotes;
    private String qrCode;
    private LocalDateTime createdAt;
}

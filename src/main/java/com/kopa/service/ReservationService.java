package com.kopa.service;

import com.kopa.model.OrderItem;
import com.kopa.model.Reservation;
import com.kopa.model.TableEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final TableService tableService;
    private final Map<String, Reservation> reservationMap = new ConcurrentHashMap<>();
    private final AtomicInteger sequenceCounter = new AtomicInteger(482);

    public ReservationService(TableService tableService) {
        this.tableService = tableService;
    }

    @PostConstruct
    public void init() {
        seedSampleReservations();
    }

    private void seedSampleReservations() {
        LocalDate today = LocalDate.now();
        String dateStr = today.plusDays(1).toString();

        Reservation sample1 = Reservation.builder()
            .id("res-init-1")
            .reservationId("KOPA-2026-00482")
            .userId("usr-demo-1")
            .customerName("Alexander Vance")
            .customerEmail("guest@kopa.coffee")
            .customerPhone("+94 77 123 4567")
            .tableId("tbl-08")
            .tableNumber("08")
            .tableType("Window Table")
            .tableLocation("Window Bay")
            .date(dateStr)
            .startTime("19:00")
            .endTime("20:30")
            .durationMinutes(90)
            .guestCount(2)
            .orderItems(List.of(
                OrderItem.builder()
                    .productId("prod-latte-sig")
                    .productName("KOPA Signature Latte")
                    .unitPrice(4.50)
                    .quantity(2)
                    .size("Medium")
                    .milk("Oat")
                    .extras(List.of("Caramel finish"))
                    .subtotal(9.00)
                    .build(),
                OrderItem.builder()
                    .productId("prod-pain-chocolat")
                    .productName("Pain au Chocolat")
                    .unitPrice(3.80)
                    .quantity(1)
                    .size("Single")
                    .milk("None")
                    .extras(List.of("Warm up"))
                    .subtotal(3.80)
                    .build()
            ))
            .totalAmount(12.80)
            .status("CONFIRMED")
            .specialNotes("Window seat requested for evening celebration.")
            .qrCode("KOPA-RESERVATION|KOPA-2026-00482|Table 08|" + dateStr + " 19:00|Guests: 2")
            .createdAt(LocalDateTime.now().minusHours(2))
            .build();

        reservationMap.put(sample1.getId(), sample1);
        reservationMap.put(sample1.getReservationId(), sample1);

        // Pre-reserve table 02 on today afternoon to demonstrate real busy tables
        Reservation sample2 = Reservation.builder()
            .id("res-init-2")
            .reservationId("KOPA-2026-00480")
            .userId("usr-demo-2")
            .customerName("Elena Rostova")
            .customerEmail("elena@example.com")
            .customerPhone("+94 71 987 6543")
            .tableId("tbl-02")
            .tableNumber("02")
            .tableType("Window Table")
            .tableLocation("Window Bay")
            .date(today.toString())
            .startTime("15:00")
            .endTime("16:30")
            .durationMinutes(90)
            .guestCount(4)
            .orderItems(Collections.emptyList())
            .totalAmount(0.0)
            .status("PREPARING")
            .qrCode("KOPA-RESERVATION|KOPA-2026-00480|Table 02|" + today + " 15:00|Guests: 4")
            .createdAt(LocalDateTime.now().minusDays(1))
            .build();

        reservationMap.put(sample2.getId(), sample2);
        reservationMap.put(sample2.getReservationId(), sample2);
    }

    public List<TableAvailabilityDTO> getTableAvailability(String date, String startTime, int guestCount) {
        List<TableEntity> allTables = tableService.getAllTables();
        LocalTime requestedStart = parseTime(startTime);
        LocalTime requestedEnd = requestedStart.plusMinutes(90);

        List<Reservation> activeReservationsOnDate = reservationMap.values().stream()
            .filter(r -> r.getDate() != null && r.getDate().equals(date))
            .filter(r -> !"CANCELLED".equalsIgnoreCase(r.getStatus()))
            .collect(Collectors.toList());

        List<TableAvailabilityDTO> result = new ArrayList<>();

        for (TableEntity table : allTables) {
            String computedStatus = "AVAILABLE";
            String note = "Available to reserve";

            if ("MAINTENANCE".equalsIgnoreCase(table.getStatus())) {
                computedStatus = "MAINTENANCE";
                note = "Currently under maintenance";
            } else if (table.getCapacity() < guestCount) {
                computedStatus = "UNSUITABLE";
                note = "Max capacity " + table.getCapacity() + " guests (requested " + guestCount + ")";
            } else {
                // Check if booked by any overlapping reservation
                boolean hasOverlap = activeReservationsOnDate.stream()
                    .filter(r -> r.getTableId().equalsIgnoreCase(table.getId()))
                    .anyMatch(r -> isTimeOverlap(requestedStart, requestedEnd, parseTime(r.getStartTime()), parseTime(r.getEndTime())));

                if (hasOverlap) {
                    computedStatus = "RESERVED";
                    note = "Reserved for this time slot";
                }
            }

            result.add(new TableAvailabilityDTO(
                table.getId(),
                table.getTableNumber(),
                table.getCapacity(),
                table.getType(),
                table.getLocation(),
                computedStatus,
                table.getDescription(),
                table.getPositionX(),
                table.getPositionY(),
                table.getShape(),
                note
            ));
        }

        return result;
    }

    public Reservation createReservation(Reservation request) {
        String internalId = UUID.randomUUID().toString();
        int seq = sequenceCounter.incrementAndGet();
        String formattedCode = String.format("KOPA-2026-%05d", seq);

        String start = request.getStartTime() != null ? request.getStartTime() : "18:00";
        LocalTime sTime = parseTime(start);
        LocalTime eTime = sTime.plusMinutes(90);
        String end = eTime.format(DateTimeFormatter.ofPattern("HH:mm"));

        double computedTotal = 0.0;
        if (request.getOrderItems() != null) {
            for (OrderItem item : request.getOrderItems()) {
                if (item.getSubtotal() != null) {
                    computedTotal += item.getSubtotal();
                } else if (item.getUnitPrice() != null && item.getQuantity() != null) {
                    computedTotal += item.getUnitPrice() * item.getQuantity();
                }
            }
        }

        if (request.getTotalAmount() != null && request.getTotalAmount() > 0) {
            computedTotal = request.getTotalAmount();
        }

        int guests = (request.getGuestCount() != null && request.getGuestCount() > 0) ? request.getGuestCount() : 2;

        TableEntity table = tableService.getTableById(request.getTableId())
            .orElseGet(() -> tableService.getAllTables().get(0));

        Reservation res = Reservation.builder()
            .id(internalId)
            .reservationId(formattedCode)
            .userId(request.getUserId() != null ? request.getUserId() : "guest-user")
            .customerName(request.getCustomerName() != null ? request.getCustomerName() : "KOPA Guest")
            .customerEmail(request.getCustomerEmail() != null ? request.getCustomerEmail() : "guest@kopa.coffee")
            .customerPhone(request.getCustomerPhone() != null ? request.getCustomerPhone() : "+94 77 000 0000")
            .tableId(table.getId())
            .tableNumber(table.getTableNumber())
            .tableType(table.getType())
            .tableLocation(table.getLocation())
            .date(request.getDate())
            .startTime(start)
            .endTime(end)
            .durationMinutes(90)
            .guestCount(guests)
            .orderItems(request.getOrderItems() != null ? request.getOrderItems() : Collections.emptyList())
            .totalAmount(computedTotal)
            .status("CONFIRMED")
            .specialNotes(request.getSpecialNotes())
            .qrCode("KOPA-RESERVATION|" + formattedCode + "|Table " + table.getTableNumber() + "|" + request.getDate() + " " + start + "|Total: $" + String.format("%.2f", computedTotal))
            .createdAt(LocalDateTime.now())
            .build();

        reservationMap.put(res.getId(), res);
        reservationMap.put(res.getReservationId(), res);

        return res;
    }

    public List<Reservation> getAllReservations() {
        // Return unique list by internal id
        return reservationMap.values().stream()
            .filter(r -> r.getId() != null)
            .distinct()
            .sorted(Comparator.comparing(Reservation::getCreatedAt).reversed())
            .collect(Collectors.toList());
    }

    public Optional<Reservation> getReservationById(String idOrCode) {
        return Optional.ofNullable(reservationMap.get(idOrCode));
    }

    public List<Reservation> getReservationsByEmail(String email) {
        if (email == null || email.isBlank()) return Collections.emptyList();
        return reservationMap.values().stream()
            .filter(r -> email.equalsIgnoreCase(r.getCustomerEmail()))
            .distinct()
            .sorted(Comparator.comparing(Reservation::getCreatedAt).reversed())
            .collect(Collectors.toList());
    }

    public Optional<Reservation> cancelReservation(String idOrCode) {
        Optional<Reservation> opt = getReservationById(idOrCode);
        opt.ifPresent(r -> r.setStatus("CANCELLED"));
        return opt;
    }

    public Optional<Reservation> updateStatus(String idOrCode, String newStatus) {
        Optional<Reservation> opt = getReservationById(idOrCode);
        opt.ifPresent(r -> r.setStatus(newStatus.toUpperCase()));
        return opt;
    }

    private LocalTime parseTime(String timeStr) {
        try {
            if (timeStr == null || timeStr.isBlank()) return LocalTime.of(18, 0);
            if (timeStr.contains("AM") || timeStr.contains("PM")) {
                DateTimeFormatter f = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
                return LocalTime.parse(timeStr.toUpperCase().trim(), f);
            }
            if (timeStr.length() == 4 && timeStr.indexOf(':') == 1) {
                timeStr = "0" + timeStr;
            }
            return LocalTime.parse(timeStr);
        } catch (Exception e) {
            return LocalTime.of(18, 0);
        }
    }

    private boolean isTimeOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    public record TableAvailabilityDTO(
        String id,
        String tableNumber,
        int capacity,
        String type,
        String location,
        String status, // AVAILABLE, RESERVED, MAINTENANCE, UNSUITABLE
        String description,
        int positionX,
        int positionY,
        String shape,
        String note
    ) {}
}

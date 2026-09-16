package com.kopa.service;

import com.kopa.model.OrderItem;
import com.kopa.model.Reservation;
import com.kopa.model.TableEntity;
import com.kopa.repository.ReservationRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
    private final TableService tableService;
    private final ReservationRepository reservationRepository;
    private final Map<String, Reservation> fallbackMap = new ConcurrentHashMap<>();
    private final AtomicInteger sequenceCounter = new AtomicInteger(482);

    public ReservationService(TableService tableService, ReservationRepository reservationRepository) {
        this.tableService = tableService;
        this.reservationRepository = reservationRepository;
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
            .tableType("Group Table")
            .tableLocation("Library Corner")
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

        fallbackMap.put(sample1.getId(), sample1);
        fallbackMap.put(sample1.getReservationId(), sample1);
        fallbackMap.put(sample2.getId(), sample2);
        fallbackMap.put(sample2.getReservationId(), sample2);

        try {
            if (reservationRepository.count() == 0) {
                reservationRepository.save(sample1);
                reservationRepository.save(sample2);
                log.info("Successfully seeded sample reservations into MongoDB Atlas");
            }
        } catch (Exception e) {
            log.warn("Could not seed reservations to MongoDB Atlas (check MONGODB_URI in .env): {}", e.getMessage());
        }
    }

    public List<TableAvailabilityDTO> getTableAvailability(String date, String startTime, int guestCount) {
        List<TableEntity> allTables = tableService.getAllTables();
        LocalTime requestedStart = parseTime(startTime);
        LocalTime requestedEnd = requestedStart.plusMinutes(90);

        List<Reservation> activeReservationsOnDate = getActiveReservationsByDate(date);

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
                boolean hasOverlap = activeReservationsOnDate.stream()
                    .filter(r -> (r.getTableId() != null && r.getTableId().equalsIgnoreCase(table.getId())) ||
                                 (r.getTableNumber() != null && r.getTableNumber().equalsIgnoreCase(table.getTableNumber())))
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

    private List<Reservation> getActiveReservationsByDate(String date) {
        try {
            List<Reservation> list = reservationRepository.findByDate(date);
            if (!list.isEmpty()) {
                return list.stream()
                    .filter(r -> !"CANCELLED".equalsIgnoreCase(r.getStatus()))
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Reading reservations by date from fallback cache: {}", e.getMessage());
        }
        return fallbackMap.values().stream()
            .filter(r -> r.getDate() != null && r.getDate().equals(date))
            .filter(r -> !"CANCELLED".equalsIgnoreCase(r.getStatus()))
            .distinct()
            .collect(Collectors.toList());
    }

    public Reservation createReservation(Reservation request) {
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

        TableEntity table = null;
        if (request.getTableId() != null) {
            table = tableService.getTableById(request.getTableId()).orElse(null);
        }
        if (table == null && request.getTableNumber() != null) {
            table = tableService.getTableByNumber(request.getTableNumber()).orElse(null);
        }
        if (table == null) {
            List<TableEntity> all = tableService.getAllTables();
            table = !all.isEmpty() ? all.get(0) : TableEntity.builder().id("tbl-01").tableNumber("01").type("Standard Table").location("Main Lounge").build();
        }

        Reservation res = Reservation.builder()
            .reservationId(formattedCode)
            .userId(request.getUserId() != null ? request.getUserId() : "guest-user")
            .customerName(request.getCustomerName() != null ? request.getCustomerName() : "KOPA Guest")
            .customerEmail(request.getCustomerEmail() != null ? request.getCustomerEmail() : "guest@kopa.coffee")
            .customerPhone(request.getCustomerPhone() != null ? request.getCustomerPhone() : "+94 77 000 0000")
            .tableId(table.getId())
            .tableNumber(table.getTableNumber())
            .tableType(table.getType())
            .tableLocation(table.getLocation())
            .date(request.getDate() != null ? request.getDate() : LocalDate.now().toString())
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

        try {
            res = reservationRepository.save(res);
        } catch (Exception e) {
            log.warn("Could not save reservation to MongoDB Atlas: {}", e.getMessage());
            if (res.getId() == null) {
                res.setId(UUID.randomUUID().toString());
            }
        }

        fallbackMap.put(res.getId(), res);
        fallbackMap.put(res.getReservationId(), res);

        return res;
    }

    public List<Reservation> getAllReservations() {
        try {
            List<Reservation> list = reservationRepository.findAll();
            if (!list.isEmpty()) {
                return list.stream()
                    .sorted(Comparator.comparing(Reservation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Reading all reservations from fallback cache: {}", e.getMessage());
        }
        return fallbackMap.values().stream()
            .filter(r -> r.getId() != null)
            .distinct()
            .sorted(Comparator.comparing(Reservation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
    }

    public Optional<Reservation> getReservationById(String idOrCode) {
        if (idOrCode == null) return Optional.empty();
        try {
            Optional<Reservation> res = reservationRepository.findById(idOrCode);
            if (res.isPresent()) return res;
            res = reservationRepository.findByReservationId(idOrCode);
            if (res.isPresent()) return res;
        } catch (Exception e) {
            log.warn("Reading reservation by ID from fallback cache: {}", e.getMessage());
        }
        return Optional.ofNullable(fallbackMap.get(idOrCode));
    }

    public List<Reservation> getReservationsByEmail(String email) {
        if (email == null || email.isBlank()) return Collections.emptyList();
        try {
            List<Reservation> list = reservationRepository.findByCustomerEmailIgnoreCase(email.trim());
            if (!list.isEmpty()) {
                return list.stream()
                    .sorted(Comparator.comparing(Reservation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Reading reservations by email from fallback cache: {}", e.getMessage());
        }
        return fallbackMap.values().stream()
            .filter(r -> email.equalsIgnoreCase(r.getCustomerEmail()))
            .distinct()
            .sorted(Comparator.comparing(Reservation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
    }

    public Optional<Reservation> cancelReservation(String idOrCode) {
        return updateStatus(idOrCode, "CANCELLED");
    }

    public Optional<Reservation> updateStatus(String idOrCode, String newStatus) {
        Optional<Reservation> opt = getReservationById(idOrCode);
        opt.ifPresent(r -> {
            r.setStatus(newStatus.toUpperCase());
            try {
                reservationRepository.save(r);
            } catch (Exception e) {
                log.warn("Could not update reservation status in MongoDB: {}", e.getMessage());
            }
            if (r.getId() != null) fallbackMap.put(r.getId(), r);
            if (r.getReservationId() != null) fallbackMap.put(r.getReservationId(), r);
        });
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

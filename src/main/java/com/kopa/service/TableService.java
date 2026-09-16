package com.kopa.service;

import com.kopa.model.TableEntity;
import com.kopa.repository.TableRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TableService {

    private static final Logger log = LoggerFactory.getLogger(TableService.class);
    private final TableRepository tableRepository;
    private final Map<String, TableEntity> fallbackMap = new ConcurrentHashMap<>();

    public TableService(TableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    @PostConstruct
    public void init() {
        seedTables();
    }

    private void seedTables() {
        List<TableEntity> initialTables = List.of(
            // Window Tables
            TableEntity.builder()
                .id("tbl-01")
                .tableNumber("01")
                .capacity(2)
                .type("Window Table")
                .location("Window Bay")
                .status("AVAILABLE")
                .description("Panoramic street-view window table with warm morning sunlight.")
                .positionX(10)
                .positionY(15)
                .shape("square")
                .build(),

            TableEntity.builder()
                .id("tbl-02")
                .tableNumber("02")
                .capacity(4)
                .type("Window Table")
                .location("Window Bay")
                .status("AVAILABLE")
                .description("Spacious corner window table overlooking the quiet tree-lined boulevard.")
                .positionX(32)
                .positionY(15)
                .shape("rect")
                .build(),

            TableEntity.builder()
                .id("tbl-03")
                .tableNumber("03")
                .capacity(2)
                .type("Window Table")
                .location("Window Bay")
                .status("AVAILABLE")
                .description("Cozy window alcove with plush cushions and soft reading lamp.")
                .positionX(65)
                .positionY(15)
                .shape("square")
                .build(),

            // Couple Tables (Intimate Booths)
            TableEntity.builder()
                .id("tbl-04")
                .tableNumber("04")
                .capacity(2)
                .type("Couple Table")
                .location("Cozy Alcove")
                .status("AVAILABLE")
                .description("Intimate velvet leather booth with soft candle amber glow.")
                .positionX(10)
                .positionY(45)
                .shape("square")
                .build(),

            TableEntity.builder()
                .id("tbl-05")
                .tableNumber("05")
                .capacity(2)
                .type("Couple Table")
                .location("Cozy Alcove")
                .status("AVAILABLE")
                .description("Warm timber nook designed for quiet conversations and shared dessert.")
                .positionX(32)
                .positionY(45)
                .shape("square")
                .build(),

            // Standard Tables
            TableEntity.builder()
                .id("tbl-06")
                .tableNumber("06")
                .capacity(4)
                .type("Standard Table")
                .location("Main Lounge")
                .status("AVAILABLE")
                .description("Solid dark oak dining table in the heart of the coffee lounge.")
                .positionX(55)
                .positionY(45)
                .shape("rect")
                .build(),

            TableEntity.builder()
                .id("tbl-07")
                .tableNumber("07")
                .capacity(4)
                .type("Standard Table")
                .location("Main Lounge")
                .status("AVAILABLE")
                .description("Center lounge table directly facing the acoustic live corner.")
                .positionX(75)
                .positionY(45)
                .shape("rect")
                .build(),

            // Group Tables
            TableEntity.builder()
                .id("tbl-08")
                .tableNumber("08")
                .capacity(6)
                .type("Group Table")
                .location("Library Corner")
                .status("AVAILABLE")
                .description("Long communal walnut table surrounded by antique book shelves.")
                .positionX(55)
                .positionY(75)
                .shape("rect")
                .build(),

            TableEntity.builder()
                .id("tbl-09")
                .tableNumber("09")
                .capacity(8)
                .type("Group Table")
                .location("Private Dining Room")
                .status("AVAILABLE")
                .description("Executive conference and dining space with discreet glass partition.")
                .positionX(78)
                .positionY(75)
                .shape("rect")
                .build(),

            // Outdoor Garden Tables
            TableEntity.builder()
                .id("tbl-10")
                .tableNumber("10")
                .capacity(2)
                .type("Outdoor Table")
                .location("Garden Terrace")
                .status("AVAILABLE")
                .description("Breezy terrace bistro set shaded by olive trees and hanging jasmines.")
                .positionX(88)
                .positionY(15)
                .shape("circle")
                .build(),

            TableEntity.builder()
                .id("tbl-11")
                .tableNumber("11")
                .capacity(4)
                .type("Outdoor Table")
                .location("Garden Terrace")
                .status("AVAILABLE")
                .description("Open-air garden patio table under warm fairy light canopies.")
                .positionX(88)
                .positionY(40)
                .shape("circle")
                .build(),

            // Counter Seats (Bar)
            TableEntity.builder()
                .id("tbl-12")
                .tableNumber("12")
                .capacity(1)
                .type("Counter Seat")
                .location("Espresso Bar")
                .status("AVAILABLE")
                .description("Front row bar stool watching baristas calibrate manual espresso shots.")
                .positionX(10)
                .positionY(78)
                .shape("circle")
                .build(),

            TableEntity.builder()
                .id("tbl-13")
                .tableNumber("13")
                .capacity(1)
                .type("Counter Seat")
                .location("Espresso Bar")
                .status("AVAILABLE")
                .description("Barista brew bar seat equipped with high-speed USB-C charging.")
                .positionX(22)
                .positionY(78)
                .shape("circle")
                .build(),

            TableEntity.builder()
                .id("tbl-14")
                .tableNumber("14")
                .capacity(1)
                .type("Counter Seat")
                .location("Espresso Bar")
                .status("MAINTENANCE")
                .description("Espresso machine service station.")
                .positionX(34)
                .positionY(78)
                .shape("circle")
                .build()
        );

        // Store into fallback cache
        initialTables.forEach(t -> fallbackMap.put(t.getId(), t));

        // Save to MongoDB if available and empty
        try {
            if (tableRepository.count() == 0) {
                tableRepository.saveAll(initialTables);
                log.info("Successfully seeded {} tables into MongoDB Atlas", initialTables.size());
            }
        } catch (Exception e) {
            log.warn("Could not seed tables to MongoDB Atlas (check MONGODB_URI in .env): {}", e.getMessage());
        }
    }

    public List<TableEntity> getAllTables() {
        try {
            List<TableEntity> tables = tableRepository.findAll();
            if (!tables.isEmpty()) return tables;
        } catch (Exception e) {
            log.warn("Reading tables from fallback cache (MongoDB unavailable): {}", e.getMessage());
        }
        return new ArrayList<>(fallbackMap.values());
    }

    public Optional<TableEntity> getTableById(String id) {
        if (id == null) return Optional.empty();
        try {
            Optional<TableEntity> table = tableRepository.findById(id);
            if (table.isPresent()) return table;
        } catch (Exception e) {
            log.warn("Reading table by ID from fallback cache: {}", e.getMessage());
        }
        return Optional.ofNullable(fallbackMap.get(id));
    }

    public Optional<TableEntity> getTableByNumber(String tableNumber) {
        if (tableNumber == null) return Optional.empty();
        try {
            Optional<TableEntity> table = tableRepository.findByTableNumber(tableNumber);
            if (table.isPresent()) return table;
        } catch (Exception e) {
            log.warn("Reading table by number from fallback cache: {}", e.getMessage());
        }
        return fallbackMap.values().stream()
            .filter(t -> t.getTableNumber().equalsIgnoreCase(tableNumber))
            .findFirst();
    }

    public TableEntity saveTable(TableEntity table) {
        fallbackMap.put(table.getId(), table);
        try {
            return tableRepository.save(table);
        } catch (Exception e) {
            log.warn("Could not persist table to MongoDB: {}", e.getMessage());
            return table;
        }
    }
}

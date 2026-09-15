package com.kopa.service;

import com.kopa.model.TableEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TableService {

    private final Map<String, TableEntity> tableMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        seedTables();
    }

    private void seedTables() {
        // Window Tables
        addTable(TableEntity.builder()
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
            .build());

        addTable(TableEntity.builder()
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
            .build());

        addTable(TableEntity.builder()
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
            .build());

        // Couple Tables (Intimate Booths)
        addTable(TableEntity.builder()
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
            .build());

        addTable(TableEntity.builder()
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
            .build());

        // Standard Tables
        addTable(TableEntity.builder()
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
            .build());

        addTable(TableEntity.builder()
            .id("tbl-07")
            .tableNumber("07")
            .capacity(4)
            .type("Standard Table")
            .location("Main Lounge")
            .status("AVAILABLE")
            .description("Comfortable mid-century armchairs with laptop power access.")
            .positionX(78)
            .positionY(45)
            .shape("rect")
            .build());

        addTable(TableEntity.builder()
            .id("tbl-08")
            .tableNumber("08")
            .capacity(2)
            .type("Window Table")
            .location("Window Bay")
            .status("AVAILABLE")
            .description("Prime window table with dedicated acoustic sound baffling.")
            .positionX(88)
            .positionY(15)
            .shape("square")
            .build());

        // Group Table
        addTable(TableEntity.builder()
            .id("tbl-09")
            .tableNumber("09")
            .capacity(6)
            .type("Group Table")
            .location("Central Atrium")
            .status("AVAILABLE")
            .description("Expansive communal dark walnut table, ideal for team sessions or brunch.")
            .positionX(55)
            .positionY(75)
            .shape("rect")
            .build());

        // Outdoor Tables
        addTable(TableEntity.builder()
            .id("tbl-10")
            .tableNumber("10")
            .capacity(4)
            .type("Outdoor Table")
            .location("Garden Terrace")
            .status("AVAILABLE")
            .description("Breezy patio table surrounded by lush fiddle-leaf figs and monsteras.")
            .positionX(80)
            .positionY(75)
            .shape("circle")
            .build());

        addTable(TableEntity.builder()
            .id("tbl-11")
            .tableNumber("11")
            .capacity(2)
            .type("Outdoor Table")
            .location("Garden Terrace")
            .status("AVAILABLE")
            .description("Bistro round marble table under the terrace awning.")
            .positionX(92)
            .positionY(75)
            .shape("circle")
            .build());

        // Counter Seats (Bar)
        addTable(TableEntity.builder()
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
            .build());

        addTable(TableEntity.builder()
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
            .build());

        addTable(TableEntity.builder()
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
            .build());
    }

    private void addTable(TableEntity table) {
        tableMap.put(table.getId(), table);
    }

    public List<TableEntity> getAllTables() {
        return new ArrayList<>(tableMap.values());
    }

    public Optional<TableEntity> getTableById(String id) {
        return Optional.ofNullable(tableMap.get(id));
    }

    public Optional<TableEntity> getTableByNumber(String tableNumber) {
        return tableMap.values().stream()
            .filter(t -> t.getTableNumber().equalsIgnoreCase(tableNumber))
            .findFirst();
    }
}

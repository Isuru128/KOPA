package com.kopa.controller;

import com.kopa.model.TableEntity;
import com.kopa.service.ReservationService;
import com.kopa.service.TableService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "*")
public class TableController {

    private final TableService tableService;
    private final ReservationService reservationService;

    public TableController(TableService tableService, ReservationService reservationService) {
        this.tableService = tableService;
        this.reservationService = reservationService;
    }

    @GetMapping
    public ResponseEntity<List<TableEntity>> getAllTables() {
        return ResponseEntity.ok(tableService.getAllTables());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TableEntity> getTableById(@PathVariable String id) {
        return tableService.getTableById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/availability")
    public ResponseEntity<List<ReservationService.TableAvailabilityDTO>> getAvailability(
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "18:00") String time,
            @RequestParam(required = false, defaultValue = "2") int guests) {

        if (date == null || date.isBlank()) {
            date = LocalDate.now().toString();
        }

        List<ReservationService.TableAvailabilityDTO> availability =
            reservationService.getTableAvailability(date, time, guests);

        return ResponseEntity.ok(availability);
    }
}

package com.kopa.repository;

import com.kopa.model.Reservation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends MongoRepository<Reservation, String> {
    Optional<Reservation> findByReservationId(String reservationId);
    List<Reservation> findByUserId(String userId);
    List<Reservation> findByCustomerEmailIgnoreCase(String customerEmail);
    List<Reservation> findByDate(String date);
    List<Reservation> findByDateAndTableId(String date, String tableId);
}

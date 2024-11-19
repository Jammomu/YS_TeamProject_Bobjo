package com.mysite.restaurant.kdh.Controller;

import com.mysite.restaurant.kdh.Entity.ReservationEntity;
import com.mysite.restaurant.kdh.Service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // 예약 생성 API (POST)
    @PostMapping("/reservations")
    public ResponseEntity<?> createReservation(@RequestBody ReservationEntity reservation) {
        // 예약 생성
        ReservationEntity createdReservation = reservationService.createReservation(reservation);

        // 생성된 예약 정보를 JSON 형식으로 반환
        return ResponseEntity.ok().body(createdReservation);
    }
}

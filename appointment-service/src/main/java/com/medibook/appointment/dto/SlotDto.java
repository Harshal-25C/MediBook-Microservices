package com.medibook.appointment.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Lightweight DTO received from slot-service via Feign.
 * Only the fields appointment-service needs — no shared entity.
 */

@Data
public class SlotDto {
    private int slotId;
    private int providerId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private int durationMinutes;
    private boolean isBooked;
    private boolean isBlocked;
}
package com.medibook.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Payload published to RabbitMQ for every appointment lifecycle event.
 * notification-service deserializes this and creates the in-app / email alert.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentEventDto {

    private int    appointmentId;
    private int    patientId;
    private int    providerId;
    private String eventType;      // BOOKED | CANCELLED | COMPLETED
    private String serviceType;
    private String modeOfConsultation;
    private String appointmentDate;
    private String startTime;
    private String endTime;
    private String message;    // human-readable notification text
}
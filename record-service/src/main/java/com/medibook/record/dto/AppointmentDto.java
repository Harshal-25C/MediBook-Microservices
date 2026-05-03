package com.medibook.record.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/*
 * FIX: Added @JsonIgnoreProperties(ignoreUnknown = true) so that any extra
 * fields returned by appointment-service (slotId, patientEmail, serviceType,
 * modeOfConsultation, etc.) are silently ignored instead of causing a
 * deserialization error → 500.
 *
 * Also added all required fields so `status` is never null.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppointmentDto {
    private int appointmentId;
    private int patientId;
    private int providerId;
    private int slotId;
    private String status;          // SCHEDULED / COMPLETED / CANCELLED / NO_SHOW
    private String serviceType;
    private String modeOfConsultation;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String notes;
    private String patientEmail;
}

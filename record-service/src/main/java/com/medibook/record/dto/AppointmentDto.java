package com.medibook.record.dto;

import lombok.Data;

@Data
public class AppointmentDto {
    private int appointmentId;
    private int patientId;
    private int providerId;
    private String status;
}

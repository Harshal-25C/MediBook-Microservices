package com.medibook.payment.dto.request;

import lombok.Data;

@Data
public class AppointmentDto {
    private int appointmentId;
    private int patientId;
    private int providerId;
    private String status;
}

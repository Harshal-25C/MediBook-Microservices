package com.medibook.notification.dto;

import lombok.Data;

@Data
public class AppointmentEventDto {
    private int appointmentId;
    private int patientId;
    private int providerId;
    private String eventType;
    private String serviceType;
    private String modeOfConsultation;
    private String appointmentDate;
    private String startTime;
    private String endTime;
    private String message;
}
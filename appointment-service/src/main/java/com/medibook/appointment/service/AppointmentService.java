package com.medibook.appointment.service;

import com.medibook.appointment.dto.AppointmentRequest;
import com.medibook.appointment.entity.Appointment;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    Appointment bookAppointment(AppointmentRequest request);

    Appointment getById(int appointmentId);

    List<Appointment> getByPatient(int patientId);

    List<Appointment> getByProvider(int providerId);

    List<Appointment> getByProviderAndDate(int providerId, LocalDate date);

    List<Appointment> getUpcomingByPatient(int patientId);

    void cancelAppointment(int appointmentId);

    Appointment rescheduleAppointment(
            int appointmentId,
            int newSlotId,
            LocalDate newDate,
            String newStartTime,
            String newEndTime
    );

    /**
     * FIX: Provider marks appointment COMPLETED.
     * requestingProviderId is checked against appointment.providerId
     * to ensure only the assigned provider can complete it.
     */
    void completeAppointment(int appointmentId, int requestingProviderId);

    /**
     * FIX: Provider marks appointment NO_SHOW.
     * requestingProviderId is checked against appointment.providerId
     * to ensure only the assigned provider can mark NO_SHOW.
     */
    void markNoShow(int appointmentId, int requestingProviderId);

    /**
     * Update appointment status.
     * Used by admin and the NoShowDetectionScheduler.
     * When status is CONFIRMED (from payment), stored as SCHEDULED.
     */
    void updateStatus(int appointmentId, String status);

    int getAppointmentCount(int providerId);
}

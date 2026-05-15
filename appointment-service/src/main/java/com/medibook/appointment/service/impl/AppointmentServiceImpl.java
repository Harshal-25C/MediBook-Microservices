package com.medibook.appointment.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medibook.appointment.client.SlotClient;
import com.medibook.appointment.dto.AppointmentRequest;
import com.medibook.appointment.dto.SlotDto;
import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.exception.BadRequestException;
import com.medibook.appointment.exception.ForbiddenException;
import com.medibook.appointment.exception.ResourceNotFoundException;
import com.medibook.appointment.messaging.AppointmentEventPublisher;
import com.medibook.appointment.repository.AppointmentRepository;
import com.medibook.appointment.service.AppointmentService;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private SlotClient slotClient;

    @Autowired
    private AppointmentEventPublisher eventPublisher;

    @Override
    @Transactional
    public Appointment bookAppointment(AppointmentRequest request) {

        SlotDto slot = slotClient.getSlotById(request.getSlotId());

        if (slot.isBooked())
            throw new BadRequestException("This slot is already booked. Please choose another slot.");
        if (slot.isBlocked())
            throw new BadRequestException("This slot is blocked by the doctor.");
        if (slot.getProviderId() != request.getProviderId())
            throw new BadRequestException("Slot does not belong to the selected provider.");

        Appointment appointment = Appointment.builder()
                .patientId(request.getPatientId())
                .providerId(request.getProviderId())
                .patientEmail(request.getPatientEmail())
                .slotId(request.getSlotId())
                .serviceType(request.getServiceType())
                .appointmentDate(slot.getDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .modeOfConsultation(request.getModeOfConsultation())
                .notes(request.getNotes())
                .status("SCHEDULED")
                .build();

        Appointment saved = appointmentRepository.save(appointment);

        // Book the slot immediately — appointment is SCHEDULED from creation
        slotClient.bookSlot(request.getSlotId());
        try { eventPublisher.publishBooked(saved); } catch (Exception e) {
            System.err.println("[RabbitMQ] publishBooked failed (non-fatal): " + e.getMessage());
        }

        return saved;
    }

    @Override
    public Appointment getById(int appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));
    }

    @Override
    public List<Appointment> getByPatient(int patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    @Override
    public List<Appointment> getUpcomingByPatient(int patientId) {
        return appointmentRepository.findUpcomingByPatientId(patientId, LocalDate.now());
    }

    @Override
    public List<Appointment> getByProvider(int providerId) {
        return appointmentRepository.findByProviderId(providerId);
    }

    @Override
    public List<Appointment> getByProviderAndDate(int providerId, LocalDate date) {
        return appointmentRepository.findByProviderIdAndAppointmentDate(providerId, date);
    }

    @Override
    @Transactional
    public void cancelAppointment(int appointmentId) {
        Appointment appointment = getById(appointmentId);
        if (appointment.getStatus().equals("COMPLETED"))
            throw new BadRequestException("Cannot cancel a completed appointment.");
        if (appointment.getStatus().equals("CANCELLED"))
            throw new BadRequestException("Appointment is already cancelled.");

        appointment.setStatus("CANCELLED");
        Appointment saved = appointmentRepository.save(appointment);
        slotClient.releaseSlot(appointment.getSlotId());
        // Wrapped in try-catch: RabbitMQ being down must never block cancellation
        try { eventPublisher.publishCancelled(saved); } catch (Exception e) {
            System.err.println("[RabbitMQ] publishCancelled failed (non-fatal): " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Appointment rescheduleAppointment(int appointmentId, int newSlotId,
            LocalDate newDate, String newStartTime, String newEndTime) {
        Appointment appointment = getById(appointmentId);

        if (!appointment.getStatus().equals("SCHEDULED") && !appointment.getStatus().equals("CONFIRMED"))
            throw new BadRequestException("Only SCHEDULED appointments can be rescheduled.");

        slotClient.releaseSlot(appointment.getSlotId());

        SlotDto newSlot = slotClient.getSlotById(newSlotId);
        if (newSlot.isBooked()) throw new BadRequestException("New slot is already booked.");

        appointment.setSlotId(newSlotId);
        appointment.setAppointmentDate(newSlot.getDate());
        appointment.setStartTime(newSlot.getStartTime());
        appointment.setEndTime(newSlot.getEndTime());
        // Keep status as-is (SCHEDULED stays SCHEDULED)

        Appointment saved = appointmentRepository.save(appointment);
        slotClient.bookSlot(newSlotId);
        return saved;
    }

    /**
     * FIX: Provider marks appointment as COMPLETED.
     * Authorization check: only the assigned provider can complete it.
     */
    @Override
    @Transactional
    public void completeAppointment(int appointmentId, int requestingProviderId) {
        Appointment appointment = getById(appointmentId);
        
        // Authorization check: only the assigned provider may complete
        if (appointment.getProviderId() != requestingProviderId) {
            throw new ForbiddenException(
                "You are not authorized to complete this appointment. " +
                "Only the assigned provider can mark it as completed."
            );
        }

        if (!appointment.getStatus().equals("SCHEDULED"))
            throw new BadRequestException("Only SCHEDULED appointments can be marked complete.");

        if (appointment.getStatus().equals("COMPLETED"))
            throw new BadRequestException("Appointment already completed.");
        if (appointment.getStatus().equals("CANCELLED"))
            throw new BadRequestException("Cannot complete a cancelled appointment.");

        appointment.setStatus("COMPLETED");
        Appointment saved = appointmentRepository.save(appointment);

        try { eventPublisher.publishCompleted(saved); } catch (Exception e) {
            System.err.println("[RabbitMQ] publishCompleted failed (non-fatal): " + e.getMessage());
        }
    }

    /**
     * FIX: Provider marks appointment as NO_SHOW.
     * Authorization check: only the assigned provider can mark NO_SHOW.
     */
    @Override
    @Transactional
    public void markNoShow(int appointmentId, int requestingProviderId) {
        Appointment appointment = getById(appointmentId);

        // Authorization check: only the assigned provider may mark NO_SHOW
        if (appointment.getProviderId() != requestingProviderId) {
            throw new ForbiddenException(
                "You are not authorized to mark this appointment as NO_SHOW. " +
                "Only the assigned provider can do this."
            );
        }

        if (appointment.getStatus().equals("COMPLETED"))
            throw new BadRequestException("Cannot mark a completed appointment as NO_SHOW.");
        if (appointment.getStatus().equals("CANCELLED"))
            throw new BadRequestException("Cannot mark a cancelled appointment as NO_SHOW.");
        if (appointment.getStatus().equals("NO_SHOW"))
            throw new BadRequestException("Appointment is already marked as NO_SHOW.");

        appointment.setStatus("NO_SHOW");
        Appointment saved = appointmentRepository.save(appointment);
        try { eventPublisher.publishCompleted(saved); } catch (Exception e) {
            System.err.println("[RabbitMQ] publishCompleted(NO_SHOW) failed (non-fatal): " + e.getMessage());
        }
    }

    /**
     * FIX: updateStatus
     * Frontend now sends "SCHEDULED" directly after payment success.
     * "CONFIRMED" is kept as fallback (legacy / COD old flow) → also stored as SCHEDULED.
     * Both book the slot and fire the booked event.
     */
    @Override
    @Transactional
    public void updateStatus(int appointmentId, String status) {
        Appointment appointment = getById(appointmentId);

        if ("SCHEDULED".equals(status) || "CONFIRMED".equals(status)) {
            // Payment verified: ensure status is SCHEDULED.
            // Slot is already booked at appointment creation — do NOT call bookSlot again.
            appointment.setStatus("SCHEDULED");
            eventPublisher.publishBooked(appointment);
        } else if ("CANCELLED".equals(status)) {
            appointment.setStatus("CANCELLED");
            slotClient.releaseSlot(appointment.getSlotId());
            eventPublisher.publishCancelled(appointment);
        } else {
            appointment.setStatus(status);
        }

        appointmentRepository.save(appointment);
    }

    @Override
    public int getAppointmentCount(int providerId) {
        return (int) appointmentRepository.countByProviderId(providerId);
    }
}

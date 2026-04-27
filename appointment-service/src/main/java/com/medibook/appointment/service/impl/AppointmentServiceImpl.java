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

    /** RabbitMQ publisher — injected to fire events after each state change */
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
                .status("PENDING_PAYMENT")
                .build();

        Appointment saved = appointmentRepository.save(appointment);
     

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

        // ── RabbitMQ: publish CANCELLED event → notification-service ──
        eventPublisher.publishCancelled(saved);
    }

    @Override
    @Transactional
    public Appointment rescheduleAppointment(int appointmentId, int newSlotId,
            LocalDate newDate, String newStartTime, String newEndTime) {
        Appointment appointment = getById(appointmentId);
        if (!appointment.getStatus().equals("SCHEDULED"))
            throw new BadRequestException("Only SCHEDULED appointments can be rescheduled.");

        slotClient.releaseSlot(appointment.getSlotId());

        SlotDto newSlot = slotClient.getSlotById(newSlotId);
        if (newSlot.isBooked()) throw new BadRequestException("New slot is already booked.");

        appointment.setSlotId(newSlotId);
        appointment.setAppointmentDate(newSlot.getDate());
        appointment.setStartTime(newSlot.getStartTime());
        appointment.setEndTime(newSlot.getEndTime());

        Appointment saved = appointmentRepository.save(appointment);
        slotClient.bookSlot(newSlotId);
        return saved;
    }

    @Override
    @Transactional
    public void completeAppointment(int appointmentId) {
        Appointment appointment = getById(appointmentId);

        if (appointment.getStatus().equals("COMPLETED")) {
            throw new BadRequestException("Appointment already completed.");
        }

        appointment.setStatus("COMPLETED");
        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public void updateStatus(int appointmentId, String status) {
        Appointment appointment = getById(appointmentId);
        appointment.setStatus(status);
        if (status.equals("CONFIRMED")) {
            slotClient.bookSlot(appointment.getSlotId());
            eventPublisher.publishBooked(appointment);
        }
        if (status.equals("CANCELLED")) {
            slotClient.releaseSlot(appointment.getSlotId());
            eventPublisher.publishCancelled(appointment);
        }
        appointmentRepository.save(appointment);
    }

    @Override
    public int getAppointmentCount(int providerId) {
        return (int) appointmentRepository.countByProviderId(providerId);
    }
}
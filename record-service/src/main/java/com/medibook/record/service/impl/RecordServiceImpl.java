package com.medibook.record.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medibook.record.client.AppointmentClient;
import com.medibook.record.dto.AppointmentDto;
import com.medibook.record.dto.RecordRequest;
import com.medibook.record.entity.MedicalRecord;
import com.medibook.record.exception.BadRequestException;
import com.medibook.record.exception.DuplicateResourceException;
import com.medibook.record.exception.ResourceNotFoundException;
import com.medibook.record.repository.RecordRepository;
import com.medibook.record.service.RecordService;

@Service
public class RecordServiceImpl implements RecordService {

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private AppointmentClient appointmentClient;

    @Override
    public MedicalRecord createRecord(RecordRequest request) {

        // FIX: Wrap Feign call in try-catch — if appointment-service is down
        // or returns an unexpected response, give a clear 400 instead of 500.
        AppointmentDto appointment;
        try {
            appointment = appointmentClient.getById(request.getAppointmentId());
        } catch (Exception e) {
            throw new BadRequestException(
                "Could not fetch appointment #" + request.getAppointmentId() +
                ". Make sure appointment-service is running. Error: " + e.getMessage()
            );
        }

        // FIX: Null-safe status check — Feign may deserialize status as null
        // if the field name doesn't match exactly.
        String status = appointment.getStatus();
        if (status == null) {
            throw new BadRequestException(
                "Appointment #" + request.getAppointmentId() +
                " returned a null status. Please check appointment-service."
            );
        }

        if (!status.equalsIgnoreCase("COMPLETED")) {
            throw new BadRequestException(
                "Medical record can only be created for COMPLETED appointments. " +
                "Current status: " + status
            );
        }

        if (recordRepository.existsByAppointmentId(request.getAppointmentId())) {
            throw new DuplicateResourceException(
                "Medical record already exists for appointment: " + request.getAppointmentId()
            );
        }

        if (request.getDiagnosis() == null || request.getDiagnosis().trim().isEmpty()) {
            throw new BadRequestException("Diagnosis is required for medical record.");
        }

        if (request.getFollowUpDate() != null && request.getFollowUpDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Follow up date cannot be in the past.");
        }

        MedicalRecord record = MedicalRecord.builder()
                .appointmentId(request.getAppointmentId())
                .patientId(request.getPatientId())
                .providerId(request.getProviderId())
                .diagnosis(request.getDiagnosis())
                .prescription(request.getPrescription())
                .notes(request.getNotes())
                .attachmentUrl(request.getAttachmentUrl())
                .followUpDate(request.getFollowUpDate())
                .build();

        return recordRepository.save(record);
    }

    @Override
    public MedicalRecord getRecordByAppointment(int appointmentId) {
        return recordRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", "appointmentId", appointmentId));
    }

    @Override
    public List<MedicalRecord> getRecordsByPatient(int patientId) {
        return recordRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    @Override
    public List<MedicalRecord> getRecordsByProvider(int providerId) {
        return recordRepository.findByProviderId(providerId);
    }

    @Override
    public MedicalRecord getRecordById(int recordId) {
        return recordRepository.findByRecordId(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", "id", recordId));
    }

    @Override
    public MedicalRecord updateRecord(int recordId, RecordRequest request) {
        MedicalRecord existing = getRecordById(recordId);

        if (request.getDiagnosis() == null || request.getDiagnosis().trim().isEmpty()) {
            throw new BadRequestException("Diagnosis cannot be empty.");
        }
        if (request.getFollowUpDate() != null && request.getFollowUpDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Follow up date cannot be in the past.");
        }

        existing.setDiagnosis(request.getDiagnosis());
        existing.setPrescription(request.getPrescription());
        existing.setNotes(request.getNotes());
        existing.setAttachmentUrl(request.getAttachmentUrl());
        existing.setFollowUpDate(request.getFollowUpDate());

        return recordRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteRecord(int recordId) {
        getRecordById(recordId);
        recordRepository.deleteByRecordId(recordId);
    }

    @Override
    public void attachDocument(int recordId, String attachmentUrl) {
        if (attachmentUrl == null || attachmentUrl.trim().isEmpty()) {
            throw new BadRequestException("Attachment URL cannot be empty.");
        }
        MedicalRecord record = getRecordById(recordId);
        record.setAttachmentUrl(attachmentUrl);
        recordRepository.save(record);
    }

    @Override
    public List<MedicalRecord> getFollowUpRecords(LocalDate date) {
        if (date == null) throw new BadRequestException("Date cannot be null.");
        return recordRepository.findByFollowUpDate(date);
    }

    @Override
    public List<MedicalRecord> getUpcomingFollowUps(int patientId) {
        return recordRepository.findUpcomingFollowUps(patientId, LocalDate.now());
    }

    @Override
    public int getRecordCount(int patientId) {
        return (int) recordRepository.countByPatientId(patientId);
    }
}

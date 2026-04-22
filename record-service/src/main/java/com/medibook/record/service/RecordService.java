package com.medibook.record.service;

import com.medibook.record.dto.RecordRequest;
import com.medibook.record.entity.MedicalRecord;

import java.time.LocalDate;
import java.util.List;

public interface RecordService {

    MedicalRecord createRecord(RecordRequest request);

    MedicalRecord getRecordByAppointment(int appointmentId);

    List<MedicalRecord> getRecordsByPatient(int patientId);

    List<MedicalRecord> getRecordsByProvider(int providerId);

    MedicalRecord getRecordById(int recordId);

    MedicalRecord updateRecord(int recordId, RecordRequest request);

    void deleteRecord(int recordId);

    void attachDocument(int recordId, String attachmentUrl);

    List<MedicalRecord> getFollowUpRecords(LocalDate date);

    List<MedicalRecord> getUpcomingFollowUps(int patientId);

    int getRecordCount(int patientId);
}
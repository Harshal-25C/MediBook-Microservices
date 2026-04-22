package com.medibook.record.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.medibook.record.entity.MedicalRecord;


@Repository
public interface RecordRepository
        extends JpaRepository<MedicalRecord, Integer> {

    Optional<MedicalRecord> findByAppointmentId(int appointmentId);

    List<MedicalRecord> findByPatientId(int patientId);

    List<MedicalRecord> findByProviderId(int providerId);

    List<MedicalRecord> findByPatientIdOrderByCreatedAtDesc(
            int patientId
    );

    List<MedicalRecord> findByFollowUpDate(LocalDate followUpDate);

    long countByPatientId(int patientId);

    @Transactional
    @Modifying
    @Query("DELETE FROM MedicalRecord r WHERE r.recordId = :recordId")
    void deleteByRecordId(int recordId);

    Optional<MedicalRecord> findByRecordId(int recordId);

    @Query("SELECT r FROM MedicalRecord r WHERE " +
           "r.patientId = :patientId " +
           "AND r.followUpDate IS NOT NULL " +
           "AND r.followUpDate >= :today")
    List<MedicalRecord> findUpcomingFollowUps(
            @Param("patientId") int patientId,
            @Param("today") LocalDate today
    );

    boolean existsByAppointmentId(int appointmentId);
}
package com.medibook.appointment.repository;

import com.medibook.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository
        extends JpaRepository<Appointment, Integer> {

    List<Appointment> findByPatientId(int patientId);

    List<Appointment> findByProviderId(int providerId);

    Optional<Appointment> findBySlotId(int slotId);

    List<Appointment> findByStatus(String status);

    List<Appointment> findByProviderIdAndAppointmentDate(
            int providerId,
            LocalDate appointmentDate
    );
    
    @Query("SELECT a FROM Appointment a WHERE " +
           "a.patientId = :patientId " +
           "AND a.status = 'SCHEDULED' " +
           "AND a.appointmentDate >= :today")
    List<Appointment> findUpcomingByPatientId(
            @Param("patientId") int patientId,
            @Param("today") LocalDate today
    );

    long countByProviderId(int providerId);

    Optional<Appointment> findByAppointmentId(int appointmentId);

    List<Appointment> findByProviderIdAndStatus(
            int providerId,
            String status
    );

    @Query("SELECT a FROM Appointment a WHERE " +
           "a.status = 'SCHEDULED' " +
           "AND a.appointmentDate < :today")
    List<Appointment> findNoShowAppointments(
            @Param("today") LocalDate today
    );
}
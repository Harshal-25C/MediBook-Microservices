package com.medibook.appointment.resource;

import com.medibook.appointment.dto.AppointmentRequest;
import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/appointments")
public class AppointmentResource {

    @Autowired
    private AppointmentService appointmentService;

    @PostMapping("/book")
    public ResponseEntity<?> bookAppointment(
            @Valid @RequestBody AppointmentRequest request) {

        Appointment appointment = appointmentService.bookAppointment(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Appointment booked successfully.",
                        "appointmentId", appointment.getAppointmentId(),
                        "status", appointment.getStatus(),
                        "appointmentDate", appointment.getAppointmentDate(),
                        "startTime", appointment.getStartTime(),
                        "modeOfConsultation", appointment.getModeOfConsultation()
                ));
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<Appointment> getById(
            @PathVariable int appointmentId) {
        return ResponseEntity.ok(appointmentService.getById(appointmentId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Appointment>> getByPatient(
            @PathVariable int patientId) {
        return ResponseEntity.ok(appointmentService.getByPatient(patientId));
    }

    @GetMapping("/patient/{patientId}/upcoming")
    public ResponseEntity<List<Appointment>> getUpcoming(
            @PathVariable int patientId) {
        return ResponseEntity.ok(appointmentService.getUpcomingByPatient(patientId));
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<Appointment>> getByProvider(
            @PathVariable int providerId) {
        return ResponseEntity.ok(appointmentService.getByProvider(providerId));
    }

    @GetMapping("/provider/{providerId}/date")
    public ResponseEntity<List<Appointment>> getByProviderAndDate(
            @PathVariable int providerId,
            @RequestParam String date) {
        LocalDate appointmentDate = LocalDate.parse(date);
        return ResponseEntity.ok(
                appointmentService.getByProviderAndDate(providerId, appointmentDate));
    }

    @PutMapping("/{appointmentId}/cancel")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable int appointmentId) {
        appointmentService.cancelAppointment(appointmentId);
        return ResponseEntity.ok(Map.of(
                "message", "Appointment cancelled successfully. Slot has been released."
        ));
    }

    @PutMapping("/{appointmentId}/reschedule")
    public ResponseEntity<Appointment> rescheduleAppointment(
            @PathVariable int appointmentId,
            @RequestBody Map<String, String> body) {

        int newSlotId = Integer.parseInt(body.get("newSlotId"));
        LocalDate newDate = LocalDate.parse(body.get("newDate"));
        String newStartTime = body.get("newStartTime");
        String newEndTime = body.get("newEndTime");

        Appointment updated = appointmentService.rescheduleAppointment(
                appointmentId, newSlotId, newDate, newStartTime, newEndTime);

        return ResponseEntity.ok(updated);
    }

    /**
     * FIX: Provider marks appointment COMPLETED.
     * Requires providerId in request body to verify authorization.
     * Only the provider who the patient selected during booking can complete the appointment.
     *
     * URL: PUT /appointments/{appointmentId}/complete
     * Body: { "providerId": 5 }
     */
    @PutMapping("/{appointmentId}/complete")
    public ResponseEntity<?> completeAppointment(
            @PathVariable int appointmentId,
            @RequestBody Map<String, Integer> body) {

        Integer providerId = body.get("providerId");
        if (providerId == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "providerId is required in request body."
            ));
        }

        appointmentService.completeAppointment(appointmentId, providerId);

        return ResponseEntity.ok(Map.of(
                "message", "Appointment marked as completed. Patient can now submit a review."
        ));
    }

    /**
     * FIX: Provider marks appointment NO_SHOW.
     * Requires providerId in request body to verify authorization.
     * Only the assigned provider can mark NO_SHOW.
     *
     * URL: PUT /appointments/{appointmentId}/no-show
     * Body: { "providerId": 5 }
     */
    @PutMapping("/{appointmentId}/no-show")
    public ResponseEntity<?> markNoShow(
            @PathVariable int appointmentId,
            @RequestBody Map<String, Integer> body) {

        Integer providerId = body.get("providerId");
        if (providerId == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "providerId is required in request body."
            ));
        }

        appointmentService.markNoShow(appointmentId, providerId);

        return ResponseEntity.ok(Map.of(
                "message", "Appointment marked as NO_SHOW. Patient did not attend."
        ));
    }

    /**
     * Update appointment status manually.
     * Used by admin or the NoShowDetectionScheduler.
     * Note: Sending status=CONFIRMED will store it as SCHEDULED (payment confirmation flow).
     *
     * URL: PUT /appointments/{appointmentId}/status?status=NO_SHOW
     */
    @PutMapping("/{appointmentId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable int appointmentId,
            @RequestParam String status) {

        appointmentService.updateStatus(appointmentId, status);

        return ResponseEntity.ok(Map.of(
                "message", "Appointment status updated to: " + status
        ));
    }

    @GetMapping("/provider/{providerId}/count")
    public ResponseEntity<?> getCount(
            @PathVariable int providerId) {

        int count = appointmentService.getAppointmentCount(providerId);

        return ResponseEntity.ok(Map.of(
                "providerId", providerId,
                "totalAppointments", count
        ));
    }
}


package com.medibook.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import com.medibook.appointment.dto.AppointmentRequest;
import com.medibook.appointment.dto.SlotDto;
import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.repository.AppointmentRepository;
import com.medibook.appointment.resource.AppointmentResource;
import com.medibook.appointment.scheduler.NoShowDetectionScheduler;
import com.medibook.appointment.service.AppointmentService;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class GeneratedAppointmentTargetedCoverageTest {
    @Test
    void resourceBadRequestBranchesAreCovered() throws Exception {
        AppointmentService service = mock(AppointmentService.class);
        AppointmentResource resource = new AppointmentResource();
        set(resource, "appointmentService", service);

        ResponseEntity<?> complete = resource.completeAppointment(1, Map.of());
        ResponseEntity<?> noShow = resource.markNoShow(1, Map.of());

        assertThat(complete.getStatusCode().value()).isEqualTo(400);
        assertThat(noShow.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void schedulerMarksPastAndElapsedAppointmentsAndCatchesFailure() throws Exception {
        AppointmentRepository repo = mock(AppointmentRepository.class);
        AppointmentService service = mock(AppointmentService.class);
        NoShowDetectionScheduler scheduler = new NoShowDetectionScheduler();
        set(scheduler, "appointmentRepository", repo);
        set(scheduler, "appointmentService", service);
        Appointment past = Appointment.builder().appointmentId(1).appointmentDate(LocalDate.now().minusDays(1)).endTime(LocalTime.NOON).build();
        Appointment today = Appointment.builder().appointmentId(2).appointmentDate(LocalDate.now()).endTime(LocalTime.now().minusMinutes(1)).build();
        Appointment future = Appointment.builder().appointmentId(3).appointmentDate(LocalDate.now().plusDays(1)).endTime(LocalTime.NOON).build();
        when(repo.findByStatus("SCHEDULED")).thenReturn(List.of(past, today, future));

        scheduler.detectNoShows();

        verify(service).updateStatus(1, "NO_SHOW");
        verify(service).updateStatus(2, "NO_SHOW");
        verify(service, never()).updateStatus(3, "NO_SHOW");

        when(repo.findByStatus("SCHEDULED")).thenThrow(new RuntimeException("db"));
        scheduler.detectNoShows();
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}

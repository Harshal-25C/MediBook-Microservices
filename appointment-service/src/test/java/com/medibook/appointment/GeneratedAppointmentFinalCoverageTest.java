
package com.medibook.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.medibook.appointment.dto.AppointmentRequest;
import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.exception.BadRequestException;
import com.medibook.appointment.exception.ForbiddenException;
import com.medibook.appointment.messaging.AppointmentEventPublisher;
import com.medibook.appointment.repository.AppointmentRepository;
import com.medibook.appointment.resource.AppointmentResource;
import com.medibook.appointment.service.AppointmentService;
import com.medibook.appointment.service.impl.AppointmentServiceImpl;
import com.medibook.appointment.client.SlotClient;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class GeneratedAppointmentFinalCoverageTest {
    @Test
    void resourceHappyPathsCoverResponseBodies() throws Exception {
        AppointmentService service = mock(AppointmentService.class);
        AppointmentResource resource = new AppointmentResource();
        set(resource, "appointmentService", service);
        Appointment appt = appointment("SCHEDULED");
        when(service.bookAppointment(any(AppointmentRequest.class))).thenReturn(appt);
        when(service.getById(1)).thenReturn(appt);
        when(service.getByPatient(2)).thenReturn(List.of(appt));
        when(service.getUpcomingByPatient(2)).thenReturn(List.of(appt));
        when(service.getByProvider(3)).thenReturn(List.of(appt));
        when(service.getByProviderAndDate(3, appt.getAppointmentDate())).thenReturn(List.of(appt));
        when(service.rescheduleAppointment(1, 8, appt.getAppointmentDate(), "10:00", "10:30")).thenReturn(appt);
        when(service.getAppointmentCount(3)).thenReturn(4);

        assertThat(resource.bookAppointment(new AppointmentRequest()).getStatusCode().value()).isEqualTo(201);
        assertThat(resource.getById(1).getBody()).isSameAs(appt);
        assertThat(resource.getByPatient(2).getBody()).containsExactly(appt);
        assertThat(resource.getUpcoming(2).getBody()).containsExactly(appt);
        assertThat(resource.getByProvider(3).getBody()).containsExactly(appt);
        assertThat(resource.getByProviderAndDate(3, appt.getAppointmentDate().toString()).getBody()).containsExactly(appt);
        assertThat(resource.cancelAppointment(1).getStatusCode().value()).isEqualTo(200);
        ResponseEntity<Appointment> rescheduled = resource.rescheduleAppointment(1, Map.of(
                "newSlotId", "8", "newDate", appt.getAppointmentDate().toString(),
                "newStartTime", "10:00", "newEndTime", "10:30"));
        assertThat(rescheduled.getBody()).isSameAs(appt);
        assertThat(resource.completeAppointment(1, Map.of("providerId", 3)).getStatusCode().value()).isEqualTo(200);
        assertThat(resource.markNoShow(1, Map.of("providerId", 3)).getStatusCode().value()).isEqualTo(200);
        assertThat(resource.updateStatus(1, "CONFIRMED").getStatusCode().value()).isEqualTo(200);
        assertThat(resource.getCount(3).getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void serviceCoversPublisherFailuresAndValidationBranches() throws Exception {
        AppointmentRepository repo = mock(AppointmentRepository.class);
        SlotClient slotClient = mock(SlotClient.class);
        AppointmentEventPublisher publisher = mock(AppointmentEventPublisher.class);
        AppointmentServiceImpl service = new AppointmentServiceImpl();
        set(service, "appointmentRepository", repo);
        set(service, "slotClient", slotClient);
        set(service, "eventPublisher", publisher);
        Appointment appt = appointment("SCHEDULED");
        when(repo.findById(1)).thenReturn(Optional.of(appt));
        when(repo.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        doThrow(new RuntimeException("down")).when(publisher).publishCancelled(any(Appointment.class));
        assertThatCode(() -> service.cancelAppointment(1)).doesNotThrowAnyException();

        appt.setStatus("SCHEDULED");
        doThrow(new RuntimeException("down")).when(publisher).publishCompleted(any(Appointment.class));
        assertThatCode(() -> service.completeAppointment(1, 3)).doesNotThrowAnyException();

        appt.setStatus("SCHEDULED");
        assertThatCode(() -> service.markNoShow(1, 3)).doesNotThrowAnyException();

        appt.setStatus("SCHEDULED");
        service.updateStatus(1, "SCHEDULED");
        verify(publisher).publishBooked(appt);
        service.updateStatus(1, "OTHER");
        assertThat(appt.getStatus()).isEqualTo("OTHER");

        appt.setStatus("COMPLETED");
        assertThatThrownBy(() -> service.completeAppointment(1, 3)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.markNoShow(1, 99)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.rescheduleAppointment(1, 9, null, null, null)).isInstanceOf(BadRequestException.class);
    }

    private static Appointment appointment(String status) {
        return Appointment.builder()
                .appointmentId(1).patientId(2).providerId(3).patientEmail("p@x.com")
                .slotId(4).serviceType("Consult").appointmentDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 30))
                .modeOfConsultation("ONLINE").status(status).build();
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}

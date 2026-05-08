package com.medibook.appointment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.medibook.appointment.client.SlotClient;
import com.medibook.appointment.dto.AppointmentRequest;
import com.medibook.appointment.dto.SlotDto;
import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.exception.BadRequestException;
import com.medibook.appointment.exception.ForbiddenException;
import com.medibook.appointment.exception.ResourceNotFoundException;
import com.medibook.appointment.messaging.AppointmentEventPublisher;
import com.medibook.appointment.repository.AppointmentRepository;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private SlotClient slotClient;
    @Mock private AppointmentEventPublisher eventPublisher;
    @InjectMocks private AppointmentServiceImpl appointmentService;

    private Appointment appointment;
    private SlotDto slot;

    @BeforeEach
    void setUp() {
        appointment = Appointment.builder()
                .appointmentId(1)
                .patientId(2)
                .providerId(3)
                .patientEmail("patient@medibook.com")
                .slotId(4)
                .serviceType("Consultation")
                .appointmentDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .modeOfConsultation("ONLINE")
                .status("SCHEDULED")
                .build();
        slot = new SlotDto();
        slot.setSlotId(4);
        slot.setProviderId(3);
        slot.setDate(appointment.getAppointmentDate());
        slot.setStartTime(appointment.getStartTime());
        slot.setEndTime(appointment.getEndTime());
    }

    @Test
    void bookAppointmentCreatesAppointmentBooksSlotAndIgnoresPublisherFailure() {
        AppointmentRequest request = request();
        when(slotClient.getSlotById(4)).thenReturn(slot);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        doThrow(new RuntimeException("rabbit down")).when(eventPublisher).publishBooked(appointment);

        Appointment saved = appointmentService.bookAppointment(request);

        assertThat(saved.getStatus()).isEqualTo("SCHEDULED");
        verify(slotClient).bookSlot(4);
        verify(appointmentRepository).save(argThat(a -> a.getProviderId() == 3 && a.getPatientId() == 2));
    }

    @Test
    void bookAppointmentRejectsUnavailableOrWrongProviderSlot() {
        AppointmentRequest request = request();
        when(slotClient.getSlotById(4)).thenReturn(slot);

        slot.setBooked(true);
        assertThatThrownBy(() -> appointmentService.bookAppointment(request)).isInstanceOf(BadRequestException.class);

        slot.setBooked(false);
        slot.setBlocked(true);
        assertThatThrownBy(() -> appointmentService.bookAppointment(request)).isInstanceOf(BadRequestException.class);

        slot.setBlocked(false);
        slot.setProviderId(99);
        assertThatThrownBy(() -> appointmentService.bookAppointment(request)).isInstanceOf(BadRequestException.class);

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void readMethodsUseRepository() {
        when(appointmentRepository.findById(1)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.findByPatientId(2)).thenReturn(List.of(appointment));
        when(appointmentRepository.findByProviderId(3)).thenReturn(List.of(appointment));
        when(appointmentRepository.findByProviderIdAndAppointmentDate(3, appointment.getAppointmentDate()))
                .thenReturn(List.of(appointment));
        when(appointmentRepository.findUpcomingByPatientId(anyInt(), any(LocalDate.class))).thenReturn(List.of(appointment));
        when(appointmentRepository.countByProviderId(3)).thenReturn(7L);

        assertThat(appointmentService.getById(1)).isSameAs(appointment);
        assertThat(appointmentService.getByPatient(2)).containsExactly(appointment);
        assertThat(appointmentService.getByProvider(3)).containsExactly(appointment);
        assertThat(appointmentService.getByProviderAndDate(3, appointment.getAppointmentDate())).containsExactly(appointment);
        assertThat(appointmentService.getUpcomingByPatient(2)).containsExactly(appointment);
        assertThat(appointmentService.getAppointmentCount(3)).isEqualTo(7);
    }

    @Test
    void cancelCompleteNoShowAndUpdateStatusMutateAppointment() {
        when(appointmentRepository.findById(1)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);

        appointmentService.cancelAppointment(1);
        verify(slotClient).releaseSlot(4);
        assertThat(appointment.getStatus()).isEqualTo("CANCELLED");
        clearInvocations(appointmentRepository);

        appointment.setStatus("SCHEDULED");
        appointmentService.completeAppointment(1, 3);
        assertThat(appointment.getStatus()).isEqualTo("COMPLETED");
        clearInvocations(appointmentRepository);

        appointment.setStatus("SCHEDULED");
        appointmentService.markNoShow(1, 3);
        assertThat(appointment.getStatus()).isEqualTo("NO_SHOW");
        clearInvocations(eventPublisher);

        appointmentService.updateStatus(1, "CANCELLED");
        verify(eventPublisher).publishCancelled(appointment);
    }

    @Test
    void stateValidationsThrowExpectedExceptions() {
        when(appointmentRepository.findById(404)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> appointmentService.getById(404)).isInstanceOf(ResourceNotFoundException.class);

        when(appointmentRepository.findById(1)).thenReturn(Optional.of(appointment));
        appointment.setStatus("COMPLETED");
        assertThatThrownBy(() -> appointmentService.cancelAppointment(1)).isInstanceOf(BadRequestException.class);

        appointment.setStatus("SCHEDULED");
        assertThatThrownBy(() -> appointmentService.completeAppointment(1, 99)).isInstanceOf(ForbiddenException.class);

        appointment.setStatus("CANCELLED");
        assertThatThrownBy(() -> appointmentService.markNoShow(1, 3)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void rescheduleReleasesOldSlotBooksNewSlotAndRejectsBookedNewSlot() {
        when(appointmentRepository.findById(1)).thenReturn(Optional.of(appointment));
        SlotDto newSlot = new SlotDto();
        newSlot.setSlotId(8);
        newSlot.setDate(LocalDate.now().plusDays(5));
        newSlot.setStartTime(LocalTime.of(12, 0));
        newSlot.setEndTime(LocalTime.of(12, 30));
        when(slotClient.getSlotById(8)).thenReturn(newSlot);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);

        Appointment updated = appointmentService.rescheduleAppointment(1, 8, null, null, null);

        assertThat(updated.getSlotId()).isEqualTo(8);
        verify(slotClient).releaseSlot(4);
        verify(slotClient).bookSlot(8);

        newSlot.setBooked(true);
        assertThatThrownBy(() -> appointmentService.rescheduleAppointment(1, 8, null, null, null))
                .isInstanceOf(BadRequestException.class);
    }

    private AppointmentRequest request() {
        AppointmentRequest request = new AppointmentRequest();
        request.setPatientId(2);
        request.setProviderId(3);
        request.setPatientEmail("patient@medibook.com");
        request.setSlotId(4);
        request.setServiceType("Consultation");
        request.setModeOfConsultation("ONLINE");
        request.setNotes("Headache");
        return request;
    }
}

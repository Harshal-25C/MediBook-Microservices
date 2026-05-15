package com.medibook.record.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.medibook.record.client.AppointmentClient;
import com.medibook.record.dto.AppointmentDto;
import com.medibook.record.dto.RecordRequest;
import com.medibook.record.entity.MedicalRecord;
import com.medibook.record.exception.BadRequestException;
import com.medibook.record.exception.DuplicateResourceException;
import com.medibook.record.exception.ResourceNotFoundException;
import com.medibook.record.repository.RecordRepository;

@ExtendWith(MockitoExtension.class)
class RecordServiceImplTest {

    @Mock private RecordRepository recordRepository;
    @Mock private AppointmentClient appointmentClient;
    @InjectMocks private RecordServiceImpl recordService;

    private MedicalRecord record;
    private RecordRequest request;
    private AppointmentDto completed;

    @BeforeEach
    void setUp() {
        request = new RecordRequest();
        request.setAppointmentId(10);
        request.setPatientId(2);
        request.setProviderId(3);
        request.setDiagnosis("Flu");
        request.setPrescription("Rest");
        request.setNotes("Follow up");
        request.setAttachmentUrl("file.pdf");
        request.setFollowUpDate(LocalDate.now().plusDays(7));
        completed = new AppointmentDto();
        completed.setStatus("COMPLETED");
        record = MedicalRecord.builder()
                .recordId(1)
                .appointmentId(10)
                .patientId(2)
                .providerId(3)
                .diagnosis("Flu")
                .prescription("Rest")
                .notes("Follow up")
                .attachmentUrl("file.pdf")
                .followUpDate(LocalDate.now().plusDays(7))
                .build();
    }

    @Test
    void createRecordValidatesAppointmentAndSaves() {
        when(appointmentClient.getById(10)).thenReturn(completed);
        when(recordRepository.existsByAppointmentId(10)).thenReturn(false);
        when(recordRepository.save(any(MedicalRecord.class))).thenReturn(record);

        MedicalRecord saved = recordService.createRecord(request);

        assertThat(saved.getDiagnosis()).isEqualTo("Flu");
        verify(recordRepository).save(argThat(r -> r.getAppointmentId() == 10 && r.getProviderId() == 3));
    }

    @Test
    void createRecordValidationFailuresThrow() {
        AppointmentDto scheduled = new AppointmentDto();
        scheduled.setStatus("SCHEDULED");
        when(appointmentClient.getById(10)).thenReturn(scheduled);
        assertThatThrownBy(() -> recordService.createRecord(request)).isInstanceOf(BadRequestException.class);

        completed.setStatus(null);
        when(appointmentClient.getById(10)).thenReturn(completed);
        assertThatThrownBy(() -> recordService.createRecord(request)).isInstanceOf(BadRequestException.class);

        completed.setStatus("COMPLETED");
        when(recordRepository.existsByAppointmentId(10)).thenReturn(true);
        assertThatThrownBy(() -> recordService.createRecord(request)).isInstanceOf(DuplicateResourceException.class);

        when(recordRepository.existsByAppointmentId(10)).thenReturn(false);
        request.setDiagnosis(" ");
        assertThatThrownBy(() -> recordService.createRecord(request)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void readMethodsUseRepository() {
        when(recordRepository.findByAppointmentId(10)).thenReturn(Optional.of(record));
        when(recordRepository.findByPatientIdOrderByCreatedAtDesc(2)).thenReturn(List.of(record));
        when(recordRepository.findByProviderId(3)).thenReturn(List.of(record));
        when(recordRepository.findByRecordId(1)).thenReturn(Optional.of(record));
        when(recordRepository.findByFollowUpDate(request.getFollowUpDate())).thenReturn(List.of(record));
        when(recordRepository.findUpcomingFollowUps(any(Integer.class), any(LocalDate.class))).thenReturn(List.of(record));
        when(recordRepository.countByPatientId(2)).thenReturn(5L);

        assertThat(recordService.getRecordByAppointment(10)).isSameAs(record);
        assertThat(recordService.getRecordsByPatient(2)).containsExactly(record);
        assertThat(recordService.getRecordsByProvider(3)).containsExactly(record);
        assertThat(recordService.getRecordById(1)).isSameAs(record);
        assertThat(recordService.getFollowUpRecords(request.getFollowUpDate())).containsExactly(record);
        assertThat(recordService.getUpcomingFollowUps(2)).containsExactly(record);
        assertThat(recordService.getRecordCount(2)).isEqualTo(5);
    }

    @Test
    void updateAttachAndDeleteRecordMutateRepositoryEntity() {
        when(recordRepository.findByRecordId(1)).thenReturn(Optional.of(record));
        when(recordRepository.save(any(MedicalRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        request.setDiagnosis("Updated");
        MedicalRecord updated = recordService.updateRecord(1, request);
        clearInvocations(recordRepository);

        recordService.attachDocument(1, "new.pdf");
        recordService.deleteRecord(1);

        assertThat(updated.getDiagnosis()).isEqualTo("Updated");
        verify(recordRepository).save(argThat(r -> "new.pdf".equals(r.getAttachmentUrl())));
        verify(recordRepository).deleteByRecordId(1);
    }

    @Test
    void recordValidationAndMissingPathsThrow() {
        when(recordRepository.findByRecordId(404)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> recordService.getRecordById(404)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> recordService.getFollowUpRecords(null)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> recordService.attachDocument(1, " ")).isInstanceOf(BadRequestException.class);

        when(recordRepository.findByRecordId(1)).thenReturn(Optional.of(record));
        request.setDiagnosis("");
        assertThatThrownBy(() -> recordService.updateRecord(1, request)).isInstanceOf(BadRequestException.class);
    }
}

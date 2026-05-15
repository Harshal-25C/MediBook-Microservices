
package com.medibook.record;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import com.medibook.record.client.NotificationClient;
import com.medibook.record.client.UserClient;
import com.medibook.record.dto.UserDto;
import com.medibook.record.entity.MedicalRecord;
import com.medibook.record.scheduler.FollowUpReminderScheduler;
import com.medibook.record.service.RecordService;

import org.junit.jupiter.api.Test;

class GeneratedRecordSchedulerCoverageTest {
    @Test
    void followUpSchedulerSendsReminderHandlesPerRecordAndFatalFailures() throws Exception {
        RecordService records = mock(RecordService.class);
        NotificationClient notifications = mock(NotificationClient.class);
        UserClient users = mock(UserClient.class);
        FollowUpReminderScheduler scheduler = new FollowUpReminderScheduler();
        set(scheduler, "recordService", records);
        set(scheduler, "notificationClient", notifications);
        set(scheduler, "userClient", users);
        MedicalRecord ok = MedicalRecord.builder().recordId(1).patientId(10).diagnosis("Checkup").build();
        MedicalRecord fail = MedicalRecord.builder().recordId(2).patientId(11).diagnosis("Labs").build();
        UserDto patient = new UserDto();
        patient.setFullName("Asha");
        patient.setEmail("asha@x.com");
        when(records.getFollowUpRecords(LocalDate.now())).thenReturn(List.of(ok, fail));
        when(users.getUserById(10)).thenReturn(patient);
        when(users.getUserById(11)).thenThrow(new RuntimeException("auth down"));

        assertThatCode(scheduler::sendFollowUpReminders).doesNotThrowAnyException();
        verify(notifications).send(any());

        when(records.getFollowUpRecords(LocalDate.now())).thenThrow(new RuntimeException("fatal"));
        assertThatCode(scheduler::sendFollowUpReminders).doesNotThrowAnyException();
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}

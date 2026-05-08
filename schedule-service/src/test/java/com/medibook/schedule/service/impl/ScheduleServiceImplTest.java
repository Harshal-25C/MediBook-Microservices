package com.medibook.schedule.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
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

import com.medibook.schedule.dto.request.SlotRequest;
import com.medibook.schedule.entity.AvailabilitySlot;
import com.medibook.schedule.exception.BadRequestException;
import com.medibook.schedule.exception.ResourceNotFoundException;
import com.medibook.schedule.repository.SlotRepository;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock private SlotRepository slotRepository;
    @InjectMocks private ScheduleServiceImpl scheduleService;

    private AvailabilitySlot slot;

    @BeforeEach
    void setUp() {
        slot = AvailabilitySlot.builder()
                .slotId(1)
                .providerId(10)
                .date(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .durationMinutes(30)
                .recurrence("NONE")
                .isBooked(false)
                .isBlocked(false)
                .build();
    }

    @Test
    void addSlotBulkAndRecurringSaveValidSlots() {
        when(slotRepository.save(any(AvailabilitySlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AvailabilitySlot saved = scheduleService.addSlot(request(LocalDate.now().plusDays(1)));
        List<AvailabilitySlot> bulk = scheduleService.addBulkSlots(List.of(request(LocalDate.now().plusDays(2))));
        SlotRequest recurring = request(LocalDate.now().plusDays(1));
        recurring.setRecurrence("DAILY");
        recurring.setRecurrenceEndDate(LocalDate.now().plusDays(3));
        List<AvailabilitySlot> generated = scheduleService.generateRecurringSlots(recurring);

        assertThat(saved.isBooked()).isFalse();
        assertThat(bulk).hasSize(1);
        assertThat(generated).hasSize(3);
    }

    @Test
    void createValidationsRejectPastEmptyAndBadRecurringRequests() {
        assertThatThrownBy(() -> scheduleService.addSlot(request(LocalDate.now().minusDays(1))))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> scheduleService.addBulkSlots(List.of()))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> scheduleService.generateRecurringSlots(request(LocalDate.now().plusDays(1))))
                .isInstanceOf(BadRequestException.class);

        SlotRequest request = request(LocalDate.now().plusDays(2));
        request.setRecurrence("MONTHLY");
        request.setRecurrenceEndDate(LocalDate.now().plusDays(3));
        assertThatThrownBy(() -> scheduleService.generateRecurringSlots(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void readMethodsReturnRepositoryResults() {
        when(slotRepository.findByProviderId(10)).thenReturn(List.of(slot));
        when(slotRepository.findAvailableByProviderAndDate(10, slot.getDate())).thenReturn(List.of(slot));
        when(slotRepository.findBySlotId(1)).thenReturn(Optional.of(slot));

        assertThat(scheduleService.getSlotsByProvider(10)).containsExactly(slot);
        assertThat(scheduleService.getAvailableSlots(10, slot.getDate())).containsExactly(slot);
        assertThat(scheduleService.getSlotById(1)).isSameAs(slot);
        assertThatThrownBy(() -> scheduleService.getAvailableSlots(10, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void bookReleaseBlockUnblockUpdateAndDeleteChangeSlotState() {
        when(slotRepository.findBySlotId(1)).thenReturn(Optional.of(slot));
        when(slotRepository.save(any(AvailabilitySlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        scheduleService.bookSlot(1);
        assertThat(slot.isBooked()).isTrue();
        clearInvocations(slotRepository);

        scheduleService.releaseSlot(1);
        assertThat(slot.isBooked()).isFalse();
        clearInvocations(slotRepository);

        scheduleService.blockSlot(1);
        assertThat(slot.isBlocked()).isTrue();
        clearInvocations(slotRepository);

        scheduleService.unblockSlot(1);
        assertThat(slot.isBlocked()).isFalse();
        clearInvocations(slotRepository);

        AvailabilitySlot updated = scheduleService.updateSlot(1, request(LocalDate.now().plusDays(5)));
        assertThat(updated.getDate()).isEqualTo(LocalDate.now().plusDays(5));

        scheduleService.deleteSlot(1);
        verify(slotRepository).deleteBySlotId(1);
    }

    @Test
    void invalidSlotStateOperationsThrow() {
        when(slotRepository.findBySlotId(404)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> scheduleService.getSlotById(404))
                .isInstanceOf(ResourceNotFoundException.class);

        slot.setBooked(true);
        when(slotRepository.findBySlotId(1)).thenReturn(Optional.of(slot));
        assertThatThrownBy(() -> scheduleService.bookSlot(1)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> scheduleService.blockSlot(1)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> scheduleService.updateSlot(1, request(LocalDate.now().plusDays(1))))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> scheduleService.deleteSlot(1)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void deleteExpiredSlotsDeletesEachExpiredSlot() {
        when(slotRepository.findExpiredSlots(any(LocalDate.class))).thenReturn(List.of(slot));

        scheduleService.deleteExpiredSlots();

        verify(slotRepository).deleteBySlotId(1);
    }

    private SlotRequest request(LocalDate date) {
        SlotRequest request = new SlotRequest();
        request.setProviderId(10);
        request.setDate(date);
        request.setStartTime(LocalTime.of(10, 0));
        request.setEndTime(LocalTime.of(10, 30));
        request.setDurationMinutes(30);
        request.setRecurrence("NONE");
        return request;
    }
}

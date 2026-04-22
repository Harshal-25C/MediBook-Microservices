package com.medibook.appointment.client;

import com.medibook.appointment.dto.SlotDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * Feign client to talk to slot-service.
 * Replaces direct ScheduleService @Autowired from monolith.
 * All calls go through api-gateway → slot-service.
 */
@FeignClient(name = "schedule-service")
public interface SlotClient {

    @GetMapping("/slots/{slotId}")
    SlotDto getSlotById(@PathVariable("slotId") int slotId);

    @PutMapping("/slots/{slotId}/book")
    void bookSlot(@PathVariable("slotId") int slotId);

    @PutMapping("/slots/{slotId}/release")
    void releaseSlot(@PathVariable("slotId") int slotId);
}
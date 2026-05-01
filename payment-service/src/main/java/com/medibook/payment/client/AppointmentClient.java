package com.medibook.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.medibook.payment.dto.request.AppointmentDto;

@FeignClient(name = "appointment-service")
public interface AppointmentClient {

    @GetMapping("/appointments/{appointmentId}")
    AppointmentDto getById(@PathVariable("appointmentId") int appointmentId);

    // FIX: After payment succeeds, set appointment to SCHEDULED (not COMPLETED)
    @PutMapping("/appointments/{appointmentId}/status")
    void updateStatus(
        @PathVariable("appointmentId") int appointmentId,
        @RequestParam("status") String status
    );
}

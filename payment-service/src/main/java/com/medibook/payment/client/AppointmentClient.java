package com.medibook.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import com.medibook.payment.dto.request.AppointmentDto;


// Feign client — payment-service calls appointment-service.
// Replaces direct @Autowired AppointmentService from monolith.

@FeignClient(name = "appointment-service")
public interface AppointmentClient {

    @GetMapping("/appointments/{appointmentId}")
    AppointmentDto getById(@PathVariable("appointmentId") int appointmentId);
    
    @PutMapping("/appointments/{appointmentId}/complete")
    void markCompleted(@PathVariable("appointmentId") int appointmentId);
}

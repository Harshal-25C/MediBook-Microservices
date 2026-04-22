package com.medibook.review.client;

import com.medibook.review.dto.request.AppointmentDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "appointment-service")
public interface AppointmentClient {

    @GetMapping("/appointments/{appointmentId}")
    AppointmentDto getById(@PathVariable("appointmentId") int appointmentId);
}

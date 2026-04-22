package com.medibook.notification.messaging;

import com.medibook.notification.dto.AppointmentEventDto;
import com.medibook.notification.dto.NotificationRequest;
import com.medibook.notification.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppointmentEventConsumer {

    @Autowired
    private NotificationService notificationService;
    @RabbitListener(queues = "medibook.appointment.booked")
    public void handleBooked(AppointmentEventDto event) {
        NotificationRequest req = new NotificationRequest();
        req.setRecipientId(event.getPatientId());
        req.setType("BOOKING");
        req.setTitle("Appointment Confirmed!");
        req.setMessage("Your appointment is confirmed for "
            + event.getAppointmentDate() + " at " + event.getStartTime());
        req.setChannel("APP");
        req.setRelatedType("APPOINTMENT");
        notificationService.send(req);
    }

    @RabbitListener(queues = "medibook.appointment.cancelled")
    public void handleCancelled(AppointmentEventDto event) {
        NotificationRequest req = new NotificationRequest();
        req.setRecipientId(event.getPatientId());
        req.setType("CANCELLATION");
        req.setTitle("Appointment Cancelled");
        req.setMessage("Your appointment on " + event.getAppointmentDate() + " has been cancelled.");
        req.setChannel("APP");
        req.setRelatedType("APPOINTMENT");
        notificationService.send(req);
    }

    @RabbitListener(queues = "medibook.appointment.completed")
    public void handleCompleted(AppointmentEventDto event) {
        NotificationRequest req = new NotificationRequest();
        req.setRecipientId(event.getPatientId());
        req.setType("BOOKING");
        req.setTitle("Appointment Completed");
        req.setMessage("Your appointment has been completed. Thank you for choosing MediBook!");
        req.setChannel("APP");
        req.setRelatedType("APPOINTMENT");
        notificationService.send(req);
    }
}
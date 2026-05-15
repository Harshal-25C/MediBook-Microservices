package com.medibook.appointment.messaging;

import com.medibook.appointment.config.RabbitMQConfig;
import com.medibook.appointment.dto.AppointmentEventDto;
import com.medibook.appointment.entity.Appointment;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Publishes appointment lifecycle events to RabbitMQ.
 * Called by AppointmentServiceImpl after every state change.
 *
 * Flow:
 *   bookAppointment()    → publishBooked()    → QUEUE_BOOKED
 *   cancelAppointment()  → publishCancelled() → QUEUE_CANCELLED
 *   completeAppointment()→ publishCompleted() → QUEUE_COMPLETED
 */
@Component
public class AppointmentEventPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void publishBooked(Appointment appointment) {
        AppointmentEventDto event = buildEvent(appointment, "BOOKED",
                "Your appointment on " + appointment.getAppointmentDate()
                + " at " + appointment.getStartTime());
        publish(RabbitMQConfig.KEY_BOOKED, event);
    }

    public void publishCancelled(Appointment appointment) {
        AppointmentEventDto event = buildEvent(appointment, "CANCELLED",
            "Your appointment on " + appointment.getAppointmentDate()
            + " at " + appointment.getStartTime() + " has been cancelled.");
        publish(RabbitMQConfig.KEY_CANCELLED, event);
        System.out.println("[RabbitMQ] Published CANCELLED event → appointmentId=" + appointment.getAppointmentId());
    }

    public void publishCompleted(Appointment appointment) {
        AppointmentEventDto event = buildEvent(appointment, "COMPLETED",
            "Your appointment on " + appointment.getAppointmentDate()
            + " has been marked as completed. Please check your medical records.");
        publish(RabbitMQConfig.KEY_COMPLETED, event);
        System.out.println("[RabbitMQ] Published COMPLETED event → appointmentId=" + appointment.getAppointmentId());
    }

    private void publish(String routingKey, AppointmentEventDto event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, routingKey, event);
    }

    private AppointmentEventDto buildEvent(Appointment a, String eventType, String message) {
        return AppointmentEventDto.builder()
                .appointmentId(a.getAppointmentId())
                .patientId(a.getPatientId())
                .providerId(a.getProviderId())
                .eventType(eventType)
                .serviceType(a.getServiceType())
                .modeOfConsultation(a.getModeOfConsultation())
                .appointmentDate(a.getAppointmentDate().toString())
                .startTime(a.getStartTime().toString())
                .endTime(a.getEndTime().toString())
                .message(message)
                .build();
    }
    
}
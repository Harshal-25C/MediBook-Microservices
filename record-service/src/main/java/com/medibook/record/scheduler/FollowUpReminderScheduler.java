package com.medibook.record.scheduler;

import com.medibook.record.client.NotificationClient;
import com.medibook.record.client.UserClient;
import com.medibook.record.dto.NotificationDto;
import com.medibook.record.dto.UserDto;
import com.medibook.record.entity.MedicalRecord;
import com.medibook.record.service.RecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Runs every morning at 8:00 AM.
 * Finds all medical records with followUpDate = today.
 * Sends email reminder to each patient via notification-service.
 *
 * Microservices version — replaces all direct @Autowired with Feign clients:
 *   UserRepository     → UserClient     (calls auth-service)
 *   NotificationService → NotificationClient (calls notification-service)
 *   RecordService stays local — same service
 */
@Component
public class FollowUpReminderScheduler {

    /** Local — RecordService lives in this same service */
    @Autowired
    private RecordService recordService;

    /**
     * Replaces direct @Autowired NotificationService.
     * Calls notification-service via Feign: POST /notifications/send
     */
    @Autowired
    private NotificationClient notificationClient;

    /**
     * Replaces direct @Autowired UserRepository.
     * Calls auth-service via Feign: GET /auth/profile/{userId}
     */
    @Autowired
    private UserClient userClient;

    @Scheduled(cron = "0 0 8 * * *")
    public void sendFollowUpReminders() {

        System.out.println("FollowUpReminderScheduler → Running at 8 AM. Checking follow-up records...");

        try {
            LocalDate today = LocalDate.now();

            List<MedicalRecord> recordsDueToday = recordService.getFollowUpRecords(today);

            System.out.println("FollowUpReminderScheduler → Records with follow-up today: "
                    + recordsDueToday.size());

            for (MedicalRecord record : recordsDueToday) {
                try {
                    // Feign call → auth-service GET /auth/profile/{patientId}
                    // Replaces: userRepository.findById(record.getPatientId())
                    UserDto patient = userClient.getUserById(record.getPatientId());

                    // Build notification request DTO
                    NotificationDto request = new NotificationDto();
                    request.setRecipientId(record.getPatientId());
                    request.setType("FOLLOWUP");
                    request.setTitle("Follow-Up Reminder 🏥");
                    request.setMessage(
                        "Dear " + patient.getFullName() + ", " +
                        "this is a reminder that today is your scheduled follow-up date. " +
                        "Please contact your doctor or visit the clinic. " +
                        "Diagnosis: " + record.getDiagnosis() + ". - MediBook Team"
                    );
                    request.setChannel("EMAIL");
                    request.setRelatedId(record.getRecordId());
                    request.setRelatedType("RECORD");

                    // Feign call → notification-service POST /notifications/send
                    // Replaces: notificationService.send(request)
                    notificationClient.send(request);

                    System.out.println("FollowUpReminderScheduler → Reminder sent to: "
                            + patient.getEmail() + " for recordId: " + record.getRecordId());

                } catch (Exception e) {
                    // one patient failing must NOT stop the others
                    System.err.println("FollowUpReminderScheduler → Failed for recordId: "
                            + record.getRecordId() + " | Error: " + e.getMessage());
                }
            }

            System.out.println("FollowUpReminderScheduler → Done. All follow-up reminders processed.");

        } catch (Exception e) {
            System.err.println("FollowUpReminderScheduler → Fatal error: " + e.getMessage());
        }
    }
}

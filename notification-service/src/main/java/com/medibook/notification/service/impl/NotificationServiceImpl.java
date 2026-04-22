package com.medibook.notification.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.medibook.notification.client.UserClient;
import com.medibook.notification.dto.NotificationRequest;
import com.medibook.notification.dto.UserDto;
import com.medibook.notification.entity.Notification;
import com.medibook.notification.exception.BadRequestException;
import com.medibook.notification.exception.ResourceNotFoundException;
import com.medibook.notification.repository.NotificationRepository;
import com.medibook.notification.service.NotificationService;

@Service
public class NotificationServiceImpl implements NotificationService {
	
    @Autowired
    private UserClient userClient;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Override
    public Notification send(NotificationRequest request) {
        String channel = request.getChannel();
        if(!channel.equals("APP") && !channel.equals("EMAIL") && !channel.equals("SMS"))
            throw new BadRequestException("Invalid channel. Allowed values: APP, EMAIL, SMS");

        String type = request.getType();
        if(!type.equals("BOOKING") && !type.equals("REMINDER") && !type.equals("CANCELLATION")
                && !type.equals("PAYMENT") && !type.equals("FOLLOWUP") && !type.equals("ANNOUNCEMENT"))
            throw new BadRequestException("Invalid type.");

        Notification notification = Notification.builder()
                .recipientId(request.getRecipientId())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .channel(request.getChannel())
                .relatedId(request.getRelatedId())
                .relatedType(request.getRelatedType())
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        if("EMAIL".equals(request.getChannel()) && emailEnabled) {
            try{
                String toEmail = request.getEmail();
                if(toEmail != null && !toEmail.isEmpty()) {
                    sendEmail(toEmail, request.getTitle(), request.getMessage());
                }else {
                    UserDto user = userClient.getUserById(request.getRecipientId());
                    sendEmail(user.getEmail(), request.getTitle(), request.getMessage());
                }
            }catch (Exception e) {
                System.out.println("[NotificationService] Email failed: " + e.getMessage());
            }
        }

        if("SMS".equals(request.getChannel()) && smsEnabled) {
            sendSms("recipient_phone", request.getMessage());
        }

        return saved;
    }

    @Override
    public void sendBulk(List<Integer> recipientIds, String title, String message) {
        if(recipientIds == null || recipientIds.isEmpty())
            throw new BadRequestException("Recipient list cannot be empty.");
        if(title == null || title.trim().isEmpty())
            throw new BadRequestException("Title cannot be empty.");
        if(message == null || message.trim().isEmpty())
            throw new BadRequestException("Message cannot be empty.");

        for(int recipientId : recipientIds) {
            NotificationRequest request = new NotificationRequest();
            request.setRecipientId(recipientId);
            request.setType("ANNOUNCEMENT");
            request.setTitle(title);
            request.setMessage(message);
            request.setChannel("APP");
            send(request);
        }
    }

    @Override
    public void markAsRead(int notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        if(notification.isRead())
            throw new BadRequestException("Notification is already marked as read.");
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public void markAllRead(int recipientId) {
        notificationRepository.markAllAsRead(recipientId);
    }

    @Override
    public List<Notification> getByRecipient(int recipientId) {
        return notificationRepository.findByRecipientIdOrderBySentAtDesc(recipientId);
    }

    @Override
    public long getUnreadCount(int recipientId) {
        return notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
    }

    @Override
    public void deleteNotification(int notificationId) {
        notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        notificationRepository.deleteByNotificationId(notificationId);
    }

    @Override
    public void sendEmail(String toEmail, String subject, String body) {
        if(toEmail == null || toEmail.trim().isEmpty())
            throw new BadRequestException("Recipient email cannot be empty.");
        try{
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(senderEmail);
            mail.setTo(toEmail);
            mail.setSubject(subject);
            mail.setText(body);
            mailSender.send(mail);
            System.out.println("[NotificationService] Email sent to: " + toEmail);
        }catch (Exception e) {
            System.out.println("[NotificationService] Email failed to: " + toEmail + " | " + e.getMessage());
        }
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        if(phoneNumber == null || phoneNumber.trim().isEmpty())
            throw new BadRequestException("Phone number cannot be empty.");
        System.out.println("SMS MOCK → To: " + phoneNumber + " | Message: " + message);
    }

    @Override
    public List<Notification> getAll() {
        return notificationRepository.findAllByOrderBySentAtDesc();
    }
}

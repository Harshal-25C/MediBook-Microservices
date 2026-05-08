package com.medibook.notification.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import com.medibook.notification.client.UserClient;
import com.medibook.notification.dto.NotificationRequest;
import com.medibook.notification.dto.UserDto;
import com.medibook.notification.entity.Notification;
import com.medibook.notification.exception.BadRequestException;
import com.medibook.notification.exception.ResourceNotFoundException;
import com.medibook.notification.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private UserClient userClient;
    @Mock private NotificationRepository notificationRepository;
    @Mock private JavaMailSender mailSender;
    @InjectMocks private NotificationServiceImpl notificationService;

    private Notification notification;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "emailEnabled", true);
        ReflectionTestUtils.setField(notificationService, "smsEnabled", true);
        ReflectionTestUtils.setField(notificationService, "senderEmail", "noreply@medibook.com");
        notification = Notification.builder()
                .notificationId(1)
                .recipientId(2)
                .type("BOOKING")
                .title("Booked")
                .message("Appointment booked")
                .channel("APP")
                .isRead(false)
                .build();
    }

    @Test
    void sendAppEmailAndSmsNotificationsSaveValidRequests() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserDto user = new UserDto();
        user.setEmail("patient@medibook.com");
        when(userClient.getUserById(2)).thenReturn(user);

        Notification app = notificationService.send(request("APP", "BOOKING", null));
        Notification email = notificationService.send(request("EMAIL", "PAYMENT", null));
        Notification sms = notificationService.send(request("SMS", "REMINDER", null));

        assertThat(app.getChannel()).isEqualTo("APP");
        assertThat(email.getChannel()).isEqualTo("EMAIL");
        assertThat(sms.getChannel()).isEqualTo("SMS");
    }

    @Test
    void sendRejectsInvalidChannelAndType() {
        assertThatThrownBy(() -> notificationService.send(request("PUSH", "BOOKING", null)))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> notificationService.send(request("APP", "UNKNOWN", null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void bulkSendCreatesAppAndEmailNotificationsForEachRecipient() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.sendBulk(List.of(2, 3), "Announcement", "Hello");

        verify(notificationRepository).save(argThat(n -> n.getRecipientId() == 2 && "APP".equals(n.getChannel())));
        verify(notificationRepository).save(argThat(n -> n.getRecipientId() == 3 && "EMAIL".equals(n.getChannel())));
    }

    @Test
    void markReadDeleteAndQueryMethodsUseRepository() {
        when(notificationRepository.findById(1)).thenReturn(Optional.of(notification));
        when(notificationRepository.findByRecipientIdOrderBySentAtDesc(2)).thenReturn(List.of(notification));
        when(notificationRepository.countByRecipientIdAndIsRead(2, false)).thenReturn(3L);
        when(notificationRepository.findAllByOrderBySentAtDesc()).thenReturn(List.of(notification));

        notificationService.markAsRead(1);
        notificationService.markAllRead(2);
        notificationService.deleteNotification(1);

        assertThat(notificationService.getByRecipient(2)).containsExactly(notification);
        assertThat(notificationService.getUnreadCount(2)).isEqualTo(3);
        assertThat(notificationService.getAll()).containsExactly(notification);
        verify(notificationRepository).save(argThat(Notification::isRead));
        verify(notificationRepository).markAllAsRead(2);
        verify(notificationRepository).deleteByNotificationId(1);
    }

    @Test
    void markReadAndDeleteValidationPathsThrow() {
        notification.setRead(true);
        when(notificationRepository.findById(1)).thenReturn(Optional.of(notification));
        when(notificationRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(1)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> notificationService.deleteNotification(404)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> notificationService.sendEmail(" ", "Subject", "Body")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> notificationService.sendSms("", "Body")).isInstanceOf(BadRequestException.class);
    }

    private NotificationRequest request(String channel, String type, String email) {
        NotificationRequest request = new NotificationRequest();
        request.setRecipientId(2);
        request.setType(type);
        request.setTitle("Title");
        request.setMessage("Message");
        request.setChannel(channel);
        request.setEmail(email);
        request.setRelatedId(99);
        request.setRelatedType("APPOINTMENT");
        return request;
    }
}

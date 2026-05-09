package com.medibook.notification.service.impl;
 
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
 
import java.util.List;
import java.util.Optional;
 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
 
import com.medibook.notification.client.UserClient;
import com.medibook.notification.dto.NotificationRequest;
import com.medibook.notification.dto.UserDto;
import com.medibook.notification.entity.Notification;
import com.medibook.notification.exception.BadRequestException;
import com.medibook.notification.exception.ResourceNotFoundException;
import com.medibook.notification.repository.NotificationRepository;
 
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {
 
    @Mock private NotificationRepository notificationRepository;
    @Mock private JavaMailSender mailSender;
    @Mock private UserClient userClient;
 
    private NotificationServiceImpl service;
 
    private Notification unreadNotification;
    private Notification readNotification;
 
    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl();
        injectField(service, "notificationRepository", notificationRepository);
        injectField(service, "mailSender", mailSender);
        injectField(service, "userClient", userClient);
        injectField(service, "emailEnabled", true);
        injectField(service, "smsEnabled", false);
        injectField(service, "senderEmail", "noreply@medibook.com");
 
        unreadNotification = Notification.builder()
                .notificationId(1).recipientId(10)
                .type("BOOKING").title("Appointment Booked")
                .message("Your appointment is confirmed.")
                .channel("APP").isRead(false)
                .build();
 
        readNotification = Notification.builder()
                .notificationId(2).recipientId(10)
                .type("REMINDER").title("Reminder")
                .message("Appointment tomorrow.")
                .channel("APP").isRead(true)
                .build();
    }
 
    private void injectField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
 
    // ─────────────────────────── send ────────────────────────────────────
 
    @Test
    void send_appChannel_savesAndReturns() {
        NotificationRequest req = buildRequest("APP", "BOOKING", null);
        when(notificationRepository.save(any())).thenReturn(unreadNotification);
 
        Notification result = service.send(req);
 
        assertThat(result.getChannel()).isEqualTo("APP");
        assertThat(result.isRead()).isFalse();
        verify(notificationRepository).save(any(Notification.class));
    }
 
    @Test
    void send_emailChannelWithEmail_sendsEmail() {
        NotificationRequest req = buildRequest("EMAIL", "BOOKING", "patient@x.com");
        when(notificationRepository.save(any())).thenReturn(unreadNotification);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
 
        service.send(req);
 
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
 
    @Test
    void send_emailChannelWithoutEmail_fetchesUserAndSendsEmail() {
        NotificationRequest req = buildRequest("EMAIL", "BOOKING", null);
        UserDto userDto = new UserDto();
        userDto.setEmail("fetched@x.com");
 
        when(notificationRepository.save(any())).thenReturn(unreadNotification);
        when(userClient.getUserById(req.getRecipientId())).thenReturn(userDto);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
 
        service.send(req);
 
        verify(userClient).getUserById(req.getRecipientId());
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
 
    @Test
    void send_emailChannelMailFails_doesNotThrow() {
        NotificationRequest req = buildRequest("EMAIL", "BOOKING", "patient@x.com");
        when(notificationRepository.save(any())).thenReturn(unreadNotification);
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));
 
        // Email failure should NOT propagate
        assertThatCode(() -> service.send(req)).doesNotThrowAnyException();
    }
 
    @Test
    void send_smsChannel_enabledSmsDisabled_doesNotSendSms() {
        injectField(service, "smsEnabled", false);
        NotificationRequest req = buildRequest("SMS", "REMINDER", null);
        when(notificationRepository.save(any())).thenReturn(unreadNotification);
 
        service.send(req);
 
        // No SMS sent since disabled
        verify(notificationRepository).save(any());
    }
 
    @Test
    void send_smsChannel_enabled_printsMockSms() {
        injectField(service, "smsEnabled", true);
        NotificationRequest req = buildRequest("SMS", "REMINDER", null);
        when(notificationRepository.save(any())).thenReturn(unreadNotification);
 
        assertThatCode(() -> service.send(req)).doesNotThrowAnyException();
    }
 
    @Test
    void send_invalidChannel_throwsBadRequestException() {
        NotificationRequest req = buildRequest("PUSH", "BOOKING", null);
 
        assertThatThrownBy(() -> service.send(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid channel");
    }
 
    @Test
    void send_invalidType_throwsBadRequestException() {
        NotificationRequest req = buildRequest("APP", "INVALID_TYPE", null);
 
        assertThatThrownBy(() -> service.send(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid type");
    }
 
    @Test
    void send_allValidTypes_doNotThrow() {
        for (String type : List.of("BOOKING","REMINDER","CANCELLATION","PAYMENT","FOLLOWUP","ANNOUNCEMENT")) {
            NotificationRequest req = buildRequest("APP", type, null);
            when(notificationRepository.save(any())).thenReturn(unreadNotification);
            assertThatCode(() -> service.send(req)).doesNotThrowAnyException();
        }
    }
 
    // ─────────────────────────── sendBulk ────────────────────────────────
 
    @Test
    void sendBulk_validRecipients_sendsToEach() {
        when(notificationRepository.save(any())).thenReturn(unreadNotification);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        when(userClient.getUserById(anyInt())).thenReturn(new UserDto() {{ setEmail("u@x.com"); }});
 
        service.sendBulk(List.of(1, 2), "Announcement", "System update tonight");
 
        // 2 recipients × 2 channels (APP + EMAIL) = 4 saves
        verify(notificationRepository, atLeast(2)).save(any());
    }
 
    @Test
    void sendBulk_emptyList_doesNothing() {
        service.sendBulk(List.of(), "Title", "Msg");
        verify(notificationRepository, never()).save(any());
    }
 
    @Test
    void sendBulk_nullList_doesNothing() {
        service.sendBulk(null, "Title", "Msg");
        verify(notificationRepository, never()).save(any());
    }
 
    @Test
    void sendBulk_emptyTitle_doesNothing() {
        service.sendBulk(List.of(1), "", "Msg");
        verify(notificationRepository, never()).save(any());
    }
 
    @Test
    void sendBulk_emptyMessage_doesNothing() {
        service.sendBulk(List.of(1), "Title", "");
        verify(notificationRepository, never()).save(any());
    }
 
    @Test
    void sendBulk_oneRecipientFails_continuesForOthers() {
        // First save throws, second should still be attempted
        when(notificationRepository.save(any()))
                .thenThrow(new RuntimeException("DB error"))
                .thenReturn(unreadNotification);
 
        // Should not propagate exception
        assertThatCode(() -> service.sendBulk(List.of(1, 2), "Title", "Msg"))
                .doesNotThrowAnyException();
    }
 
    // ─────────────────────────── markAsRead ──────────────────────────────
 
    @Test
    void markAsRead_unread_setsReadTrueAndSaves() {
        when(notificationRepository.findById(1)).thenReturn(Optional.of(unreadNotification));
        when(notificationRepository.save(any())).thenReturn(unreadNotification);
 
        service.markAsRead(1);
 
        assertThat(unreadNotification.isRead()).isTrue();
        verify(notificationRepository).save(unreadNotification);
    }
 
    @Test
    void markAsRead_alreadyRead_throwsBadRequestException() {
        when(notificationRepository.findById(2)).thenReturn(Optional.of(readNotification));
 
        assertThatThrownBy(() -> service.markAsRead(2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already marked as read");
    }
 
    @Test
    void markAsRead_notFound_throwsResourceNotFoundException() {
        when(notificationRepository.findById(99)).thenReturn(Optional.empty());
 
        assertThatThrownBy(() -> service.markAsRead(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }
 
    // ─────────────────────────── markAllRead ─────────────────────────────
 
    @Test
    void markAllRead_callsRepositoryMethod() {
        doNothing().when(notificationRepository).markAllAsRead(10);
 
        service.markAllRead(10);
 
        verify(notificationRepository).markAllAsRead(10);
    }
 
    // ─────────────────────────── getByRecipient ──────────────────────────
 
    @Test
    void getByRecipient_returnsList() {
        when(notificationRepository.findByRecipientIdOrderBySentAtDesc(10))
                .thenReturn(List.of(unreadNotification, readNotification));
 
        assertThat(service.getByRecipient(10)).hasSize(2);
    }
 
    // ─────────────────────────── getUnreadCount ──────────────────────────
 
    @Test
    void getUnreadCount_returnsCount() {
        when(notificationRepository.countByRecipientIdAndIsRead(10, false)).thenReturn(3L);
        assertThat(service.getUnreadCount(10)).isEqualTo(3L);
    }
 
    // ─────────────────────────── deleteNotification ──────────────────────
 
    @Test
    void deleteNotification_exists_deletes() {
        when(notificationRepository.findById(1)).thenReturn(Optional.of(unreadNotification));
        doNothing().when(notificationRepository).deleteByNotificationId(1);
 
        service.deleteNotification(1);
 
        verify(notificationRepository).deleteByNotificationId(1);
    }
 
    @Test
    void deleteNotification_notFound_throwsResourceNotFoundException() {
        when(notificationRepository.findById(99)).thenReturn(Optional.empty());
 
        assertThatThrownBy(() -> service.deleteNotification(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }
 
    // ─────────────────────────── sendEmail ───────────────────────────────
 
    @Test
    void sendEmail_validInput_sendsAndDoesNotThrow() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
 
        assertThatCode(() -> service.sendEmail("dest@x.com", "Subject", "Body"))
                .doesNotThrowAnyException();
 
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
 
    @Test
    void sendEmail_emptyEmail_throwsBadRequestException() {
        assertThatThrownBy(() -> service.sendEmail("", "Subject", "Body"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("email cannot be empty");
    }
 
    @Test
    void sendEmail_nullEmail_throwsBadRequestException() {
        assertThatThrownBy(() -> service.sendEmail(null, "Subject", "Body"))
                .isInstanceOf(BadRequestException.class);
    }
 
    @Test
    void sendEmail_mailSenderThrows_doesNotPropagate() {
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));
 
        assertThatCode(() -> service.sendEmail("valid@x.com", "Sub", "Msg"))
                .doesNotThrowAnyException();
    }
 
    // ─────────────────────────── sendSms ─────────────────────────────────
 
    @Test
    void sendSms_validInput_doesNotThrow() {
        assertThatCode(() -> service.sendSms("9876543210", "Test message"))
                .doesNotThrowAnyException();
    }
 
    @Test
    void sendSms_emptyPhone_throwsBadRequestException() {
        assertThatThrownBy(() -> service.sendSms("", "Test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Phone number cannot be empty");
    }
 
    @Test
    void sendSms_nullPhone_throwsBadRequestException() {
        assertThatThrownBy(() -> service.sendSms(null, "Test"))
                .isInstanceOf(BadRequestException.class);
    }
 
    // ─────────────────────────── getAll ──────────────────────────────────
 
    @Test
    void getAll_returnsList() {
        when(notificationRepository.findAllByOrderBySentAtDesc())
                .thenReturn(List.of(unreadNotification, readNotification));
 
        assertThat(service.getAll()).hasSize(2);
    }
 
    // ─────────────────────────── helpers ─────────────────────────────────
 
    private NotificationRequest buildRequest(String channel, String type, String email) {
        NotificationRequest req = new NotificationRequest();
        req.setRecipientId(10);
        req.setType(type);
        req.setTitle("Test Title");
        req.setMessage("Test message body");
        req.setChannel(channel);
        req.setEmail(email);
        return req;
    }
}

package com.medibook.notification.service;

import com.medibook.notification.dto.NotificationRequest;
import com.medibook.notification.entity.Notification;

import java.util.List;

public interface NotificationService {

    Notification send(NotificationRequest request);

    void sendBulk(List<Integer> recipientIds,
                  String title, String message);

    void markAsRead(int notificationId);

    void markAllRead(int recipientId);

    List<Notification> getByRecipient(int recipientId);

    long getUnreadCount(int recipientId);

    void deleteNotification(int notificationId);

    void sendEmail(String toEmail,
                   String subject, String body);

    void sendSms(String phoneNumber, String message);

    List<Notification> getAll();
}
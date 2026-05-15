package com.medibook.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int notificationId;

    /*
     * Who receives this notification?
     * Links to userId in users table (UC1).
     */
    @Column(nullable = false)
    private int recipientId;

    /*
     * Type of notification.
     * BOOKING/REMINDER/CANCELLATION/PAYMENT/FOLLOWUP
     */
    @Column(nullable = false)
    private String type;

    /*
     * Short heading shown in notification bell.
     * Example: "Appointment Confirmed"
     */
    @Column(nullable = false)
    private String title;

    /*
     * Full notification message.
     * Example: "Your appointment with Dr. Sharma
     * is confirmed for April 10 at 10:00 AM"
     */
    @Column(nullable = false, length = 1000)
    private String message;

    /*
     * Which channel was used?
     * APP / EMAIL / SMS
     */
    @Column(nullable = false)
    private String channel;

    /*
     * Which record does this relate to?
     * Example: appointmentId = 5
     * Used for deep linking from notification centre.
     */
    private int relatedId;

    /*
     * What type of record?
     * Example: APPOINTMENT / PAYMENT / RECORD
     */
    private String relatedType;

    /*
     * Has patient read this notification?
     * false → unread → show in badge count
     * true  → read → not counted in badge
     * Default false — all new notifications unread.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean isRead = false;

    /*
     * When was this notification sent?
     * Auto set by @PrePersist.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @PrePersist
    public void prePersist() {
        this.sentAt = LocalDateTime.now();
    }
}
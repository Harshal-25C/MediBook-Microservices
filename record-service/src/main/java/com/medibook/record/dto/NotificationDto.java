package com.medibook.record.dto;

import lombok.Data;

@Data
public class NotificationDto {
    private int recipientId;
    private String type;
    private String title;
    private String message;
    private String channel;
    private int relatedId;
    private String relatedType;
}

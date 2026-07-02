package com.portfolio.memo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Stream key/group/consumer/poll 설정 관리
@Getter
@Setter
@ConfigurationProperties(prefix = "notification.stream")
public class NotificationStreamProperties {
    private String key = "stream:task:events:0";
    private String group = "cg:notification";
    private String consumer = "notification-1";
    private int batchSize = 50;
    private long pollTimeoutMs = 2000;
}

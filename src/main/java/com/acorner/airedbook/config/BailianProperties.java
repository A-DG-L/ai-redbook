package com.acorner.airedbook.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.bailian")
public class BailianProperties {
    private String apiKey;
    private String endpoint;
    private String model;
}
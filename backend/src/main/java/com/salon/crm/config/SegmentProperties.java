package com.salon.crm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "crm.segments")
@Data
public class SegmentProperties {
    private int newDays = 30;
    private int atRiskMinDays = 45;
    private int atRiskMaxDays = 60;
    private int lapsedDays = 60;
    private long vipSpend = 20000;
    private int vipVisits = 10;
}

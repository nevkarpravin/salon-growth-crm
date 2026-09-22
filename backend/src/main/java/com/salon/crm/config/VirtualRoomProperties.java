package com.salon.crm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "crm.virtual-room")
@Data
public class VirtualRoomProperties {
    private String salonName = "Glow Salon";
    private int notifyAtPosition = 2;
    private int requeuePositions = 2;
    private int maxSkips = 2;
    private int presenceTimeoutMinutes = 5;
    private int reviewDelayMinutes = 0;
    private String googleReviewUrl = "";
    private String publicBaseUrl = "http://localhost:5173";
    private Payments payments = new Payments();
    private Whatsapp whatsapp = new Whatsapp();

    @Data
    public static class Payments {
        private String upiId = "glowsalon@upi";
        private String payeeName = "Glow Salon";
    }

    @Data
    public static class Whatsapp {
        private String provider = "LOG";
        private String accessToken = "";
        private String phoneNumberId = "";
        private String verifyToken = "salon-verify";
    }
}

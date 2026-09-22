package com.salon.crm.service;

import com.salon.crm.config.VirtualRoomProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class PaymentService {

    private final VirtualRoomProperties properties;

    public PaymentService(VirtualRoomProperties properties) {
        this.properties = properties;
    }

    public String upiIntentLink(BigDecimal amount, String note) {
        String am = (amount != null ? amount : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP).toPlainString();
        return "upi://pay?pa=" + properties.getPayments().getUpiId()
                + "&pn=" + encode(properties.getPayments().getPayeeName())
                + "&am=" + am
                + "&cu=INR"
                + "&tn=" + encode(note != null ? note : "Salon bill");
    }

    private String encode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }
}

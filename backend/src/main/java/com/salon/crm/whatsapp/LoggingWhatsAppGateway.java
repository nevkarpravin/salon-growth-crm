package com.salon.crm.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "crm.virtual-room.whatsapp.provider", havingValue = "LOG",
        matchIfMissing = true)
public class LoggingWhatsAppGateway implements WhatsAppGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingWhatsAppGateway.class);

    @Override
    public SendResult send(String toPhone, String body) {
        log.info("WhatsApp -> {}: {}", toPhone, body);
        return new SendResult(false, null, null);
    }
}

package com.salon.crm.whatsapp;

public interface WhatsAppGateway {

    SendResult send(String toPhone, String body);

    record SendResult(boolean sent, String providerMessageId, String error) {
    }
}

package com.salon.crm.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.salon.crm.config.VirtualRoomProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "crm.virtual-room.whatsapp.provider", havingValue = "META")
public class MetaCloudWhatsAppGateway implements WhatsAppGateway {

    private static final Logger log = LoggerFactory.getLogger(MetaCloudWhatsAppGateway.class);

    private final VirtualRoomProperties properties;
    private final RestClient restClient;

    public MetaCloudWhatsAppGateway(VirtualRoomProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.build();
    }

    @Override
    public SendResult send(String toPhone, String body) {
        try {
            String url = "https://graph.facebook.com/v20.0/"
                    + properties.getWhatsapp().getPhoneNumberId() + "/messages";
            JsonNode response = restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + properties.getWhatsapp().getAccessToken())
                    .body(Map.of(
                            "messaging_product", "whatsapp",
                            "to", toPhone,
                            "type", "text",
                            "text", Map.of("body", body)))
                    .retrieve()
                    .body(JsonNode.class);
            String messageId = response != null
                    ? response.path("messages").path(0).path("id").asText(null)
                    : null;
            return new SendResult(true, messageId, null);
        } catch (Exception e) {
            log.warn("Meta WhatsApp send to {} failed: {}", toPhone, e.getMessage());
            return new SendResult(false, null, e.getMessage());
        }
    }
}

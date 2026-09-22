package com.salon.crm.whatsapp;

import com.salon.crm.entity.OutboundMessage;
import com.salon.crm.entity.QueueTicket;
import com.salon.crm.repository.OutboundMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessagingService {

    private final WhatsAppGateway gateway;
    private final OutboundMessageRepository outboundMessageRepository;

    public MessagingService(WhatsAppGateway gateway,
                            OutboundMessageRepository outboundMessageRepository) {
        this.gateway = gateway;
        this.outboundMessageRepository = outboundMessageRepository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public OutboundMessage send(String phone, String body, QueueTicket ticket) {
        WhatsAppGateway.SendResult result = gateway.send(phone, body);
        OutboundMessage message = new OutboundMessage();
        message.setToPhone(phone);
        message.setBody(body);
        message.setChannel("WHATSAPP");
        message.setTicket(ticket);
        message.setProviderMessageId(result.providerMessageId());
        message.setError(result.error());
        message.setStatus(result.sent() ? "SENT" : (result.error() != null ? "FAILED" : "LOGGED"));
        return outboundMessageRepository.save(message);
    }
}

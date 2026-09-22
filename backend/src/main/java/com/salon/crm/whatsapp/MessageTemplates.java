package com.salon.crm.whatsapp;

import com.salon.crm.entity.QueueTicket;

import java.math.BigDecimal;

public final class MessageTemplates {

    private MessageTemplates() {
    }

    public static String menu(String salonName, String firstName) {
        return "Welcome to " + salonName + ", " + firstName + "! Reply with a number:\n"
                + "1 Join queue\n"
                + "2 Check my queue status\n"
                + "3 Pay my bill\n"
                + "4 Leave a review\n"
                + "5 Leave the queue";
    }

    public static String joinConfirmation(String salonName, String firstName, QueueTicket ticket,
                                          int peopleAhead, int etaMinutes, String publicBaseUrl) {
        return "Hi " + firstName + "! You're in the queue at " + salonName
                + ". Token #" + ticket.getTokenNumber() + ". "
                + peopleAhead + " ahead of you, estimated wait ~" + etaMinutes + " min. "
                + "Track: " + publicBaseUrl + "/q/" + ticket.getId();
    }

    public static String yourTurn(QueueTicket ticket) {
        return "It's your turn, token #" + ticket.getTokenNumber()
                + "! Please come to the counter. Reply 1 = I'm here, "
                + "2 = need 10 more minutes, 3 = cancel";
    }

    public static String serviceStarted() {
        return "We've started your service. Enjoy!";
    }

    public static String bill(QueueTicket ticket, String upiLink) {
        StringBuilder sb = new StringBuilder("Your bill at a glance:\n");
        ticket.getServices().forEach(s ->
                sb.append(s.getName()).append(" — ₹").append(amountText(s.getPrice())).append("\n"));
        sb.append("Total: ₹").append(amountText(ticket.getAmount())).append("\n");
        sb.append("Pay via UPI: ").append(upiLink)
                .append("  or pay at the counter. Receipt will follow.");
        return sb.toString();
    }

    public static String receipt(QueueTicket ticket, String mode) {
        String receiptNo = ticket.getQueueDate().toString().replace("-", "")
                + "-" + ticket.getTokenNumber();
        return "Payment of ₹" + amountText(ticket.getAmount()) + " received (" + mode + "). "
                + "Receipt #" + receiptNo + ". Thank you!";
    }

    public static String movedBack(QueueTicket ticket, int position) {
        return "We couldn't find you, so we've moved you back. New position " + position + ".";
    }

    public static String noShowCancelled() {
        return "We couldn't find you, so your spot in the queue has been cancelled.";
    }

    public static String youreNext(QueueTicket ticket) {
        return "You're next! Token #" + ticket.getTokenNumber()
                + ". Please head to the salon now.";
    }

    public static String reviewPrompt() {
        return "How was your visit? Reply with a rating 1-5.";
    }

    public static String publicReviewThanks(String googleReviewUrl) {
        return "Thanks! Would you share it publicly? " + googleReviewUrl;
    }

    public static String privateReviewThanks(int rating) {
        return rating >= 4
                ? "Thank you so much! We're glad you enjoyed your visit."
                : "Thanks for your feedback, we'll do better.";
    }

    public static String amountText(BigDecimal amount) {
        if (amount == null) return "0";
        return amount.stripTrailingZeros().toPlainString();
    }
}

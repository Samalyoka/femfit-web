package com.femfit.service.impl;

import com.femfit.dto.BookingReminderDto;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Sends class reminder emails via SMTP.
 * When mail.enabled=false (default), logs what it would have sent.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("EEEE, d MMM 'at' HH:mm");

    @Value("${mail.enabled:false}")   private boolean enabled;
    @Value("${mail.smtp.host:}")      private String smtpHost;
    @Value("${mail.smtp.port:587}")   private int smtpPort;
    @Value("${mail.smtp.username:}")  private String smtpUser;
    @Value("${mail.smtp.password:}")  private String smtpPass;
    @Value("${mail.from.address:noreply@femfit.kz}") private String fromAddr;
    @Value("${mail.from.name:FemFit}")               private String fromName;

    public void sendClassReminder(BookingReminderDto b) {
        String subject = "Reminder: " + b.getClassName();
        String body = "Hi " + (b.getMemberFirstName() != null ? b.getMemberFirstName() : "") + ",\n\n"
                + "Your class \"" + b.getClassName() + "\" is coming up "
                + (b.getScheduledAt() != null ? b.getScheduledAt().format(FMT) : "soon")
                + (b.getRoom() != null ? " in " + b.getRoom() : "") + ".\n\nSee you there!\nFemFit";
        if (!enabled) {
            log.info("[mail.enabled=false] Would send to {}: {}", b.getMemberEmail(), subject);
            return;
        }
        try {
            send(b.getMemberEmail(), subject, body);
            log.info("Reminder sent to {} for booking id={}", b.getMemberEmail(), b.getBookingId());
        } catch (MessagingException e) {
            log.error("Failed to send reminder to {}: {}", b.getMemberEmail(), e.getMessage());
        }
    }

    private void send(String to, String subject, String body) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUser, smtpPass);
            }
        });
        MimeMessage msg = new MimeMessage(session);
        try { msg.setFrom(new InternetAddress(fromAddr, fromName)); }
        catch (java.io.UnsupportedEncodingException e) { throw new MessagingException("Bad from-name", e); }
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        msg.setSubject(subject);
        msg.setText(body);
        Transport.send(msg);
    }
}

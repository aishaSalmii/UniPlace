package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.Notification;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/** Records notifications in the Outbox and, when SMTP is configured, also e-mails them. */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    public static final int PAGE_SIZE = 20;

    private final NotificationRepository repository;
    private final SettingsService settings;
    private final Clock clock;

    public NotificationService(NotificationRepository repository, SettingsService settings, Clock clock) {
        this.repository = repository;
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional
    public Notification notify(User recipient, String subject, String body) {
        Notification n = repository.save(new Notification(recipient, subject, body, LocalDateTime.now(clock)));
        sendMail(recipient, subject, body);
        return n;
    }

    private void sendMail(User recipient, String subject, String body) {
        String host = settings.get(SettingsService.SMTP_HOST).trim();
        if (host.isEmpty() || recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            return;
        }
        try {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(host);
            sender.setPort(Integer.parseInt(settings.get(SettingsService.SMTP_PORT).trim()));
            sender.setUsername(settings.get(SettingsService.SMTP_USERNAME));
            sender.setPassword(settings.get(SettingsService.SMTP_PASSWORD));
            sender.getJavaMailProperties().put("mail.smtp.auth", String.valueOf(!sender.getUsername().isBlank()));
            sender.getJavaMailProperties().put("mail.smtp.starttls.enable", "true");
            sender.getJavaMailProperties().put("mail.smtp.connectiontimeout", "5000");
            sender.getJavaMailProperties().put("mail.smtp.timeout", "5000");
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(settings.get(SettingsService.SMTP_FROM));
            msg.setTo(recipient.getEmail());
            msg.setSubject(subject);
            msg.setText(body);
            sender.send(msg);
        } catch (RuntimeException ex) {
            log.warn("E-mail to user {} not sent: {}", recipient.getUsername(), ex.getClass().getSimpleName());
            throw new BusinessRuleException("The e-mail to " + recipient.getEmail() + " could not be sent. "
                    + "Check the e-mail settings and try again.");
        }
    }

    @Transactional(readOnly = true)
    public Page<Notification> forUser(User user, int page) {
        return repository.findByRecipientOrderByCreatedAtDescIdDesc(user, PageRequest.of(Math.max(0, page), PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public Page<Notification> all(int page) {
        return repository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(Math.max(0, page), PAGE_SIZE));
    }
}

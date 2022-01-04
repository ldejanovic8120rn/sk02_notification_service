package com.sk02.sk02_notification_service.mail;

import com.sk02.sk02_notification_service.domain.Notification;
import com.sk02.sk02_notification_service.dto.notification.NotificationTransferDto;
import com.sk02.sk02_notification_service.exception.NotFoundException;
import com.sk02.sk02_notification_service.repository.NotificationRepository;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import java.util.Map;

@Component
public class EmailListener {

    private static final String notFoundNotification = "Notification with given type doesn't exist";

    private final MessageHelper messageHelper;
    private final EmailService emailService;
    private final NotificationRepository notificationRepository;

    public EmailListener(MessageHelper messageHelper, EmailService emailService, NotificationRepository notificationRepository) {
        this.messageHelper = messageHelper;
        this.emailService = emailService;
        this.notificationRepository = notificationRepository;
    }

    @JmsListener(destination = "${destination.notification}", concurrency = "5-10")
    public void sendNotification(Message message) throws JMSException {
        NotificationTransferDto notificationTransferDto = messageHelper.getMessage(message, NotificationTransferDto.class);
        Notification notification = notificationRepository.findNotificationByType(notificationTransferDto.getType()).orElseThrow(() -> new NotFoundException(notFoundNotification));

        String messageToSend = notification.getMessage();
        for (Map.Entry<String,String> entry : notificationTransferDto.getParameters().entrySet()) {
            messageToSend = messageToSend.replace(entry.getKey(), entry.getValue());
        }

        emailService.sendEmail(notificationTransferDto.getEmail(), notification.getSubject(), messageToSend);
    }
}

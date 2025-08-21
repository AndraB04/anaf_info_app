package com.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String usernameFromEmailAddress;

    public void sendEmailWithAttachment(
            String to,
            String cui,
            String companyName,
            MultipartFile attachment
    ) throws MessagingException, IOException {

        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(usernameFromEmailAddress);
        helper.setTo(to);
        helper.setSubject("Company Report for " + companyName + " (CUI: " + cui + ")");

        String emailBody = "Hello,\n\nPlease find the attached PDF report for the company you requested.";
        helper.setText(emailBody);

        helper.addAttachment(
                attachment.getOriginalFilename(),
                new ByteArrayResource(attachment.getBytes())
        );

        mailSender.send(message);
        System.out.println("Email with attachment sent successfully to " + to);
    }
}

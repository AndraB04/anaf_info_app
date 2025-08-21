package com.backend.service;

import com.backend.model.entity.CompanyData;
import com.backend.service.PdfStorageService.StorageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PostmarkEmailService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.postmark.api-token}")
    private String apiToken;

    @Value("${app.postmark.from-email:noreply@your-domain.com}")
    private String fromEmail;

    @Value("${app.postmark.from-name:Company Reports}")
    private String fromName;

    private static final String POSTMARK_API_URL = "https://api.postmarkapp.com/email";

    public PostmarkEmailService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        log.info("Postmark email service initialized successfully");
    }

    public void sendEmailWithVerifiedPdf(String recipientEmail, CompanyData company, StorageResult verifiedPdf, String requestId) {
        try {
            log.info("Preparing Postmark email for {} with PDF: {} [RequestID: {}]",
                    recipientEmail, verifiedPdf.getFileName(), requestId);

            if (apiToken == null || apiToken.trim().isEmpty() || apiToken.equals("YOUR_POSTMARK_API_TOKEN_HERE")) {
                log.warn("Postmark API token not configured properly. Please set app.postmark.api-token property.");
                throw new IllegalStateException("Postmark API token is not configured. Please check your application.properties file.");
            }

            log.info("PDF integrity verified through storage service. Checksum: {} [RequestID: {}]",
                    verifiedPdf.getChecksum(), requestId);

            String htmlBody = generateEmailTemplate(company, verifiedPdf, requestId);
            String base64Pdf = Base64.getEncoder().encodeToString(verifiedPdf.getPdfData());

            Map<String, Object> emailData = new HashMap<>();
            emailData.put("From", String.format("%s <%s>", fromName, fromEmail));
            emailData.put("To", recipientEmail);
            emailData.put("Subject", String.format("Company Report - %s (CUI: %s)", 
                    company.getCompanyName() != null ? company.getCompanyName() : "N/A",
                    company.getCui()));
            emailData.put("HtmlBody", htmlBody);

            Map<String, String> attachment = new HashMap<>();
            attachment.put("Name", verifiedPdf.getFileName());
            attachment.put("Content", base64Pdf);
            attachment.put("ContentType", "application/pdf");
            emailData.put("Attachments", List.of(attachment));

            Map<String, String> headers = new HashMap<>();
            headers.put("X-Request-ID", requestId);
            headers.put("X-PDF-Checksum", verifiedPdf.getChecksum());
            headers.put("X-Company-CUI", company.getCui());
            emailData.put("Headers", List.of(headers.entrySet().stream()
                    .map(entry -> Map.of("Name", entry.getKey(), "Value", entry.getValue()))
                    .toArray()));

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.set("X-Postmark-Server-Token", apiToken);
            httpHeaders.set("Accept", "application/json");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailData, httpHeaders);

            ResponseEntity<Map> response = restTemplate.postForEntity(POSTMARK_API_URL, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String messageId = (String) response.getBody().get("MessageID");
                log.info("Email sent successfully via Postmark to {} with MessageID: {} [RequestID: {}]", 
                        recipientEmail, messageId, requestId);
            } else {
                log.error("Postmark API returned unexpected response: {} [RequestID: {}]", response.getBody(), requestId);
                throw new RuntimeException("Email sending failed - unexpected API response");
            }

        } catch (Exception e) {
            log.error("Error sending email via Postmark to {} [RequestID: {}]", recipientEmail, requestId, e);
            throw new RuntimeException("Email sending failed via Postmark: " + e.getMessage(), e);
        }
    }

    private String generateEmailTemplate(CompanyData company, StorageResult verifiedPdf, String requestId) {
        LocalDateTime now = LocalDateTime.now();

        return String.format("""
            <h1>hi</h1>
            """,
                company.getCompanyName() != null ? company.getCompanyName() : "N/A",
                company.getCompanyName() != null ? company.getCompanyName() : "N/A",
                company.getCui(),
                now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                requestId,
                verifiedPdf.getFileName(),
                verifiedPdf.getFileSize(),
                verifiedPdf.getChecksum(),
                now.getYear()
        );
    }
}

package com.backend.controller;

import com.backend.model.SecurePdfResult;
import com.backend.model.entity.CompanyData;
import com.backend.model.response.ApiResponse;
import com.backend.service.PostmarkEmailService;
import com.backend.service.PdfGenerationService;
import com.backend.service.PdfStorageService;
import com.backend.service.CompanyFinancialService;
import com.backend.service.PdfUtilService;
import com.backend.service.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Slf4j
@Validated
@CrossOrigin(origins = "http://localhost:4200", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}, allowCredentials = "true")
public class EmailController {

    private final PostmarkEmailService emailService;
    private final CompanyFinancialService companyService;
    private final PdfGenerationService pdfGenerationService;
    private final PdfStorageService storageService;
    private final PdfUtilService pdfUtilService;
    private final EmailVerificationService verificationService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @PostMapping("/send-report")
    public ResponseEntity<ApiResponse<String>> sendEmailWithReport(
            @RequestParam @Pattern(regexp = "^[0-9]{2,10}$", message = "CUI must be 2-10 digits") String cui,
            @RequestParam @Email(message = "Invalid email format") String email,
            @RequestParam(defaultValue = "3") @Min(1) @Max(10) int years,
            HttpServletRequest request) {

        String requestId = UUID.randomUUID().toString();

        try {
            log.info("Email report request received: CUI={}, email={}, years={}, [RequestID: {}]", cui, email, years, requestId);

            var companyOpt = companyService.getCompanyFromDatabase(cui);
            if (companyOpt.isEmpty()) {
                log.warn("Company not found for CUI: {} [RequestID: {}]", cui, requestId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("Company not found for CUI: " + cui, requestId));
            }
            CompanyData company = companyOpt.get();

            SecurePdfResult pdfResult = pdfGenerationService.generateSecurePdf(company, cui, years);
            log.info("PDF generated successfully with checksum: {} [RequestID: {}]", pdfResult.getChecksum(), requestId);

            String calculatedChecksum = pdfUtilService.calculateChecksum(pdfResult.getPdfData());
            if (!calculatedChecksum.equals(pdfResult.getChecksum())) {
                log.error("CRITICAL: PDF checksum mismatch during generation! Expected: {}, Calculated: {} [RequestID: {}]",
                        pdfResult.getChecksum(), calculatedChecksum, requestId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponse<>("PDF integrity verification failed during generation", requestId));
            }

            String fileName = storageService.storePdf(
                    pdfResult.getPdfData(),
                    cui,
                    pdfResult.getTimestamp(),
                    pdfResult.getChecksum(),
                    pdfResult.getVersion()
            );

            PdfStorageService.StorageResult storageResult = storageService.retrieveAndVerifyPdf(fileName);
            log.info("PDF integrity verified after storage: {} (size: {} bytes) [RequestID: {}]", fileName, storageResult.getFileSize(), requestId);

            emailService.sendEmailWithVerifiedPdf(email, company, storageResult, requestId);

            log.info("Email sent successfully to {} for CUI {} with verified PDF {} [RequestID: {}]", email, cui, fileName, requestId);
            return ResponseEntity.ok(new ApiResponse<>("Email sent successfully with verified PDF", requestId));

        } catch (SecurityException e) {
            log.error("SECURITY VIOLATION: {} [RequestID: {}]", e.getMessage(), requestId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("PDF integrity verification failed - security violation detected", requestId));
        } catch (Exception e) {
            log.error("Error sending email report for CUI: {} [RequestID: {}]", cui, requestId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Failed to send email: " + e.getMessage(), requestId));
        }
    }


    @PostMapping("/request-verification")
    public ResponseEntity<Map<String, String>> requestEmailVerification(
            @RequestParam @Pattern(regexp = "^[0-9]{2,10}$", message = "CUI must be 2-10 digits") String cui,
            @RequestParam(defaultValue = "3") @Min(1) @Max(10) int years) {

        String requestId = UUID.randomUUID().toString();

        try {
            log.info("Email verification request received: CUI={}, years={}, [RequestID: {}]", cui, years, requestId);
            var companyOpt = companyService.getCompanyFromDatabase(cui);
            if (companyOpt.isEmpty()) {
                log.warn("Company not found for CUI: {} [RequestID: {}]", cui, requestId);
                Map<String, String> response = new HashMap<>();
                response.put("error", "Company not found for CUI: " + cui);
                response.put("requestId", requestId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            String sessionId = verificationService.initiateEmailVerification(cui, years);

            String authUrl = "http://localhost:8080/oauth2/start?sessionId=" + sessionId;

            Map<String, String> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("authUrl", authUrl);
            response.put("message", "Please complete email verification through Google OAuth");
            response.put("requestId", requestId);

            log.info("Verification session created: sessionId={} [RequestID: {}]", sessionId, requestId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error creating verification session [RequestID: {}]", requestId, e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to initiate verification: " + e.getMessage());
            response.put("requestId", requestId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/send-verified")
    public ResponseEntity<Map<String, String>> sendVerifiedEmail(@RequestParam String sessionId) {
        String requestId = UUID.randomUUID().toString();

        try {
            log.info("Verified email send request: sessionId={} [RequestID: {}]", sessionId, requestId);

            if (!verificationService.isSessionVerified(sessionId)) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Session not verified or expired");
                response.put("requestId", requestId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            EmailVerificationService.VerificationSession session = verificationService.getSession(sessionId);
            if (session == null) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Session not found");
                response.put("requestId", requestId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            var companyOpt = companyService.getCompanyFromDatabase(session.getCui());
            if (companyOpt.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Company not found");
                response.put("requestId", requestId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            CompanyData company = companyOpt.get();

            SecurePdfResult pdfResult = pdfGenerationService.generateSecurePdf(company, session.getCui(), session.getYears());
            log.info("PDF generated successfully with checksum: {} [RequestID: {}]", pdfResult.getChecksum(), requestId);

            String calculatedChecksum = pdfUtilService.calculateChecksum(pdfResult.getPdfData());
            if (!calculatedChecksum.equals(pdfResult.getChecksum())) {
                log.error("CRITICAL: PDF checksum mismatch during generation! Expected: {}, Calculated: {} [RequestID: {}]",
                        pdfResult.getChecksum(), calculatedChecksum, requestId);
                Map<String, String> response = new HashMap<>();
                response.put("error", "PDF integrity verification failed during generation");
                response.put("requestId", requestId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            String fileName = storageService.storePdf(
                    pdfResult.getPdfData(),
                    session.getCui(),
                    pdfResult.getTimestamp(),
                    pdfResult.getChecksum(),
                    pdfResult.getVersion()
            );

            PdfStorageService.StorageResult storageResult = storageService.retrieveAndVerifyPdf(fileName);
            log.info("PDF integrity verified after storage: {} (size: {} bytes) [RequestID: {}]", fileName, storageResult.getFileSize(), requestId);

            emailService.sendEmailWithVerifiedPdf(session.getVerifiedEmail(), company, storageResult, requestId);

            verificationService.cleanupSession(sessionId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Email sent successfully with verified PDF");
            response.put("requestId", requestId);
            response.put("fileName", fileName);

            log.info("Email sent successfully to {} for CUI {} with verified PDF {} [RequestID: {}]",
                    session.getVerifiedEmail(), session.getCui(), fileName, requestId);
            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            log.error("SECURITY VIOLATION: {} [RequestID: {}]", e.getMessage(), requestId, e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "PDF integrity verification failed - security violation detected");
            response.put("requestId", requestId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            log.error("Error sending verified email [RequestID: {}]", requestId, e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to send email: " + e.getMessage());
            response.put("requestId", requestId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
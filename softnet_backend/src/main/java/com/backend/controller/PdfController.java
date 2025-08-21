package com.backend.controller;

import com.backend.model.SecurePdfResult;
import com.backend.model.entity.CompanyData;
import com.backend.model.response.ApiResponse;
import com.backend.service.CompanyFinancialService;
import com.backend.service.PdfGenerationService;
import com.backend.service.PdfStorageService;
import com.backend.service.PdfUtilService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
@Slf4j
@Validated
@CrossOrigin(origins = "http://localhost:4200", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}, allowCredentials = "true")
public class PdfController {

    private final CompanyFinancialService companyService;
    private final PdfGenerationService pdfService;
    private final PdfStorageService storageService;
    private final PdfUtilService pdfUtilService;

    @GetMapping("/company/{cui}")
    public ResponseEntity<?> generatePdfReport(
            @PathVariable @Pattern(regexp = "^[0-9]{2,10}$", message = "CUI must be 2-10 digits") String cui,
            @RequestParam(defaultValue = "3") int years,
            HttpServletRequest request) {

        try {
            log.info("PDF generation requested for CUI: {} with {} years", cui, years);

            var companyOpt = companyService.getCompanyFromDatabase(cui);
            if (companyOpt.isEmpty()) {
                log.warn("Company not found for CUI: {}", cui);
                return ResponseEntity.notFound().build();
            }

            CompanyData company = companyOpt.get();
            SecurePdfResult result = pdfService.generateSecurePdf(company, cui, years);
            
            String verificationChecksum = pdfUtilService.calculateChecksum(result.getPdfData());
            if (!verificationChecksum.equals(result.getChecksum())) {
                log.error("CRITICAL: PDF checksum verification failed! Expected: {}, Actual: {}", 
                         result.getChecksum(), verificationChecksum);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponse<>("PDF integrity verification failed", null));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", result.getFileName());
            headers.setContentLength(result.getPdfData().length);
            headers.add("X-PDF-Checksum", result.getChecksum().substring(0, 16));

            log.info("PDF successfully generated for CUI: {} (size: {} bytes)", cui, result.getPdfData().length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(result.getPdfData());

        } catch (Exception e) {
            log.error("Error generating PDF for CUI: {}", cui, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Error generating PDF: " + e.getMessage(), null));
        }
    }

    @GetMapping("/company/{cui}/data")
    public ResponseEntity<ApiResponse<CompanyData>> getCompanyData(
            @PathVariable @Pattern(regexp = "^[0-9]{2,10}$", message = "CUI must be 2-10 digits") String cui,
            HttpServletRequest request) {

        try {
            log.info("Company data requested for CUI: {}", cui);

            var companyOpt = companyService.getCompanyFromDatabase(cui);
            if (companyOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            CompanyData company = companyOpt.get();

            return ResponseEntity.ok(new ApiResponse<>("Success", company));

        } catch (Exception e) {
            log.error("Error getting company data for CUI: {}", cui, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Error: " + e.getMessage(), null));
        }
    }

    @GetMapping("/company/{cui}/stored")
    public ResponseEntity<ApiResponse<?>> getStoredPdfs(
            @PathVariable @Pattern(regexp = "^[0-9]{2,10}$", message = "CUI must be 2-10 digits") String cui) {

        try {
            log.info("Retrieving stored PDFs for CUI: {}", cui);
            
            var storedFiles = storageService.getStoredPdfsForCompany(cui);
            
            return ResponseEntity.ok(new ApiResponse<>("Success", storedFiles));

        } catch (Exception e) {
            log.error("Error retrieving stored PDFs for CUI: {}", cui, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Error: " + e.getMessage(), null));
        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<?> downloadStoredPdf(@PathVariable String fileName) {

        try {
            log.info("Download request for stored PDF: {}", fileName);
            
            PdfStorageService.StorageResult storageResult = storageService.retrieveAndVerifyPdf(fileName);
            
            if (storageResult == null || storageResult.getPdfData() == null) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(storageResult.getPdfData().length);
            headers.add("X-PDF-Checksum", storageResult.getChecksum().substring(0, 16));

            log.info("PDF download successful: {} (verified checksum: {})", fileName, storageResult.getChecksum().substring(0, 8));

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(storageResult.getPdfData());

        } catch (Exception e) {
            log.error("Error downloading stored PDF: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Error downloading PDF: " + e.getMessage(), null));
        }
    }

    @PostMapping("/sign")
    public ResponseEntity<?> signPdf(@RequestBody byte[] pdfBytes) {
        try {
            String signature = storageService.generateSignature(pdfBytes);
            return ResponseEntity.ok(new ApiResponse<>("PDF signed successfully", signature));
        } catch (Exception e) {
            log.error("Error signing PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Error signing PDF: " + e.getMessage(), null));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPdf(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("No file uploaded"));
            }

            byte[] pdfBytes = file.getBytes();

            com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(
                    new com.itextpdf.kernel.pdf.PdfReader(new ByteArrayInputStream(pdfBytes))
            );
            StringBuilder textBuilder = new StringBuilder();
            int pages = pdfDoc.getNumberOfPages();
            for (int i = 1; i <= pages; i++) {
                String pageText = com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i));
                if (pageText != null) {
                    textBuilder.append(pageText).append('\n');
                }
            }
            com.itextpdf.kernel.pdf.PdfDocumentInfo info = pdfDoc.getDocumentInfo();
            String embeddedSignature = info.getMoreInfo("report_Signature");
            String embeddedAlg = info.getMoreInfo("Report-Signature-Alg");
            String embeddedTextHash = info.getMoreInfo("Report-Content-Text-Hash");
            pdfDoc.close();

            if (embeddedSignature == null || embeddedTextHash == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("PDF not recognized. Missing verification metadata."));
            }
            if (embeddedAlg != null && !"HMAC-SHA256".equalsIgnoreCase(embeddedAlg)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Unsupported signature algorithm"));
            }

            byte[] textBytes = textBuilder.toString().getBytes(StandardCharsets.UTF_8);
            String recomputedTextHash = pdfUtilService.calculateChecksum(textBytes);
            String recomputedSignature;
            try {
                recomputedSignature = storageService.generateSignature(textBytes);
            } catch (Exception e) {
                log.error("Failed to compute signature for verification", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Verification service error"));
            }

            boolean hashMatches = recomputedTextHash.equalsIgnoreCase(embeddedTextHash);
            boolean signatureMatches = recomputedSignature.equals(embeddedSignature);

            if (hashMatches && signatureMatches) {
                return ResponseEntity.ok(ApiResponse.success("PDF is valid (embedded signature verified)", recomputedTextHash));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("PDF integrity check failed: signature or content hash mismatch"));

        } catch (Exception e) {
            log.error("Error verifying PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Error verifying PDF: " + e.getMessage(), null));
        }
    }
}

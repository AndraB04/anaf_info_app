package com.backend.service;

import com.backend.model.SecurePdfResult;
import com.backend.model.entity.CompanyData;
import com.backend.model.entity.FinancialRecord;
import com.itextpdf.html2pdf.HtmlConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGenerationService {

    private final CompanyFinancialService companyService;
    private final PdfUtilService pdfUtilService;
    private final PdfStorageService storageService;

    @Value("${app.pdf.version:1.0}")
    private String pdfVersion;

    @Value("${app.pdf.watermark.text:}")
    private String watermarkText;

    public SecurePdfResult generateSecurePdf(CompanyData company, String cui, int years) throws IOException {
        String requestId = UUID.randomUUID().toString();
        LocalDateTime timestamp = LocalDateTime.now();

        try {
            log.info("Starting secure PDF generation for CUI: {} with {} years [RequestID: {}]", cui, years, requestId);

            CompanyData validatedCompany = validateAndNormalizeCompanyData(company, cui);
            java.time.LocalDate now = java.time.LocalDate.now();
            int currentYear = now.getYear();
            int currentMonth = now.getMonthValue();

            int latestAvailableYear;
            if (currentMonth > 7) {
                latestAvailableYear = currentYear - 1;
            } else {
                latestAvailableYear = currentYear - 2;
            }

            int startYear = latestAvailableYear - years + 1;
            int endYear = latestAvailableYear;

            log.info("PDF generation for CUI: {} - Years: {} to {} (requested {} years)", cui, startYear, endYear, years);
            List<FinancialRecord> records = companyService.getFinancialRecordsForPeriod(cui, startYear, endYear);

            String htmlContent = generateDeterministicHtml(validatedCompany, records, timestamp, requestId, cui);

            byte[] pdfBytes = createSecurePdfFromHtmlWithSignature(htmlContent);
            String checksum = calculateSHA256(pdfBytes);

            String fileName = generateFileName(cui, timestamp, pdfVersion, checksum);

            log.info("PDF generated successfully for CUI: {} [RequestID: {}, Size: {} bytes, Checksum: {}]",
                    cui, requestId, pdfBytes.length, checksum);

            return new SecurePdfResult(pdfBytes, fileName, checksum, pdfBytes.length, timestamp, pdfVersion);

        } catch (Exception e) {
            log.error("Error generating PDF for CUI: {} [RequestID: {}]", cui, requestId, e);
            throw e;
        }
    }

    private CompanyData validateAndNormalizeCompanyData(CompanyData company, String cui) {
        if (!company.getCui().equals(cui)) {
            throw new IllegalArgumentException("CUI mismatch in company data");
        }
        if (company.getPhone() != null) {
            company.setPhone(normalizePhoneNumber(company.getPhone()));
        }
        if (company.getFiscalAddress() != null) {
            company.setFiscalAddress(company.getFiscalAddress().trim().toUpperCase());
        }
        validateBusinessRules(company);
        return company;
    }

    private String generateDeterministicHtml(CompanyData company, List<FinancialRecord> records,
                                             LocalDateTime timestamp, String requestId, String cui) {
        StringBuilder html = new StringBuilder();

        html.append(String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>Company Report - %s</title>
                <meta name="version" content="%s">
                <meta name="timestamp" content="%s">
                <meta name="request-id" content="%s">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji";
                        margin: 0 auto;
                        max-width: 800px;
                        padding: 20px;
                        color: #333;
                        font-size: 15px;
                        line-height: 1.6;
                    }
                    .header { text-align: center; border-bottom: 2px solid #e9ecef; padding-bottom: 20px; margin-bottom: 30px; position: relative; }
                    .header h1 { font-size: 28px; color: #2c3e50; margin-bottom: 5px; }
                    .header h2 { font-size: 20px; font-weight: normal; color: #7f8c8d; margin-top: 0; }
                    .watermark { position: absolute; top: 5px; right: 5px; color: #ced4da; font-size: 12px; font-weight: bold; }
                    .section { margin-bottom: 30px; }
                    .section-title { font-size: 20px; font-weight: bold; color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px; margin-bottom: 20px; }
                    .info-table { width: 100%%; border-collapse: collapse; }
                    .info-table td { padding: 8px 0; vertical-align: top; }
                    .info-table .label { font-weight: bold; width: 200px; color: #555; }
                    .financial-table { width: 100%%; border-collapse: collapse; margin-top: 15px; }
                    .financial-table th, .financial-table td { border-bottom: 1px solid #dee2e6; padding: 12px; text-align: left; }
                    .financial-table th { background-color: #f8f9fa; font-weight: bold; color: #34495e; border-bottom-width: 2px; }
                    .financial-table tr:last-child td { border-bottom: none; }
                    .text-right { text-align: right; }
                </style>
            </head>
            <body>
            """, company.getCui(), pdfVersion, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), requestId));

        html.append(String.format("""
            <div class="header">
                <div class="watermark">%s</div>
                <h1>Company Report</h1>
                <h2>%s</h2>
                <p>CUI: %s | Generated: %s</p>
            </div>
            """, watermarkText,
                company.getCompanyName() != null ? company.getCompanyName() : "N/A",
                company.getCui(),
                timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

        html.append("""
            <div class="section">
                <h3 class="section-title">General Information</h3>
                <table class="info-table">
            """);

        addInfoRow(html, "Company Name:", company.getCompanyName());
        addInfoRow(html, "Unique Tax ID (CUI):", company.getCui());
        addInfoRow(html, "Registered Address:", company.getFiscalAddress());
        addInfoRow(html, "Trade Register No.:", company.getTradeRegisterNo());
        addInfoRow(html, "Phone:", company.getPhone());
        addInfoRow(html, "Fax:", company.getFax());
        addInfoRow(html, "Postal Code:", company.getPostalCode());
        addInfoRow(html, "Inactive Status:", (company.getIsInactive() != null && company.getIsInactive()) ? "YES" : "NO");
        addInfoRow(html, "VAT Payer:", (company.getIsVatPayer() != null && company.getIsVatPayer()) ? "YES" : "NO");

        if (company.getCaenDescription() != null) {
            addInfoRow(html, "Primary Activity (CAEN):", company.getCaenDescription());
        }

        html.append("</table></div>");

        if (records != null && !records.isEmpty()) {
            html.append(String.format("""
                <div class="section">
                    <h3 class="section-title">Financial Data (Last %d Years)</h3>
                    <table class="financial-table">
                        <thead>
                            <tr>
                                <th>Year</th>
                                <th class="text-right">Net Turnover (RON)</th>
                                <th class="text-right">Net Profit (RON)</th>
                                <th class="text-right">Total Expenses (RON)</th>
                                <th class="text-right">Total Liabilities (RON)</th>
                                <th class="text-right">Total Capital (RON)</th>
                                <th class="text-right">Fixed Assets (RON)</th>
                                <th class="text-right">Avg. Employees</th>
                            </tr>
                        </thead>
                        <tbody>
                """, records.size()));

            records.stream()
                    .sorted((r1, r2) -> Integer.compare(r2.getYear(), r1.getYear()))
                    .forEach(record -> {
                        html.append(String.format("""
                            <tr>
                                <td><strong>%d</strong></td>
                                <td class="text-right">%s</td>
                                <td class="text-right">%s</td>
                                <td class="text-right">%s</td>
                                <td class="text-right">%s</td>
                                <td class="text-right">%s</td>
                                <td class="text-right">%s</td>
                                <td class="text-right">%s</td>
                            </tr>
                            """,
                                record.getYear(),
                                formatCurrency(record.getNetTurnover()),
                                formatCurrency(record.getNetProfit()),
                                formatCurrency(record.getTotalExpenses()),
                                formatCurrency(record.getLiabilities()),
                                formatCurrency(record.getTotalCapital()),
                                formatCurrency(record.getFixedAssets()),
                                record.getAverageEmployees() != null ? record.getAverageEmployees().toString() : "N/A"
                        ));
                    });

            html.append("""
                        </tbody>
                    </table>
                </div>
                """);
        }


        return html.toString();
    }

    private byte[] createSecurePdfFromHtmlWithSignature(String htmlContent) throws IOException {
        ByteArrayOutputStream initialOut = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(htmlContent, initialOut);
        byte[] initialPdf = initialOut.toByteArray();

        StringBuilder textBuilder = new StringBuilder();
        com.itextpdf.kernel.pdf.PdfDocument readDoc = null;
        try {
            readDoc = new com.itextpdf.kernel.pdf.PdfDocument(
                    new com.itextpdf.kernel.pdf.PdfReader(new java.io.ByteArrayInputStream(initialPdf))
            );
            int pages = readDoc.getNumberOfPages();
            for (int i = 1; i <= pages; i++) {
                String pageText = com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(readDoc.getPage(i));
                if (pageText != null) {
                    textBuilder.append(pageText).append('\n');
                }
            }
        } finally {
            if (readDoc != null) {
                readDoc.close();
            }
        }
        String extractedText = textBuilder.toString();

        System.out.println(extractedText);
        String textHash = pdfUtilService.calculateChecksum(extractedText.getBytes(StandardCharsets.UTF_8));
        String signature;
        try {
            signature = storageService.generateSignature(extractedText.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IOException("Failed to sign PDF content", e);
        }
        ByteArrayOutputStream stampedOut = new ByteArrayOutputStream();
        com.itextpdf.kernel.pdf.PdfDocument stampDoc = null;
        try {
            stampDoc = new com.itextpdf.kernel.pdf.PdfDocument(
                    new com.itextpdf.kernel.pdf.PdfReader(new java.io.ByteArrayInputStream(initialPdf)),
                    new com.itextpdf.kernel.pdf.PdfWriter(stampedOut),
                    new com.itextpdf.kernel.pdf.StampingProperties().useAppendMode()
            );
            com.itextpdf.kernel.pdf.PdfDocumentInfo info = stampDoc.getDocumentInfo();
            info.setMoreInfo("report_Signature", signature);
            info.setMoreInfo("Report-Signature-Alg", "HMAC-SHA256");
            info.setMoreInfo("Report-Content-Text-Hash", textHash);
            info.setCreator("Report API");
            info.setTitle("Company Report");
        } finally {
            if (stampDoc != null) {
                stampDoc.close();
            }
        }

        return stampedOut.toByteArray();
    }

    private void addInfoRow(StringBuilder html, String label, String value) {
        if (value != null && !value.isBlank()) {
            html.append("<tr><td class=\"label\">")
                    .append(label)
                    .append("</td><td>")
                    .append(value)
                    .append("</td></tr>");
        }
    }

    private String formatCurrency(Long amount) {
        if (amount == null) return "N/A";
        return String.format("%,d", amount);
    }

    private String normalizePhoneNumber(String phone) {
        return phone.replaceAll("\\s+", "").replaceAll("-", "");
    }

    private void validateBusinessRules(CompanyData company) {
        if (company.getCui() == null || company.getCui().trim().isEmpty()) {
            throw new IllegalArgumentException("CUI is required");
        }
        if (!company.getCui().matches("^[0-9]{2,10}$")) {
            throw new IllegalArgumentException("Invalid CUI format");
        }
    }

    private String calculateSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public String calculateChecksum(byte[] data) {
        return pdfUtilService.calculateChecksum(data);
    }

    public String generateFileName(String cui, LocalDateTime timestamp, String version, String checksum) {
        return pdfUtilService.generateFileName(cui, timestamp, version, checksum);
    }
}
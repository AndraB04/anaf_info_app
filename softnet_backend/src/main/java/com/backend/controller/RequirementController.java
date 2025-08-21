package com.backend.controller;

import com.backend.model.entity.CompanyData;
import com.backend.model.entity.FinancialRecord;
import com.backend.service.CompanyFinancialService;
import com.backend.service.PdfGenerationService;
import com.backend.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}, allowCredentials = "true")
@Tag(name = "Romanian Company Financial Data", description = "API for managing Romanian company data and financial records from ANAF")
public class RequirementController {

    private final CompanyFinancialService companyFinancialService;
    private final PdfGenerationService pdfGenerationService;
    private final EmailService emailService;


    @Operation(
        summary = "Get company data by CUI",
        description = "Retrieves company information from the database using the Romanian CUI. " +
                     "This endpoint only returns data from the local database and does not make requests to ANAF."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Company data retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CompanyData.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Company not found in database",
            content = @Content
        )
    })
    @GetMapping("/firma/{cui}")
    public ResponseEntity<CompanyData> getFirma(
            @Parameter(description = "Romanian CUI (Unique Registration Code)", example = "12345678")
            @PathVariable
            @Pattern(regexp = "^[0-9]{2,10}$", message = "CUI must be 2-10 digits")
            String cui) {
        log.info("Getting company data for CUI: {}", cui);
        Optional<CompanyData> companyData = companyFinancialService.getCompanyFromDatabase(cui);
        return companyData.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Get all financial records for a company",
        description = "Retrieves all available financial records (balance sheets) for a company from the database. " +
                     "Records are ordered by year in descending order (most recent first)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Financial records retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = FinancialRecord.class))
        )
    })
    @GetMapping("/bilant/{cui}")
    public ResponseEntity<List<FinancialRecord>> getBilant(
            @Parameter(description = "Romanian CUI (Unique Registration Code)", example = "12345678")
            @PathVariable
            @Pattern(regexp = "^[0-9]{2,10}$", message = "CUI must be 2-10 digits")
            String cui) {
        log.info("Getting financial records for CUI: {}", cui);
        List<FinancialRecord> records = companyFinancialService.getFinancialRecordsFromDatabase(cui);
        return ResponseEntity.ok(records);
    }


    @Operation(
        summary = "Get financial records for a specific period",
        description = "Retrieves financial records for a company within a calculated period based on the number of years requested. " +
                     "The period is automatically calculated considering Romanian financial reporting deadlines. " +
                     "Financial data is typically available with a 1-2 year delay."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Financial records for the period retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = FinancialRecord.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid years parameter (must be between 1 and 5)",
            content = @Content
        )
    })
    @GetMapping("/bilant/{cui}/period")
    public ResponseEntity<List<FinancialRecord>> getBilantForPeriod(
            @Parameter(description = "Romanian CUI (Unique Registration Code)", example = "12345678")
            @PathVariable
            @Pattern(regexp = "^[0-9]{2,10}$", message = "CUI must be 2-10 digits")
            String cui,
            @Parameter(description = "Number of years to retrieve (1-5)", example = "3")
            @RequestParam(defaultValue = "3") Integer years) {
        
        log.info("Getting financial records for CUI: {} for last {} years", cui, years);
        
        if (years < 1 || years > 5) {
            log.warn("Invalid years parameter: {}. Must be between 1 and 5", years);
            return ResponseEntity.badRequest().build();
        }
        
        java.time.LocalDate now = java.time.LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();
        
        int latestAvailableYear;
        if (currentMonth > 7) {
            latestAvailableYear = currentYear - 1;
        } else if (currentMonth <= 6) {
            latestAvailableYear = currentYear - 2;
        } else {
            latestAvailableYear = currentYear - 2;
        }
        
        int startYear = latestAvailableYear - years + 1;
        int endYear = latestAvailableYear;
        
        log.info("Calculated period: {} to {} (current date: {}-{}, latest available: {})", 
                startYear, endYear, currentYear, currentMonth, latestAvailableYear);
        
        List<FinancialRecord> records = companyFinancialService.getFinancialRecordsForPeriod(cui, startYear, endYear);
        return ResponseEntity.ok(records);
    }

    @Operation(
        summary = "Process and store complete company data",
        description = "Fetches company information and financial records from ANAF and stores them in the database. " +
                     "This endpoint always makes requests to ANAF APIs and updates/creates records in the local database. " +
                     "It retrieves company data and financial records for the specified number of years."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Company data processed and stored successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CompanyData.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid years parameter (must be between 1 and 5)",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Company not found in ANAF database",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error while processing ANAF data",
            content = @Content
        )
    })
    @PostMapping("/firma/{cui}/process")
    public ResponseEntity<CompanyData> processFirma(
            @Parameter(description = "Romanian CUI (Unique Registration Code)", example = "12345678")
            @PathVariable
            @Pattern(regexp = "^[0-9]{2,10}$", message = "CUI must be 2-10 digits")
            String cui,
            @Parameter(description = "Number of years of financial data to retrieve (1-5)", example = "3")
            @RequestParam(defaultValue = "3") Integer years) {
        
        log.info("Processing company for CUI: {} with {} years", cui, years);
        
        try {
            if (years < 1 || years > 5) {
                log.warn("Invalid years parameter: {}. Must be between 1 and 5", years);
                return ResponseEntity.badRequest().build();
            }
            
            CompanyData companyData = companyFinancialService.processCompanyCompletely(cui, years);
            if (companyData == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(companyData);
        } catch (IOException | InterruptedException e) {
            log.error("Error processing company with CUI: {}", cui, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/send-company-pdf")
    public ResponseEntity<Map<String, String>> sendCompanyPdf(
            @Parameter(description = "Romanian CUI of the company", example = "12345678") 
            @RequestParam String cui,
            
            @Parameter(description = "Email address to verify and send PDF to", example = "user@gmail.com") 
            @RequestParam String email,
            
            @Parameter(description = "Number of years of financial data to include", example = "3") 
            @RequestParam(defaultValue = "3") Integer years) {
        
        log.info("Request to send PDF for CUI: {} to email: {} with {} years", cui, email, years);
        
        try {
            if (years < 1 || years > 5) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "number of years wrong",
                    "years", years.toString()
                ));
            }
            var companyData = companyFinancialService.getCompanyFromDatabase(cui);
            if (companyData == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "no found in the data base",
                    "cui", cui,
                    "hint", "use the others endpoints before"
                ));
            }

            
            return ResponseEntity.ok(Map.of(
                "message", "check email address",
                "verificationUrl", "temp",
                "requestedEmail", email,
                "cui", cui,
                "companyName", companyData.isPresent() ? companyData.get().getCompanyName() : "N/A",
                "instructions", "Click on the verification link to verify your email: " + email
            ));
            
        } catch (Exception e) {
            log.error("Error processing request for CUI: {} and email: {}", cui, email, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "error: " + e.getMessage()
            ));
        }
    }
}

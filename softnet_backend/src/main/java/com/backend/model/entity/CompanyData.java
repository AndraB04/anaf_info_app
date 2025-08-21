package com.backend.model.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "FIRMA")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Romanian company data retrieved from ANAF")
public class CompanyData {

    @Id
    @Column(name = "GGUID", nullable = false, updatable = false, columnDefinition = "UUID")
    private UUID gguid;

    @CreationTimestamp
    @Column(name = "InsertTimestamp", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime insertTimestamp;

    @Column(name = "cui", unique = true, nullable = false, columnDefinition = "VARCHAR(10)")
    @Schema(description = "Romanian CUI (Unique Registration Code)", example = "12345678", required = true)
    private String cui;

    @Column(name = "company_name", columnDefinition = "VARCHAR(255)")
    @Schema(description = "Official company name", example = "SC EXAMPLE SRL")
    private String companyName;

    @Column(name = "fiscal_address", columnDefinition = "TEXT")
    @Schema(description = "Fiscal address of the company", example = "Str. Exemplu Nr. 1, Bucuresti")
    private String fiscalAddress;

    @Column(name = "trade_register_no", columnDefinition = "VARCHAR(50)")
    @Schema(description = "Trade register number", example = "J40/1234/2020")
    private String tradeRegisterNo;

    @Column(name = "phone", columnDefinition = "VARCHAR(25)")
    @Schema(description = "Phone number", example = "+40123456789")
    private String phone;

    @Column(name = "fax", columnDefinition = "VARCHAR(25)")
    @Schema(description = "Fax number", example = "+40123456790")
    private String fax;

    @Column(name = "postal_code", columnDefinition = "VARCHAR(15)")
    @Schema(description = "Postal code", example = "010101")
    private String postalCode;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(name = "caen_code", columnDefinition = "INTEGER")
    @Schema(description = "CAEN activity code", example = "6201")
    private Integer caenCode;

    @Column(name = "caen_description", columnDefinition = "VARCHAR(255)")
    @Schema(description = "CAEN activity description", example = "Computer programming activities")
    private String caenDescription;

    @Column(name = "is_vat_payer", columnDefinition = "BOOLEAN")
    @Schema(description = "Whether the company is a VAT payer", example = "true")
    private Boolean isVatPayer;

    @Column(name = "is_inactive", columnDefinition = "BOOLEAN")
    @Schema(description = "Whether the company is inactive", example = "false")
    private Boolean isInactive;

    @OneToMany(mappedBy = "companyData", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Schema(description = "List of financial records for this company")
    private List<FinancialRecord> financialRecords;

    @PrePersist
    public void generateGguid() {
        if (this.gguid == null) {
            this.gguid = UUID.randomUUID();
        }
    }
}
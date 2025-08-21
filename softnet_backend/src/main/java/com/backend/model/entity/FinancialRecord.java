package com.backend.model.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "BILANT", uniqueConstraints = {
        @UniqueConstraint(name = "bilant_cui_an", columnNames = {"cui", "year"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Schema(description = "Financial record (balance sheet data) for a Romanian company from ANAF")
public class FinancialRecord {

    @Id
    @Column(name = "GGUID", nullable = false, updatable = false,  columnDefinition = "UUID")
    private UUID gguid;

    @CreationTimestamp
    @Column(name = "InsertTimestamp", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime insertTimestamp;

    @Column(name = "cui", columnDefinition = "VARCHAR(10)")
    @Schema(description = "Romanian CUI (Unique Registration Code)", example = "12345678")
    private String cui;

    @Column(name = "year", columnDefinition = "INTEGER")
    @Schema(description = "Financial year", example = "2023")
    private Integer year;

    @Column(name = "net_turnover", columnDefinition = "BIGINT")
    @Schema(description = "Net turnover (ANAF indicator I13) in RON", example = "1000000")
    private Long netTurnover; // I13

    @Column(name = "net_profit", columnDefinition = "BIGINT")
    @Schema(description = "Net profit (ANAF indicator I18) in RON", example = "50000")
    private Long netProfit; // I18

    @Column(name = "total_expenses", columnDefinition = "BIGINT")
    @Schema(description = "Total expenses (ANAF indicator I15) in RON", example = "950000")
    private Long totalExpenses; // I15

    @Column(name = "liabilities", columnDefinition = "BIGINT")
    @Schema(description = "Total liabilities (ANAF indicator I7) in RON", example = "200000")
    private Long liabilities; // I7

    @Column(name = "total_capital", columnDefinition = "BIGINT")
    @Schema(description = "Total capital (ANAF indicator I10) in RON", example = "500000")
    private Long totalCapital; // I10

    @Column(name = "fixed_assets", columnDefinition = "BIGINT")
    @Schema(description = "Fixed assets (ANAF indicator I1) in RON", example = "300000")
    private Long fixedAssets; // I1

    @Column(name = "average_employees", columnDefinition = "INTEGER")
    @Schema(description = "Average number of employees (ANAF indicator I20)", example = "10")
    private Integer averageEmployees; // I20

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cui", referencedColumnName = "cui", insertable = false, updatable = false)
    @JsonBackReference
    @Schema(description = "Reference to the company data")
    private CompanyData companyData;

    @PrePersist
    public void generateGguid() {
        if (this.gguid == null) {
            this.gguid = UUID.randomUUID();
        }
    }
}
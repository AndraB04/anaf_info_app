package com.backend.service;

import com.backend.model.entity.CompanyData;
import com.backend.model.entity.FinancialRecord;
import com.backend.model.response.AnafResponse;
import com.backend.model.response.BilantResponse;
import com.backend.model.response.FoundCompany;
import com.backend.model.response.GeneralData;
import com.backend.model.response.Indicator;
import com.backend.repository.CompanyDataRepository;
import com.backend.repository.FinancialRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseService {

    private final CompanyDataRepository companyDataRepository;
    private final FinancialRecordRepository financialRecordRepository;

    @Transactional
    public CompanyData saveCompanyData(AnafResponse anafResponse) {
        if (anafResponse == null || anafResponse.getFound() == null || anafResponse.getFound().isEmpty()) {
            return null;
        }

        FoundCompany foundCompany = anafResponse.getFound().get(0);
        GeneralData generalData = foundCompany.getDateGenerale();
        if (generalData == null) {
            return null;
        }
        
        String cui = String.valueOf(generalData.getCui());

        Optional<CompanyData> existingCompany = companyDataRepository.findByCui(cui);

        CompanyData companyData;
        if (existingCompany.isPresent()) {
            companyData = existingCompany.get();
            updateCompanyDataFromFoundCompany(companyData, foundCompany);
        } else {
            companyData = createCompanyDataFromFoundCompany(foundCompany);
        }

        return companyDataRepository.save(companyData);
    }

    @Transactional
    public FinancialRecord saveFinancialRecord(BilantResponse bilantResponse) {
        if (bilantResponse == null) {
            return null;
        }

        String cui = String.valueOf(bilantResponse.getCui());
        Integer year = bilantResponse.getAn();

        Optional<FinancialRecord> existingRecord = financialRecordRepository.findByCuiAndYear(cui, year);

        FinancialRecord financialRecord;
        if (existingRecord.isPresent()) {
            financialRecord = existingRecord.get();
            updateFinancialRecordFromBilantResponse(financialRecord, bilantResponse);
        } else {
            financialRecord = createFinancialRecordFromBilantResponse(bilantResponse);
        }

        return financialRecordRepository.save(financialRecord);
    }

    public Optional<CompanyData> getCompanyByCui(String cui) {
        return companyDataRepository.findByCui(cui);
    }

    public List<FinancialRecord> getFinancialRecordsByCui(String cui) {
        return financialRecordRepository.findByCuiOrderByYearDesc(cui);
    }

    public List<FinancialRecord> getFinancialRecordsByCuiAndYearRange(String cui, Integer startYear, Integer endYear) {
        return financialRecordRepository.findByCuiAndYearRange(cui, startYear, endYear);
    }

    private CompanyData createCompanyDataFromFoundCompany(FoundCompany foundCompany) {
        GeneralData generalData = foundCompany.getDateGenerale();
        if (generalData == null) {
            return null;
        }
        
        return CompanyData.builder()
                .cui(String.valueOf(generalData.getCui()))
                .companyName(generalData.getDenumire())
                .fiscalAddress(generalData.getAdresa())
                .tradeRegisterNo(generalData.getNrRegCom())
                .phone(generalData.getTelefon())
                .fax(generalData.getFax())
                .postalCode(generalData.getCodPostal())
                .registrationDate(parseRegistrationDate(generalData.getDataInregistrare()))
                .caenCode(generalData.getCaen())
                .caenDescription(generalData.getDenumireCaen())
                .isVatPayer(foundCompany.getInregistrareScopTva() != null && foundCompany.getInregistrareScopTva().getScpTVA())
                .isInactive(foundCompany.getStareInactiv() != null && foundCompany.getStareInactiv().getStareInactiva())
                .build();
    }

    private void updateCompanyDataFromFoundCompany(CompanyData companyData, FoundCompany foundCompany) {
        GeneralData generalData = foundCompany.getDateGenerale();
        if (generalData == null) {
            return;
        }
        
        companyData.setCompanyName(generalData.getDenumire());
        companyData.setFiscalAddress(generalData.getAdresa());
        companyData.setTradeRegisterNo(generalData.getNrRegCom());
        companyData.setPhone(generalData.getTelefon());
        companyData.setFax(generalData.getFax());
        companyData.setPostalCode(generalData.getCodPostal());
        companyData.setRegistrationDate(parseRegistrationDate(generalData.getDataInregistrare()));
        companyData.setCaenCode(generalData.getCaen());
        companyData.setCaenDescription(generalData.getDenumireCaen());
        companyData.setIsVatPayer(foundCompany.getInregistrareScopTva() != null && foundCompany.getInregistrareScopTva().getScpTVA());
        companyData.setIsInactive(foundCompany.getStareInactiv() != null && foundCompany.getStareInactiv().getStareInactiva());
    }

    private FinancialRecord createFinancialRecordFromBilantResponse(BilantResponse bilantResponse) {
        Map<String, Indicator> indicatorMap = bilantResponse.getIndicatori().stream()
                .collect(Collectors.toMap(Indicator::getCodIndicator, i -> i));

        return FinancialRecord.builder()
                .cui(String.valueOf(bilantResponse.getCui()))
                .year(bilantResponse.getAn())
                .netTurnover(getIndicatorValue(indicatorMap, "I13"))
                .netProfit(getIndicatorValue(indicatorMap, "I18"))
                .totalExpenses(getIndicatorValue(indicatorMap, "I15"))
                .liabilities(getIndicatorValue(indicatorMap, "I7"))
                .totalCapital(getIndicatorValue(indicatorMap, "I10"))
                .fixedAssets(getIndicatorValue(indicatorMap, "I1"))
                .averageEmployees(getIndicatorValueAsInteger(indicatorMap, "I20"))
                .build();
    }

    private void updateFinancialRecordFromBilantResponse(FinancialRecord financialRecord, BilantResponse bilantResponse) {
        Map<String, Indicator> indicatorMap = bilantResponse.getIndicatori().stream()
                .collect(Collectors.toMap(Indicator::getCodIndicator, i -> i));

        financialRecord.setYear(bilantResponse.getAn());
        financialRecord.setNetTurnover(getIndicatorValue(indicatorMap, "I13"));
        financialRecord.setNetProfit(getIndicatorValue(indicatorMap, "I18"));
        financialRecord.setTotalExpenses(getIndicatorValue(indicatorMap, "I15"));
        financialRecord.setLiabilities(getIndicatorValue(indicatorMap, "I7"));
        financialRecord.setTotalCapital(getIndicatorValue(indicatorMap, "I10"));
        financialRecord.setFixedAssets(getIndicatorValue(indicatorMap, "I1"));
        financialRecord.setAverageEmployees(getIndicatorValueAsInteger(indicatorMap, "I20"));
    }

    private Long getIndicatorValue(Map<String, Indicator> indicatorMap, String code) {
        Indicator indicator = indicatorMap.get(code);
        return indicator != null ? (long) indicator.getValoareIndicator() : 0L;
    }

    private Integer getIndicatorValueAsInteger(Map<String, Indicator> indicatorMap, String code) {
        Indicator indicator = indicatorMap.get(code);
        return indicator != null ? (int) indicator.getValoareIndicator() : 0;
    }

    private LocalDate parseRegistrationDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            log.warn("Could not parse registration date: {}", dateString);
            return null;
        }
    }
}

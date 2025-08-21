package com.backend.service;

import com.backend.model.entity.CompanyData;
import com.backend.model.entity.FinancialRecord;
import com.backend.model.response.AnafResponse;
import com.backend.model.response.BilantResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyFinancialService {

    private final AnafService anafService;
    private final BilantService bilantService;
    private final DatabaseService databaseService;

    public CompanyData processCompanyCompletely(String cui, int numberOfYears) throws IOException, InterruptedException {
        log.info("Processing company with CUI: {} for {} years", cui, numberOfYears);
        
        AnafResponse anafResponse = anafService.checkAnaf(Integer.parseInt(cui));
        if (anafResponse == null || anafResponse.getFound() == null || anafResponse.getFound().isEmpty()) {
            log.warn("No company found for CUI: {}", cui);
            return null;
        }

        CompanyData companyData = databaseService.saveCompanyData(anafResponse);
        log.info("Saved company data for: {}", companyData.getCompanyName());

        int startYear = getCurrentFinancialYear();
        for (int i = 0; i < numberOfYears; i++) {
            int year = startYear - i;
            try {
                BilantResponse bilantResponse = bilantService.checkBilant(Integer.parseInt(cui), year);
                if (bilantResponse != null) {
                    FinancialRecord financialRecord = databaseService.saveFinancialRecord(bilantResponse);
                    log.info("Saved financial data for year {} - Net Turnover: {}", 
                            year, financialRecord.getNetTurnover());
                } else {
                    log.warn("No financial data found for CUI: {} and year: {}", cui, year);
                }
            } catch (Exception e) {
                log.error("Error processing financial data for CUI: {} and year: {}", cui, year, e);
            }
        }

        return companyData;
    }

    public Optional<CompanyData> getCompanyFromDatabase(String cui) {
        return databaseService.getCompanyByCui(cui);
    }


    public List<FinancialRecord> getFinancialRecordsFromDatabase(String cui) {
        return databaseService.getFinancialRecordsByCui(cui);
    }

    public List<FinancialRecord> getFinancialRecordsForPeriod(String cui, Integer startYear, Integer endYear) {
        return databaseService.getFinancialRecordsByCuiAndYearRange(cui, startYear, endYear);
    }

    public CompanyData updateCompanyData(String cui) throws IOException, InterruptedException {
        log.info("Updating company data for CUI: {}", cui);
        
        AnafResponse anafResponse = anafService.checkAnaf(Integer.parseInt(cui));
        if (anafResponse == null || anafResponse.getFound() == null || anafResponse.getFound().isEmpty()) {
            log.warn("No company found for CUI: {} during update", cui);
            return null;
        }

        return databaseService.saveCompanyData(anafResponse);
    }

    public FinancialRecord updateFinancialDataForYear(String cui, Integer year) throws IOException, InterruptedException {
        log.info("Updating financial data for CUI: {} and year: {}", cui, year);
        
        BilantResponse bilantResponse = bilantService.checkBilant(Integer.parseInt(cui), year);
        if (bilantResponse == null) {
            log.warn("No financial data found for CUI: {} and year: {}", cui, year);
            return null;
        }

        return databaseService.saveFinancialRecord(bilantResponse);
    }

    public int getCurrentFinancialYear() {
        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonth().getValue();
        

        if (currentMonth > 7) {
            return now.minusYears(1).getYear();
        } else if (currentMonth <= 6) {
            return now.minusYears(2).getYear();
        } else {
            return now.minusYears(2).getYear();
        }
    }
}

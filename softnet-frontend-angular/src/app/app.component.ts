import { Component , OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import {CompanyDataModel} from './models/company-data.model';
import {FinancialRecordModel} from './models/financial-record.model';
import {ApiService} from './services/api.service';
import {CacheService} from './services/cache.service';
import {catchError, of} from 'rxjs';
import {SearchFormComponent} from './components/search-form/search-form.component';
import {CompanyInfoComponent} from './components/company-info/company-info.component';
import {FinancialTableComponent} from './components/financial-table/financial-table.component';
import {FinancialChartComponent} from './components/financial-chart/financial-chart.component';
import { SearchHistoryComponent } from './components/search-history/search-history.component';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    SearchFormComponent,
    CompanyInfoComponent,
    FinancialTableComponent,
    FinancialChartComponent,
    SearchHistoryComponent,
    FormsModule
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  companyData: CompanyDataModel | null = null;
  financialRecords: FinancialRecordModel[] = [];
  searchHistory: string[] = [];
  isGeneratingPdf = false;

  lastSearchParams: {cui: string, years: number} | null = null;

  isLoading = false;
  errorMessage: string | null = null;
  isFromCache = false;
  selectedCui: string = '';
  selectedYears: number = 3;

  private currentSessionId: string | null = null;

  selectedFile: File | null = null;
  verificationResult: { valid: boolean; message: string } | null = null;

  constructor(private apiService: ApiService, private cacheService: CacheService) {}

  ngOnInit(): void {
    this.searchHistory = this.cacheService.getSearchHistory();
  }

  onSearch (searchParams: {cui: string, years: number}): void {
    console.log(`Search triggered for CUI: ${searchParams.cui}, Years: ${searchParams.years}`);

    this.selectedCui = searchParams.cui;
    this.selectedYears = searchParams.years;

    const isDifferentSearch = !this.lastSearchParams ||
      this.lastSearchParams.cui !== searchParams.cui ||
      this.lastSearchParams.years !== searchParams.years;

    const isYearsChanged = this.lastSearchParams &&
      this.lastSearchParams.cui === searchParams.cui &&
      this.lastSearchParams.years !== searchParams.years;

    if (isDifferentSearch) {
      console.log(`New search parameters detected, clearing cache...`);
      this.lastSearchParams = { ...searchParams };
    }

    this.isLoading = true;
    this.errorMessage = null;
    this.companyData = null;
    this.financialRecords = [];
    this.isFromCache = false;

    if (isYearsChanged) {
      console.log(`Years parameter changed for same CUI, forcing backend update...`);
      this.apiService.updateFinancialRecords(searchParams.cui, searchParams.years).pipe(
        catchError(updateError => {
          console.error('Update Error:', updateError);
          return of(null);
        })
      ).subscribe(() => {
        this.fetchCompanyData(searchParams);
      });
    } else {
      this.fetchCompanyData(searchParams);
    }
  }

  generatePdf(): void {
    if (!this.companyData) {
      console.error('No company data available for PDF generation');
      return;
    }

    console.log('Starting PDF generation for CUI:', this.companyData.cui);
    this.isGeneratingPdf = true;

    this.apiService.downloadCompanyPdf(this.companyData.cui, this.selectedYears)
      .subscribe({
        next: (pdfBlob: Blob) => {
          console.log('PDF generated successfully. Size:', pdfBlob.size);

          const filename = `company_report_${this.companyData!.cui}.pdf`;
          const url = window.URL.createObjectURL(pdfBlob);
          const link = document.createElement('a');
          link.href = url;
          link.download = filename;
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          window.URL.revokeObjectURL(url);

          this.isGeneratingPdf = false;
        },
        error: (error) => {
          console.error('Error generating PDF:', error);
          alert('Error generating PDF. Please try again.');
          this.isGeneratingPdf = false;
        }
      });
  }

  sendReportByEmail(): void {
    if (!this.companyData) {
      alert('Please search for a company first.');
      return;
    }

    console.log(`Starting OAuth verification for CUI ${this.companyData.cui}`);
    this.isGeneratingPdf = true;

    this.apiService.requestEmailVerification(this.companyData.cui, this.selectedYears)
      .subscribe({
        next: (response) => {
          console.log('OAuth verification response:', response);

          if (response.authUrl) {
            this.currentSessionId = response.sessionId;

            const popup = window.open(
              response.authUrl,
              'google-auth',
              'width=500,height=600,scrollbars=yes,resizable=yes'
            );

            const messageListener = (event: MessageEvent) => {
              if (event.data.type === 'GOOGLE_AUTH_SUCCESS') {
                // Clear timeout and close popup
                window.clearTimeout(authTimeout);
                if (popup) {
                  popup.close();
                }

                this.sendVerifiedEmailAfterAuth();

                window.removeEventListener('message', messageListener);
              } else if (event.data.type === 'GOOGLE_AUTH_ERROR') {
                window.clearTimeout(authTimeout);
                console.error('Google authentication error:', event.data.message);
                alert('Error during Google authentication. Please try again.');

                if (popup) {
                  popup.close();
                }

                this.isGeneratingPdf = false;
                window.removeEventListener('message', messageListener);
              }
            };

            window.addEventListener('message', messageListener);

            const authTimeout = window.setTimeout(() => {
              window.removeEventListener('message', messageListener);
              try { popup?.close(); } catch {}
              console.log('OAuth popup timed out or was closed without completing authentication');
              this.isGeneratingPdf = false;
              alert('Authentication timed out or the window was closed. Please try again.');
            }, 2 * 60 * 1000);
            alert('A Google authentication window will open. Please log in to verify your email.');

          } else {
            console.error('No authUrl received from backend');
            alert('Error initiating verification process.');
            this.isGeneratingPdf = false;
          }
        },
        error: (error) => {
          console.error('Error requesting verification:', error);
          alert('Error connecting to server. Please try again.');
          this.isGeneratingPdf = false;
        }
      });
  }

  private sendVerifiedEmailAfterAuth(): void {
    if (!this.currentSessionId || !this.companyData) {
      alert('Verification session expired. Please try again.');
      this.isGeneratingPdf = false;
      return;
    }

    this.apiService.sendVerifiedEmail(this.currentSessionId)
      .subscribe({
        next: (response) => {
          console.log('Email sent successfully:', response);
          alert('Report sent successfully to your verified Google email!');
          this.currentSessionId = null; // Reset session ID
          this.isGeneratingPdf = false;
        },
        error: (error) => {
          console.error('Error sending email:', error);
          if (error.error && error.error.error) {
            alert('Error: ' + error.error.error);
          } else {
            alert('Error sending email. Please try again.');
          }
          this.currentSessionId = null;
          this.isGeneratingPdf = false;
        }
      });
  }

  getCacheStats() {
    return this.cacheService.getCacheStats();
  }

  clearAllCache(): void {
    this.cacheService.clearAllCache();
    console.log('All cache cleared');
  }

  clearSearchHistory(): void {
    this.cacheService.clearSearchHistory();
    this.searchHistory = [];
    console.log('Search history cleared');
  }

  refreshCurrentData(): void {
    if (this.lastSearchParams) {
      console.log('Refreshing current data...');
      this.apiService.refreshCompanyData(this.lastSearchParams.cui, this.lastSearchParams.years)
        .subscribe(response => {
          if (response) {
            this.companyData = response.company;
            this.financialRecords = response.records;
            console.log('Data refreshed successfully');
          }
        });
    }
  }

  onHistorySelect(cui: string): void {
    this.selectedCui = cui;
    const years = this.lastSearchParams?.years || 3;
    this.onSearch({ cui, years });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
    }
  }

  verifyPdf(): void {
    if (!this.selectedFile) {
      alert('Please upload a PDF generated by this site.');
      return;
    }

    const formData = new FormData();
    formData.append('file', this.selectedFile);

    this.apiService.verifyPdf(formData).subscribe({
      next: (response) => {
        this.verificationResult = { valid: true, message: `PDF is valid. Checksum: ${response.data}` };
      },
      error: (error) => {
        console.error('Verification failed:', error);
        const msg = error?.error?.message || 'PDF verification failed. Please try again.';
        this.verificationResult = { valid: false, message: msg };
      }
    });
  }

  private fetchCompanyData(searchParams: {cui: string, years: number}): void {
    const cachedData = this.cacheService.getCompanyData(searchParams.cui, searchParams.years);
    this.isFromCache = cachedData !== null;

    this.apiService.getFullCompanyInfo(searchParams.cui, searchParams.years).pipe(
      catchError(error => {
        console.error('API Error:', error);
        if (error.message.includes('404') || error.message.includes('not found')) {
          console.log('Company not found in database, processing from ANAF...');
          return this.apiService.processCompany(searchParams.cui, searchParams.years).pipe(
            catchError(processError => {
              console.error('Process Error:', processError);
              this.errorMessage = `Error processing company: ${processError.message || 'Unable to fetch from ANAF'}`;
              return of(null);
            })
          );
        }
        this.errorMessage = `Error fetching data: ${error.message || 'Unknown error'}. Please check if the backend is running and the CUI exists.`;
        return of(null);
      })
    ).subscribe (response =>{
      console.log('API Response received:', response);
      this.isLoading = false;

      if (response) {
        if (!('company' in response)) {
          console.log('Company processed, fetching full data...');
          this.apiService.getFullCompanyInfo(searchParams.cui, searchParams.years).subscribe(fullResponse => {
            if (fullResponse) {
              this.cacheService.addToSearchHistory(searchParams.cui);
              this.searchHistory = this.cacheService.getSearchHistory();
              this.companyData = fullResponse.company;
              this.financialRecords = fullResponse.records;
              console.log(`Final data loaded: ${this.financialRecords?.length} records for ${searchParams.years} years`);
            }
          });
        } else {
          this.companyData = response.company;
          this.financialRecords = response.records;

          this.cacheService.addToSearchHistory(searchParams.cui);
          this.searchHistory = this.cacheService.getSearchHistory();

          console.log(`Company data set: ${this.companyData?.companyName}`);
          console.log(`Financial records set: ${this.financialRecords?.length} records for ${searchParams.years} years`);

          if (this.financialRecords && this.financialRecords.length > 0) {
            console.log('Financial records received in component:', this.financialRecords);
            console.log('First record netTurnover value:', this.financialRecords[0].netTurnover);
            console.log('First record type check:', typeof this.financialRecords[0].netTurnover);
          }
        }
      }
    });
  }
}

import { Injectable } from '@angular/core';
import {HttpClient, HttpErrorResponse, HttpParams} from '@angular/common/http';
import {forkJoin, Observable, catchError, throwError, tap, of} from 'rxjs';
import { CompanyDataModel } from '../models/company-data.model';
import { FinancialRecordModel } from '../models/financial-record.model';
import { CacheService, CompanyCacheData } from './cache.service';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly API_BASE_URL = 'http://localhost:8080';
  constructor(private http:HttpClient, private cacheService: CacheService) { }

  downloadCompanyPdf(cui: string, years: number = 3): Observable<Blob> {
    const url = `${this.API_BASE_URL}/api/pdf/company/${cui}?years=${years}`;
    return this.http.get(url, { responseType: 'blob' }).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('PDF generation failed:', error);
        return throwError(() => error);
      })
    );
  }

  processAndDownloadPdf(cui: string, years: number = 3): Observable<Blob> {
    const url = `${this.API_BASE_URL}/api/pdf/company/${cui}/process-and-generate?years=${years}`;
    return this.http.post(url, null, { responseType: 'blob' }).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('PDF processing and generation failed:', error);
        return throwError(() => error);
      })
    );
  }

  sendEmailWithReport(recipientEmail: string, cui: string, years: number = 3): Observable<string> {
    const url = `${this.API_BASE_URL}/api/email/send-report`;
    const params = new HttpParams()
      .set('email', recipientEmail)
      .set('cui', cui)
      .set('years', years.toString());

    return this.http.post(url, null, {
      params: params,
      responseType: 'text'
    }).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('Email sending failed:', error);
        return throwError(() => error);
      })
    );
  }

  getFullCompanyInfo(cui: string, years: number = 3, useCache: boolean = true): Observable<{ company: CompanyDataModel, records: FinancialRecordModel[] }> {
    console.log(`Starting API calls for CUI: ${cui} with ${years} years (useCache: ${useCache})`);

    if (useCache) {
      const cachedData = this.cacheService.getCompanyData(cui, years);
      if (cachedData) {
        console.log('Returning cached data');
        return of({
          company: cachedData.company,
          records: cachedData.records
        });
      }
    }

    const timestamp = new Date().getTime();

    return forkJoin({
      company: this.getCompanyData(cui),
      records: this.getFinancialRecordsWithTimestamp(cui, years, timestamp)
    }).pipe(
      tap(response => {
        if (response && response.company && response.records) {
          const cacheData: CompanyCacheData = {
            company: response.company,
            records: response.records,
            years: years
          };
          this.cacheService.setCompanyData(cui, years, cacheData);
          console.log(`Data cached for CUI: ${cui}, Years: ${years}`);
        }
      })
    );
  }

  private getFinancialRecordsWithTimestamp(cui: string, years: number, timestamp: number): Observable<FinancialRecordModel[]> {
    const url = `${this.API_BASE_URL}/api/bilant/${cui}/period?years=${years}&_t=${timestamp}`;
    console.log(`Calling financial records API: ${url} with ${years} years (timestamp: ${timestamp})`);
    return this.http.get<FinancialRecordModel[]>(url).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error(`Financial Records API Error (${error.status}):`, error.message);
        console.error('Full error:', error);
        return throwError(() => new Error(`Financial Records API failed: ${error.status} - ${error.message}`));
      })
    ).pipe(
      catchError((error) => {
        console.error('Error in financial records:', error);
        return throwError(() => error);
      })
    ).pipe(
      tap((records: FinancialRecordModel[]) => {
        console.log('Financial records received from backend:', records);
        if (records && records.length > 0) {
          console.log('First record netTurnover:', records[0].netTurnover);
          console.log('First record data:', JSON.stringify(records[0], null, 2));
        }
      })
    );
  }

  processCompany(cui: string, years: number = 3): Observable<CompanyDataModel> {
    const url = `${this.API_BASE_URL}/api/firma/${cui}/process?years=${years}`;
    console.log(`Processing company: ${url}`);
    return this.http.post<CompanyDataModel>(url, {}).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error(`Process Company API Error (${error.status}):`, error.message);
        console.error('Full error:', error);
        return throwError(() => new Error(`Process Company API failed: ${error.status} - ${error.message}`));
      })
    );
  }

  updateFinancialRecords(cui: string, years: number = 3): Observable<CompanyDataModel> {
    console.log(`Forcing update of financial records for CUI: ${cui} with ${years} years`);

    this.cacheService.clearCompanyCache(cui, years);

    return this.processCompany(cui, years);
  }

  getCacheStats() {
    return this.cacheService.getCacheStats();
  }

  clearCache(cui?: string, years?: number): void {
    if (cui) {
      this.cacheService.clearCompanyCache(cui, years);
    } else {
      this.cacheService.clearAllCache();
    }
  }

  refreshCompanyData(cui: string, years: number = 3): Observable<{ company: CompanyDataModel, records: FinancialRecordModel[] }> {
    console.log(`Force refreshing data for CUI: ${cui}, Years: ${years}`);

    this.cacheService.clearCompanyCache(cui, years);
    return this.getFullCompanyInfo(cui, years, false);
  }

  private getCompanyData(cui: string): Observable<CompanyDataModel> {
    const url = `${this.API_BASE_URL}/api/firma/${cui}`;
    console.log(`Calling company API: ${url}`);
    return this.http.get<CompanyDataModel>(url).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error(`Company API Error (${error.status}):`, error.message);
        console.error('Full error:', error);
        return throwError(() => new Error(`Company API failed: ${error.status} - ${error.message}`));
      })
    );
  }

  private getFinancialRecords(cui: string, years: number = 3): Observable<FinancialRecordModel[]> {
    const url = `${this.API_BASE_URL}/api/bilant/${cui}/period?years=${years}`;
    console.log(`Calling financial records API: ${url} with ${years} years`);
    return this.http.get<FinancialRecordModel[]>(url).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error(`Financial Records API Error (${error.status}):`, error.message);
        console.error('Full error:', error);
        return throwError(() => new Error(`Financial Records API failed: ${error.status} - ${error.message}`));
      })
    );
  }

  requestEmailVerification(cui: string, years: number): Observable<any> {
    const url = `${this.API_BASE_URL}/api/email/request-verification`;
    const params = new HttpParams()
      .set('cui', cui)
      .set('years', years.toString());

    return this.http.post(url, {}, { params }).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('Email verification request failed:', error);
        return throwError(() => error);
      })
    );
  }

  sendVerifiedEmail(sessionId: string): Observable<any> {
    const url = `${this.API_BASE_URL}/api/email/send-verified`;
    const params = new HttpParams()
      .set('sessionId', sessionId);

    return this.http.post(url, {}, { params }).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('Verified email sending failed:', error);
        return throwError(() => error);
      })
    );
  }

  verifyPdf(formData: FormData): Observable<{ data: string }> {
    const url = `${this.API_BASE_URL}/api/pdf/verify`;
    return this.http.post<{ data: string }>(url, formData).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('PDF verification failed:', error);
        return throwError(() => error);
      })
    );
  }
}

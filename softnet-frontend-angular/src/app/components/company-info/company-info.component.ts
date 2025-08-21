import {Component, Input} from '@angular/core';
import {CompanyDataModel} from '../../models/company-data.model';
import {CommonModule, DatePipe} from '@angular/common';
import {FinancialRecordModel} from '../../models/financial-record.model';

@Component({
  selector: 'app-company-info',
  imports: [
    DatePipe,
    CommonModule
  ],
  templateUrl: './company-info.component.html',
  styleUrl: './company-info.component.css'
})
export class CompanyInfoComponent {
  @Input() data: CompanyDataModel | null = null;

  getYearRange(records: FinancialRecordModel[]): string {
    if (!records || records.length === 0) return 'No data';
    
    const years = records.map(r => r.year).sort((a, b) => a - b);
    const minYear = Math.min(...years);
    const maxYear = Math.max(...years);
    
    return minYear === maxYear ? `${minYear}` : `${minYear}-${maxYear}`;
  }
}

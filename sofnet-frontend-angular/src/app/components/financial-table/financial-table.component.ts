import {Component, Input} from '@angular/core';
import {FinancialRecordModel} from '../../models/financial-record.model';
import {CommonModule, NgClass} from '@angular/common';

@Component({
  selector: 'app-financial-table',
  standalone: true,
  imports: [
    NgClass,
    CommonModule
  ],
  templateUrl: './financial-table.component.html',
  styleUrl: './financial-table.component.css'
})
export class FinancialTableComponent {
  @Input() records: FinancialRecordModel[] = [];

  getDisplayProfit(record: FinancialRecordModel): number {
    console.log(`Calculating profit for year ${record.year}:`);
    console.log(`- netProfit: ${record.netProfit}`);
    console.log(`- netTurnover: ${record.netTurnover}`);
    console.log(`- totalExpenses: ${record.totalExpenses}`);
    
    if (record.netProfit !== 0) {
      console.log(`Using provided netProfit: ${record.netProfit}`);
      return record.netProfit;
    }

    const calculatedProfit = record.netTurnover - record.totalExpenses;
    console.log(`Calculated profit: ${calculatedProfit}`);

    if (Math.abs(calculatedProfit) > 1000) {
      console.log(`Using calculated profit: ${calculatedProfit}`);
      return calculatedProfit;
    }

    console.log(`Using original netProfit: ${record.netProfit}`);
    return record.netProfit;
  }
}

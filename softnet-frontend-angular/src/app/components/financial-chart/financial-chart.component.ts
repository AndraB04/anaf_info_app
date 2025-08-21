import { Component, Input, OnChanges, SimpleChanges, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FinancialRecordModel } from '../../models/financial-record.model';
import { ChartConfiguration, ChartType } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';

@Component({
  selector: 'app-financial-chart',
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  templateUrl: './financial-chart.component.html',
})
export class FinancialChartComponent implements OnChanges {
  @ViewChild(BaseChartDirective) chart?: BaseChartDirective;

  @Input() records: FinancialRecordModel[] = [];

  public barChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    scales: {
      y: {
        beginAtZero: true,
        ticks: {
          callback: function (value) {
            return (value as number).toLocaleString('en-US') + ' LEI';
          }
        }
      }
    },
    plugins: {
      legend: { display: true },
      tooltip: {
        callbacks: {
          label: function (context) {
            let label = context.dataset.label || '';
            if (label) {
              label += ': ';
            }
            if (context.parsed.y !== null) {
              label += context.parsed.y.toLocaleString('en-US') + ' LEI';
            }
            return label;
          }
        }
      }
    }
  };

  public barChartType: ChartType = 'bar';

  public barChartData: ChartConfiguration['data'] = {
    labels: [],
    datasets: [
      { data: [], label: 'Turnover' },
      { data: [], label: 'Net Profit' }
    ]
  };

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['records'] && this.records?.length > 0) {
      this.prepareChartData();
    }
  }

  private prepareChartData(): void {
    const sortedRecords = [...this.records].sort((a, b) => a.year - b.year);

    const labels = sortedRecords.map(record => record.year.toString());
    const turnoverData = sortedRecords.map(record => record.netTurnover);
    const profitData = sortedRecords.map(record => record.netProfit);

    this.barChartData = {
      labels: labels,
      datasets: [
        { data: turnoverData, label: 'Turnover', backgroundColor: 'rgba(54, 162, 235, 0.6)' },
        { data: profitData, label: 'Net Profit', backgroundColor: 'rgba(75, 192, 192, 0.6)' }
      ]
    };

    this.chart?.update();
  }
}

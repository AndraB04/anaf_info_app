import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FinancialTableComponent } from './financial-table.component';

describe('FinancialTableComponent', () => {
  let component: FinancialTableComponent;
  let fixture: ComponentFixture<FinancialTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FinancialTableComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FinancialTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

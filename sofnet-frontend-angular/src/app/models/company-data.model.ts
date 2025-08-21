import {FinancialRecordModel} from './financial-record.model';

export interface CompanyDataModel{
  cui:string;
  companyName:string;
  fiscalAddress:string;
  tradeRegisterNo:string;
  phone:string;
  fax:string;
  postalCode:string;
  registrationDate:Date;
  caenCode: number;
  caenDescription: string;
  isVatPayer: boolean;
  isInactive: boolean;
  financialRecords: FinancialRecordModel[];

}

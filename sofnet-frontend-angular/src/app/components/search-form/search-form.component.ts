import { Component, EventEmitter, Output, Input } from '@angular/core';
import {FormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';

@Component({
  selector: 'app-search-form',
  imports: [
    FormsModule,
    CommonModule
  ],
  templateUrl: './search-form.component.html',
  styleUrl: './search-form.component.css'
})
export class SearchFormComponent {
  @Output() search = new EventEmitter<{cui: string, years: number}>();
  @Input() cui = '';
  years = 3;
  
  handleSubmit(): void {
    if (this.cui && this.cui.trim() !== '') {
      this.search.emit({
        cui: this.cui.trim(),
        years: this.years
      });
    }
  }
}

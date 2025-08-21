import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CacheService } from '../../services/cache.service';

@Component({
  selector: 'app-search-history',
  imports: [CommonModule],
  templateUrl: './search-history.component.html',
  styleUrl: './search-history.component.css'
})
export class SearchHistoryComponent {
  @Input() searchHistory: string[] = [];
  @Input() selectedCui: string = '';
  @Output() selectHistory = new EventEmitter<string>();
  @Output() clearHistory = new EventEmitter<void>();

  constructor(private cacheService: CacheService) {}

  onSelectCui(cui: string): void {
    this.selectHistory.emit(cui);
  }

  onClearHistory(): void {
    this.clearHistory.emit();
  }

  onRemoveItem(cui: string): void {
    this.cacheService.removeFromSearchHistory(cui);
    this.searchHistory = this.cacheService.getSearchHistory();
  }
}

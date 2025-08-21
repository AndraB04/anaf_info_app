import { Injectable } from '@angular/core';
import { CompanyDataModel } from '../models/company-data.model';
import { FinancialRecordModel } from '../models/financial-record.model';

export interface CacheEntry<T> {
  data: T;
  timestamp: number;
  expiresAt: number;
}

export interface CompanyCacheData {
  company: CompanyDataModel;
  records: FinancialRecordModel[];
  years: number;
}

@Injectable({
  providedIn: 'root'
})
export class CacheService {
  private readonly CACHE_PREFIX = 'cache_';
  private readonly DEFAULT_TTL = 5 * 60 * 1000;

  constructor() {}

  private isLocalStorageAvailable(): boolean {
    try {
      return typeof Storage !== 'undefined' && typeof localStorage !== 'undefined';
    } catch {
      return false;
    }
  }

  setCompanyData(cui: string, years: number, data: CompanyCacheData, ttl: number = this.DEFAULT_TTL): void {
    if (!this.isLocalStorageAvailable()) {
      console.log('localStorage not available, skipping cache set');
      return;
    }

    const cacheKey = this.getCacheKey(cui, years);
    const cacheEntry: CacheEntry<CompanyCacheData> = {
      data: data,
      timestamp: Date.now(),
      expiresAt: Date.now() + ttl
    };

    try {
      localStorage.setItem(cacheKey, JSON.stringify(cacheEntry));
      console.log(`Cache set for ${cacheKey}, expires in ${ttl / 1000} seconds`);
    } catch (error) {
      console.error('Error saving to cache:', error);
      this.clearExpiredEntries();
      try {
        localStorage.setItem(cacheKey, JSON.stringify(cacheEntry));
      } catch (retryError) {
        console.error('Failed to save to cache after cleanup:', retryError);
      }
    }
  }

  getCompanyData(cui: string, years: number): CompanyCacheData | null {
    if (!this.isLocalStorageAvailable()) {
      console.log('localStorage not available, returning null');
      return null;
    }

    const cacheKey = this.getCacheKey(cui, years);

    try {
      const cached = localStorage.getItem(cacheKey);
      if (!cached) {
        console.log(`No cache found for ${cacheKey}`);
        return null;
      }

      const cacheEntry: CacheEntry<CompanyCacheData> = JSON.parse(cached);

      if (Date.now() > cacheEntry.expiresAt) {
        console.log(`Cache expired for ${cacheKey}`);
        localStorage.removeItem(cacheKey);
        return null;
      }

      console.log(`Cache hit for ${cacheKey}`);
      return cacheEntry.data;
    } catch (error) {
      console.error('Error reading from cache:', error);
      localStorage.removeItem(cacheKey);
      return null;
    }
  }

  hasValidCache(cui: string, years: number): boolean {
    return this.getCompanyData(cui, years) !== null;
  }

  clearCompanyCache(cui: string, years?: number): void {
    if (!this.isLocalStorageAvailable()) {
      console.log('localStorage not available, skipping cache clear');
      return;
    }

    if (years !== undefined) {
      const cacheKey = this.getCacheKey(cui, years);
      localStorage.removeItem(cacheKey);
      console.log(`Cache cleared for ${cacheKey}`);
    } else {
      const keysToRemove: string[] = [];
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        if (key && key.startsWith(`${this.CACHE_PREFIX}${cui}_`)) {
          keysToRemove.push(key);
        }
      }
      keysToRemove.forEach(key => localStorage.removeItem(key));
      console.log(`All cache cleared for CUI ${cui}`);
    }
  }

  clearExpiredEntries(): void {
    if (!this.isLocalStorageAvailable()) {
      console.log('localStorage not available, skipping expired entries cleanup');
      return;
    }

    const keysToRemove: string[] = [];
    const now = Date.now();

    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key && key.startsWith(this.CACHE_PREFIX)) {
        try {
          const cached = localStorage.getItem(key);
          if (cached) {
            const cacheEntry: CacheEntry<any> = JSON.parse(cached);
            if (now > cacheEntry.expiresAt) {
              keysToRemove.push(key);
            }
          }
        } catch (error) {
          // If we can't parse it, remove it
          keysToRemove.push(key);
        }
      }
    }

    keysToRemove.forEach(key => localStorage.removeItem(key));
    if (keysToRemove.length > 0) {
      console.log(`Cleared ${keysToRemove.length} expired cache entries`);
    }
  }

  clearAllCache(): void {
    if (!this.isLocalStorageAvailable()) {
      console.log('localStorage not available, skipping cache clear');
      return;
    }

    const keysToRemove: string[] = [];
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key && key.startsWith(this.CACHE_PREFIX)) {
        keysToRemove.push(key);
      }
    }
    keysToRemove.forEach(key => localStorage.removeItem(key));
    console.log(`Cleared all cache (${keysToRemove.length} entries)`);
  }

  getCacheStats(): { total: number; expired: number; size: string } {
    if (!this.isLocalStorageAvailable()) {
      return { total: 0, expired: 0, size: '0 KB' };
    }

    let total = 0;
    let expired = 0;
    let totalSize = 0;
    const now = Date.now();

    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key && key.startsWith(this.CACHE_PREFIX)) {
        total++;
        const value = localStorage.getItem(key);
        if (value) {
          totalSize += value.length;
          try {
            const cacheEntry: CacheEntry<any> = JSON.parse(value);
            if (now > cacheEntry.expiresAt) {
              expired++;
            }
          } catch (error) {
            expired++;
          }
        }
      }
    }

    return {
      total,
      expired,
      size: `${(totalSize / 1024).toFixed(2)} KB`
    };
  }

  private getCacheKey(cui: string, years: number): string {
    return `cache_${cui}_${years}`;
  }

  private readonly SEARCH_HISTORY_KEY = 'search_history';
  private readonly MAX_HISTORY_ITEMS = 10;

  addToSearchHistory(cui: string): void {
    if (!this.isLocalStorageAvailable()) {
      console.log('localStorage not available, skipping search history update');
      return;
    }

    try {
      const history = this.getSearchHistory();

      const filteredHistory = history.filter(item => item !== cui);

      filteredHistory.unshift(cui);

      const limitedHistory = filteredHistory.slice(0, this.MAX_HISTORY_ITEMS);

      localStorage.setItem(this.SEARCH_HISTORY_KEY, JSON.stringify(limitedHistory));
      console.log(`Added ${cui} to search history`);
    } catch (error) {
      console.error('Error saving search history:', error);
    }
  }

  getSearchHistory(): string[] {
    if (!this.isLocalStorageAvailable()) {
      return [];
    }

    try {
      const history = localStorage.getItem(this.SEARCH_HISTORY_KEY);
      return history ? JSON.parse(history) : [];
    } catch (error) {
      console.error('Error reading search history:', error);
      return [];
    }
  }


  clearSearchHistory(): void {
    if (!this.isLocalStorageAvailable()) {
      console.log('localStorage not available, skipping search history clear');
      return;
    }

    localStorage.removeItem(this.SEARCH_HISTORY_KEY);
    console.log('Search history cleared');
  }

  removeFromSearchHistory(cui: string): void {
    if (!this.isLocalStorageAvailable()) {
      console.log('localStorage not available, skipping search history removal');
      return;
    }

    try {
      const history = this.getSearchHistory();
      const filteredHistory = history.filter(item => item !== cui);
      localStorage.setItem(this.SEARCH_HISTORY_KEY, JSON.stringify(filteredHistory));
      console.log(`Removed ${cui} from search history`);
    } catch (error) {
      console.error('Error removing from search history:', error);
    }
  }
}

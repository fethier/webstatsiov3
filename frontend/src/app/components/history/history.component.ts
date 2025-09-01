import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { SpeedTestService, HistoryItem } from '../../services/speed-test.service';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="history-container">
      <div class="page-header mb-4">
        <h2>Test History</h2>
        <p class="text-muted">View your previous speed test results</p>
      </div>
      
      <div class="controls mb-4">
        <div class="row align-items-center">
          <div class="col-md-6">
            <div class="d-flex align-items-center">
              <label class="form-label me-2 mb-0">Show:</label>
              <select class="form-select w-auto" [(ngModel)]="pageSize" (change)="loadHistory()">
                <option value="10">10 results</option>
                <option value="25">25 results</option>
                <option value="50">50 results</option>
                <option value="100">100 results</option>
              </select>
            </div>
          </div>
          <div class="col-md-6 text-md-end">
            <button class="btn btn-outline-primary" (click)="loadHistory()" [disabled]="loading">
              <span *ngIf="loading" class="spinner-border spinner-border-sm me-2"></span>
              {{ loading ? 'Loading...' : 'Refresh' }}
            </button>
          </div>
        </div>
      </div>
      
      <!-- History Table -->
      <div class="history-card" *ngIf="historyItems && historyItems.length > 0">
        <div class="table-responsive">
          <table class="table table-hover">
            <thead class="table-light">
              <tr>
                <th>Date & Time</th>
                <th class="text-center">Download</th>
                <th class="text-center">Upload</th>
                <th class="text-center">Latency</th>
                <th class="text-center">Server</th>
                <th class="text-center">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let item of historyItems; trackBy: trackByResultId">
                <td>
                  <div class="timestamp">
                    <div class="date">{{ item.testTimestamp | date:'shortDate' }}</div>
                    <div class="time text-muted">{{ item.testTimestamp | date:'shortTime' }}</div>
                  </div>
                </td>
                <td class="text-center">
                  <div class="metric-cell download">
                    <span class="metric-value">{{ item.downloadSpeedMbps | number:'1.1-1' }}</span>
                    <small class="metric-unit">Mbps</small>
                  </div>
                </td>
                <td class="text-center">
                  <div class="metric-cell upload">
                    <span class="metric-value">{{ item.uploadSpeedMbps | number:'1.1-1' }}</span>
                    <small class="metric-unit">Mbps</small>
                  </div>
                </td>
                <td class="text-center">
                  <div class="metric-cell latency">
                    <span class="metric-value">{{ item.latencyMs | number:'1.0-0' }}</span>
                    <small class="metric-unit">ms</small>
                  </div>
                </td>
                <td class="text-center">
                  <div class="server-info">
                    <div class="server-location">{{ item.location || 'N/A' }}</div>
                    <small class="server-provider text-muted">{{ item.serverProvider || 'Unknown' }}</small>
                  </div>
                </td>
                <td class="text-center">
                  <div class="btn-group btn-group-sm">
                    <button class="btn btn-outline-primary" (click)="viewDetails(item)">
                      Details
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      
      <!-- Empty State -->
      <div *ngIf="historyItems && historyItems.length === 0 && !loading" class="empty-state">
        <div class="empty-state-content">
          <div class="empty-state-icon">📊</div>
          <h4>No test history yet</h4>
          <p class="text-muted">Run your first speed test to see results here.</p>
          <button class="btn btn-primary" routerLink="/test">Run Speed Test</button>
        </div>
      </div>
      
      <!-- Loading State -->
      <div *ngIf="loading" class="loading-state">
        <div class="text-center py-5">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Loading...</span>
          </div>
          <p class="mt-2 text-muted">Loading test history...</p>
        </div>
      </div>
      
      <!-- Error State -->
      <div *ngIf="error" class="alert alert-danger">
        <h5>Unable to load history</h5>
        <p>{{ error }}</p>
        <button class="btn btn-outline-danger" (click)="loadHistory()">Try Again</button>
      </div>
      
      <!-- Quick Stats -->
      <div class="quick-stats" *ngIf="historyItems && historyItems.length > 0">
        <div class="row g-3">
          <div class="col-md-3">
            <div class="stat-card">
              <div class="stat-label">Total Tests</div>
              <div class="stat-value">{{ historyItems.length }}</div>
            </div>
          </div>
          <div class="col-md-3">
            <div class="stat-card download">
              <div class="stat-label">Avg Download</div>
              <div class="stat-value">{{ getAverageDownload() | number:'1.1-1' }} Mbps</div>
            </div>
          </div>
          <div class="col-md-3">
            <div class="stat-card upload">
              <div class="stat-label">Avg Upload</div>
              <div class="stat-value">{{ getAverageUpload() | number:'1.1-1' }} Mbps</div>
            </div>
          </div>
          <div class="col-md-3">
            <div class="stat-card latency">
              <div class="stat-label">Avg Latency</div>
              <div class="stat-value">{{ getAverageLatency() | number:'1.0-0' }} ms</div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Details Modal (Simple Implementation) -->
    <div class="modal fade" id="detailsModal" *ngIf="selectedItem" tabindex="-1">
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">Test Details</h5>
            <button type="button" class="btn-close" (click)="closeDetails()"></button>
          </div>
          <div class="modal-body">
            <div class="row g-3">
              <div class="col-md-6">
                <strong>Test Date:</strong>
                <p>{{ selectedItem.testTimestamp | date:'full' }}</p>
              </div>
              <div class="col-md-6">
                <strong>Result ID:</strong>
                <p class="font-monospace">{{ selectedItem.resultId }}</p>
              </div>
              <div class="col-md-4">
                <strong>Download Speed:</strong>
                <p class="text-success">{{ selectedItem.downloadSpeedMbps | number:'1.2-2' }} Mbps</p>
              </div>
              <div class="col-md-4">
                <strong>Upload Speed:</strong>
                <p class="text-warning">{{ selectedItem.uploadSpeedMbps | number:'1.2-2' }} Mbps</p>
              </div>
              <div class="col-md-4">
                <strong>Latency:</strong>
                <p class="text-danger">{{ selectedItem.latencyMs | number:'1.0-0' }} ms</p>
              </div>
              <div class="col-md-6">
                <strong>Server Location:</strong>
                <p>{{ selectedItem.location || 'Not specified' }}</p>
              </div>
              <div class="col-md-6">
                <strong>Server Provider:</strong>
                <p>{{ selectedItem.serverProvider || 'Not specified' }}</p>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" (click)="closeDetails()">Close</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .history-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 2rem 1rem;
    }
    
    .history-card {
      background: white;
      border-radius: 0.75rem;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      overflow: hidden;
    }
    
    .table {
      margin: 0;
    }
    
    .table th {
      border-top: none;
      font-weight: 600;
      color: #495057;
      padding: 1rem 0.75rem;
    }
    
    .table td {
      padding: 1rem 0.75rem;
      vertical-align: middle;
    }
    
    .timestamp .date {
      font-weight: 500;
      color: #333;
    }
    
    .timestamp .time {
      font-size: 0.875rem;
    }
    
    .metric-cell {
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    
    .metric-value {
      font-size: 1.1rem;
      font-weight: 600;
      color: #333;
    }
    
    .metric-unit {
      font-size: 0.75rem;
      color: #6c757d;
      text-transform: uppercase;
    }
    
    .metric-cell.download .metric-value {
      color: #28a745;
    }
    
    .metric-cell.upload .metric-value {
      color: #ffc107;
    }
    
    .metric-cell.latency .metric-value {
      color: #dc3545;
    }
    
    .server-info {
      text-align: center;
    }
    
    .server-location {
      font-weight: 500;
      color: #333;
    }
    
    .server-provider {
      font-size: 0.8rem;
    }
    
    .empty-state {
      background: white;
      border-radius: 0.75rem;
      padding: 4rem 2rem;
      text-align: center;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }
    
    .empty-state-icon {
      font-size: 4rem;
      margin-bottom: 1rem;
    }
    
    .loading-state {
      background: white;
      border-radius: 0.75rem;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }
    
    .quick-stats {
      margin-top: 2rem;
    }
    
    .stat-card {
      background: white;
      border-radius: 0.5rem;
      padding: 1rem;
      text-align: center;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      border-left: 4px solid #007bff;
    }
    
    .stat-card.download {
      border-left-color: #28a745;
    }
    
    .stat-card.upload {
      border-left-color: #ffc107;
    }
    
    .stat-card.latency {
      border-left-color: #dc3545;
    }
    
    .stat-label {
      font-size: 0.9rem;
      color: #6c757d;
      margin-bottom: 0.25rem;
    }
    
    .stat-value {
      font-size: 1.25rem;
      font-weight: bold;
      color: #333;
    }
    
    .page-header h2 {
      color: #333;
      margin-bottom: 0.5rem;
    }
    
    .controls {
      background: white;
      border-radius: 0.5rem;
      padding: 1rem;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    }
  `]
})
export class HistoryComponent implements OnInit {
  historyItems: HistoryItem[] = [];
  loading = false;
  error: string | null = null;
  pageSize = 25;
  selectedItem: HistoryItem | null = null;

  constructor(private speedTestService: SpeedTestService) {}

  ngOnInit() {
    this.loadHistory();
  }

  loadHistory() {
    this.loading = true;
    this.error = null;
    
    this.speedTestService.getUserHistory(this.pageSize).subscribe({
      next: (items) => {
        this.historyItems = items;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Failed to load test history';
        this.loading = false;
      }
    });
  }

  trackByResultId(index: number, item: HistoryItem): string {
    return item.resultId;
  }

  viewDetails(item: HistoryItem) {
    this.selectedItem = item;
    // In a real implementation, you would use a proper modal service
    // For now, this is a placeholder for the modal functionality
  }

  closeDetails() {
    this.selectedItem = null;
  }

  getAverageDownload(): number {
    if (!this.historyItems || this.historyItems.length === 0) return 0;
    const sum = this.historyItems.reduce((acc, item) => acc + item.downloadSpeedMbps, 0);
    return sum / this.historyItems.length;
  }

  getAverageUpload(): number {
    if (!this.historyItems || this.historyItems.length === 0) return 0;
    const sum = this.historyItems.reduce((acc, item) => acc + item.uploadSpeedMbps, 0);
    return sum / this.historyItems.length;
  }

  getAverageLatency(): number {
    if (!this.historyItems || this.historyItems.length === 0) return 0;
    const sum = this.historyItems.reduce((acc, item) => acc + item.latencyMs, 0);
    return sum / this.historyItems.length;
  }
}
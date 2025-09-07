import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { SpeedTestService, SpeedTestRequest, SpeedTestResponse } from '../../services/speed-test.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-speed-test',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="speed-test-container">
      <div class="row">
        <div class="col-lg-8 mx-auto">
          <div class="test-card">
            <h2 class="text-center mb-4">Internet Speed Test</h2>
            
            <!-- Test Configuration -->
            <div *ngIf="!isTestRunning && !testResult" class="test-config">
              <div class="row g-3 mb-4">
                <div class="col-md-6">
                  <label class="form-label">Test Type</label>
                  <select class="form-select" [(ngModel)]="testConfig.testType">
                    <option value="FULL">Full Test (Download + Upload + Latency)</option>
                    <option value="DOWNLOAD_ONLY">Download Only</option>
                    <option value="UPLOAD_ONLY">Upload Only</option>
                    <option value="LATENCY_ONLY">Latency Only</option>
                  </select>
                </div>
                
                <div class="col-md-6">
                  <label class="form-label">Duration (seconds)</label>
                  <select class="form-select" [(ngModel)]="testConfig.testDurationSeconds">
                    <option value="5">5 seconds</option>
                    <option value="10">10 seconds</option>
                    <option value="15">15 seconds</option>
                    <option value="30">30 seconds</option>
                  </select>
                </div>
                
                <div class="col-md-6">
                  <label class="form-label">Number of Runs</label>
                  <select class="form-select" [(ngModel)]="testConfig.numberOfRuns">
                    <option value="1">1 run</option>
                    <option value="3">3 runs</option>
                    <option value="5">5 runs</option>
                    <option value="10">10 runs</option>
                  </select>
                </div>
                
                <div class="col-md-6">
                  <label class="form-label">Concurrent Connections</label>
                  <select class="form-select" [(ngModel)]="testConfig.concurrentConnections">
                    <option value="1">1 connection</option>
                    <option value="2">2 connections</option>
                    <option value="4">4 connections</option>
                    <option value="8">8 connections</option>
                  </select>
                </div>
              </div>
              
              <div class="text-center">
                <button class="btn btn-primary btn-lg" (click)="startTest()" [disabled]="isStarting">
                  <span *ngIf="isStarting" class="spinner-border spinner-border-sm me-2"></span>
                  {{ isStarting ? 'Starting...' : 'Start Speed Test' }}
                </button>
              </div>
            </div>
            
            <!-- Test Progress -->
            <div *ngIf="isTestRunning" class="test-progress">
              <div class="progress-header text-center mb-3">
                <h4>{{ getCurrentPhaseText() }}</h4>
                <p class="text-muted">{{ testStatus?.progressPercentage }}% Complete</p>
              </div>
              
              <div class="progress mb-4" style="height: 20px;">
                <div class="progress-bar progress-bar-striped progress-bar-animated" 
                     [style.width.%]="testStatus?.progressPercentage || 0">
                </div>
              </div>
              
              <div class="current-metrics" *ngIf="testStatus">
                <div class="row text-center">
                  <div class="col-md-4" *ngIf="testStatus.downloadMetrics">
                    <div class="metric-box">
                      <h5>Download</h5>
                      <p class="metric-value">{{ testStatus.downloadMetrics.speedMbps | number:'1.1-1' }} Mbps</p>
                    </div>
                  </div>
                  
                  <div class="col-md-4" *ngIf="testStatus.uploadMetrics">
                    <div class="metric-box">
                      <h5>Upload</h5>
                      <p class="metric-value">{{ testStatus.uploadMetrics.speedMbps | number:'1.1-1' }} Mbps</p>
                    </div>
                  </div>
                  
                  <div class="col-md-4" *ngIf="testStatus.latencyMetrics">
                    <div class="metric-box">
                      <h5>Latency</h5>
                      <p class="metric-value">{{ testStatus.latencyMetrics.pingMs | number:'1.0-0' }} ms</p>
                    </div>
                  </div>
                </div>
              </div>
              
              <div class="text-center mt-4">
                <button class="btn btn-outline-danger" (click)="cancelTest()">Cancel Test</button>
              </div>
            </div>
            
            <!-- Test Results -->
            <div *ngIf="testResult && !isTestRunning" class="test-results">
              <div class="results-header text-center mb-4">
                <h3 class="text-success">Test Complete!</h3>
                <p class="text-muted">{{ testResult.testTimestamp | date:'medium' }}</p>
              </div>
              
              <div class="results-grid">
                <div class="row g-4">
                  <!-- Download Results -->
                  <div class="col-md-4" *ngIf="testResult.downloadMetrics">
                    <div class="result-card download">
                      <div class="result-icon">📥</div>
                      <h4>Download Speed</h4>
                      <div class="main-metric">{{ testResult.downloadMetrics.speedMbps | number:'1.1-1' }} Mbps</div>
                      <div class="sub-metrics">
                        <small>Peak: {{ testResult.downloadMetrics.peakSpeedMbps | number:'1.1-1' }} Mbps</small><br>
                        <small>Avg: {{ testResult.downloadMetrics.averageSpeedMbps | number:'1.1-1' }} Mbps</small><br>
                        <small>Stability: {{ testResult.downloadMetrics.stabilityScore | number:'1.0-0' }}%</small>
                      </div>
                    </div>
                  </div>
                  
                  <!-- Upload Results -->
                  <div class="col-md-4" *ngIf="testResult.uploadMetrics">
                    <div class="result-card upload">
                      <div class="result-icon">📤</div>
                      <h4>Upload Speed</h4>
                      <div class="main-metric">{{ testResult.uploadMetrics.speedMbps | number:'1.1-1' }} Mbps</div>
                      <div class="sub-metrics">
                        <small>Peak: {{ testResult.uploadMetrics.peakSpeedMbps | number:'1.1-1' }} Mbps</small><br>
                        <small>Avg: {{ testResult.uploadMetrics.averageSpeedMbps | number:'1.1-1' }} Mbps</small><br>
                        <small>Stability: {{ testResult.uploadMetrics.stabilityScore | number:'1.0-0' }}%</small>
                      </div>
                    </div>
                  </div>
                  
                  <!-- Latency Results -->
                  <div class="col-md-4" *ngIf="testResult.latencyMetrics">
                    <div class="result-card latency">
                      <div class="result-icon">⚡</div>
                      <h4>Latency</h4>
                      <div class="main-metric">{{ testResult.latencyMetrics.pingMs | number:'1.0-0' }} ms</div>
                      <div class="sub-metrics">
                        <small>Jitter: {{ testResult.latencyMetrics.jitterMs | number:'1.1-1' }} ms</small><br>
                        <small>Loss: {{ testResult.latencyMetrics.packetLossPercent | number:'1.1-1' }}%</small><br>
                        <small>DNS: {{ testResult.latencyMetrics.dnsLookupMs | number:'1.0-0' }} ms</small>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- Statistical Summary -->
              <div *ngIf="testResult.statisticalSummary" class="statistical-summary mt-4">
                <h5>Statistical Summary</h5>
                <div class="stats-table">
                  <div class="table-responsive">
                    <table class="table table-sm">
                      <thead>
                        <tr>
                          <th>Metric</th>
                          <th>Mean</th>
                          <th>Median</th>
                          <th>Min</th>
                          <th>Max</th>
                          <th>95th %</th>
                          <th>Std Dev</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr *ngIf="testResult.statisticalSummary.downloadStats">
                          <td>Download (Mbps)</td>
                          <td>{{ testResult.statisticalSummary.downloadStats.mean | number:'1.1-1' }}</td>
                          <td>{{ testResult.statisticalSummary.downloadStats.median | number:'1.1-1' }}</td>
                          <td>{{ testResult.statisticalSummary.downloadStats.min | number:'1.1-1' }}</td>
                          <td>{{ testResult.statisticalSummary.downloadStats.max | number:'1.1-1' }}</td>
                          <td>{{ testResult.statisticalSummary.downloadStats.percentile95 | number:'1.1-1' }}</td>
                          <td>{{ testResult.statisticalSummary.downloadStats.standardDeviation | number:'1.1-1' }}</td>
                        </tr>
                        <tr *ngIf="testResult.statisticalSummary.uploadStats">
                          <td>Upload (Mbps)</td>
                          <td>{{ testResult.statisticalSummary.uploadStats.mean | number:'1.1-1' }}</td>
                          <td>{{ testResult.statisticalSummary.uploadStats.median | number:'1.1-1' }}</td>
                          <td>{{ testResult.statisticalSummary.uploadStats.min | number:'1.1-1' }}</td>
                          <td>{{ testResult.statisticalSummary.uploadStats.max | number:'1.1-1' }}</td>
                          <td>{{ testResult.statisticalSummary.uploadStats.percentile95 | number:'1.1-1' }}</td>
                          <td>{{ testResult.statisticalSummary.uploadStats.standardDeviation | number:'1.1-1' }}</td>
                        </tr>
                        <tr *ngIf="testResult.statisticalSummary.latencyStats">
                          <td>Latency (ms)</td>
                          <td>{{ testResult.statisticalSummary.latencyStats.mean | number:'1.1-1' }}</td>
                          <td>{{ testResult.statisticalSummary.latencyStats.median | number:'1.1-1' }}</td>
                          <td>{{ testResult.statisticalSummary.latencyStats.min | number:'1.1-1' }}</td>
                          <td>{{ testResult.statisticalSummary.latencyStats.max | number:'1.1-1' }}</td>
                          <td>{{ testResult.statisticalSummary.latencyStats.percentile95 | number:'1.1-1' }}</td>
                          <td>{{ testResult.statisticalSummary.latencyStats.standardDeviation | number:'1.1-1' }}</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
              
              <div class="text-center mt-4">
                <button class="btn btn-primary me-2" (click)="runAnotherTest()">Run Another Test</button>
                <button class="btn btn-outline-primary" routerLink="/history">View History</button>
              </div>
            </div>
            
            <!-- Error State -->
            <div *ngIf="error" class="alert alert-danger">
              <h5>Test Failed</h5>
              <p>{{ error }}</p>
              <button class="btn btn-outline-danger" (click)="resetTest()">Try Again</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .speed-test-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 2rem 1rem;
    }
    
    .test-card {
      background: white;
      border-radius: 1rem;
      padding: 2rem;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }
    
    .metric-box {
      background: #f8f9fa;
      padding: 1rem;
      border-radius: 0.5rem;
      margin-bottom: 1rem;
    }
    
    .metric-value {
      font-size: 1.5rem;
      font-weight: bold;
      color: #007bff;
      margin: 0;
    }
    
    .result-card {
      background: #f8f9fa;
      border-radius: 0.75rem;
      padding: 1.5rem;
      text-align: center;
      height: 100%;
      border-left: 4px solid #007bff;
    }
    
    .result-card.download {
      border-left-color: #28a745;
    }
    
    .result-card.upload {
      border-left-color: #ffc107;
    }
    
    .result-card.latency {
      border-left-color: #dc3545;
    }
    
    .result-icon {
      font-size: 2rem;
      margin-bottom: 0.5rem;
    }
    
    .main-metric {
      font-size: 2rem;
      font-weight: bold;
      color: #333;
      margin: 0.5rem 0;
    }
    
    .sub-metrics {
      color: #666;
      font-size: 0.9rem;
    }
    
    .progress-header h4 {
      color: #007bff;
    }
    
    .stats-table {
      margin-top: 1rem;
    }
    
    .table th {
      border-top: none;
      font-size: 0.9rem;
    }
    
    .table td {
      font-size: 0.9rem;
    }
  `]
})
export class SpeedTestComponent implements OnDestroy {
  testConfig: SpeedTestRequest = {
    testType: 'FULL',
    testDurationSeconds: 10,
    numberOfRuns: 3,
    concurrentConnections: 4
  };

  isStarting = false;
  isTestRunning = false;
  testStatus: SpeedTestResponse | null = null;
  testResult: SpeedTestResponse | null = null;
  error: string | null = null;
  
  private pollSubscription?: Subscription;

  constructor(private speedTestService: SpeedTestService) {}

  ngOnDestroy() {
    if (this.pollSubscription) {
      this.pollSubscription.unsubscribe();
    }
  }

  startTest() {
    this.isStarting = true;
    this.error = null;
    
    this.speedTestService.startSpeedTest(this.testConfig).subscribe({
      next: (response) => {
        this.isStarting = false;
        this.isTestRunning = true;
        this.testStatus = response;
        this.pollForUpdates(response.sessionId);
      },
      error: (err) => {
        this.isStarting = false;
        this.error = err.error?.message || 'Failed to start speed test';
      }
    });
  }

  private pollForUpdates(sessionId: string) {
    this.pollSubscription = this.speedTestService.pollTestStatus(sessionId).subscribe({
      next: (response) => {
        this.testStatus = response;
        
        if (response.status === 'COMPLETED') {
          this.isTestRunning = false;
          this.testResult = response;
        } else if (response.status === 'FAILED') {
          this.isTestRunning = false;
          this.error = response.errorMessage || 'Test failed';
        }
      },
      error: (err) => {
        this.isTestRunning = false;
        this.error = err.error?.message || 'Failed to get test status';
      }
    });
  }

  getCurrentPhaseText(): string {
    if (!this.testStatus) return 'Initializing...';
    
    switch (this.testStatus.currentPhase) {
      case 'INITIALIZATION':
        return 'Initializing Test...';
      case 'LATENCY_TEST':
        return 'Testing Latency...';
      case 'DOWNLOAD_TEST':
        return 'Testing Download Speed...';
      case 'UPLOAD_TEST':
        return 'Testing Upload Speed...';
      case 'ANALYSIS':
        return 'Analyzing Results...';
      case 'COMPLETED':
        return 'Test Complete!';
      default:
        return 'Running Test...';
    }
  }

  cancelTest() {
    if (this.pollSubscription) {
      this.pollSubscription.unsubscribe();
    }
    this.isTestRunning = false;
    this.testStatus = null;
    this.error = 'Test cancelled by user';
  }

  runAnotherTest() {
    this.resetTest();
  }

  resetTest() {
    this.isStarting = false;
    this.isTestRunning = false;
    this.testStatus = null;
    this.testResult = null;
    this.error = null;
    
    if (this.pollSubscription) {
      this.pollSubscription.unsubscribe();
    }
  }
}
import { Component, OnDestroy, OnInit } from '@angular/core';
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
    <div class="speed-test-container fullscreen-container">
      <div class="row h-100">
        <div class="col-12 d-flex align-items-center justify-content-center">
          <div class="test-card fullscreen-card">
            <h1 class="text-center mb-5 display-3">Internet Speed Test</h1>

            <!-- Starting State -->
            <div *ngIf="!isTestRunning && !testResult && isStarting" class="starting-state text-center">
              <div class="spinner-border text-primary mb-3" style="width: 4rem; height: 4rem;"></div>
              <h3>Initializing Speed Test...</h3>
              <p class="lead">Test Configuration: 5 seconds per test, 1 run, 4 connections</p>
            </div>
            
            <!-- Test Progress -->
            <div *ngIf="isTestRunning" class="test-progress text-center">
              <div class="progress-header mb-5">
                <h2 class="display-4 mb-3">{{ getCurrentPhaseText() }}</h2>
                <p class="lead text-muted">Testing each metric for 5 seconds...</p>
              </div>

              <!-- Real-time Results Display -->
              <div class="current-metrics">
                <div class="row text-center justify-content-center">
                  <div class="col-lg-3 col-md-6 mb-4">
                    <div class="metric-box fullscreen-metric" [class.active]="testStatus?.currentPhase === 'DOWNLOAD_TEST'">
                      <h3>Download</h3>
                      <p class="metric-value">{{ getDisplayValue('download') }}</p>
                      <p class="metric-unit">Mbps</p>
                      <div class="phase-indicator" *ngIf="testStatus?.currentPhase === 'DOWNLOAD_TEST'">Testing...</div>
                    </div>
                  </div>

                  <div class="col-lg-3 col-md-6 mb-4">
                    <div class="metric-box fullscreen-metric" [class.active]="testStatus?.currentPhase === 'UPLOAD_TEST'">
                      <h3>Upload</h3>
                      <p class="metric-value">{{ getDisplayValue('upload') }}</p>
                      <p class="metric-unit">Mbps</p>
                      <div class="phase-indicator" *ngIf="testStatus?.currentPhase === 'UPLOAD_TEST'">Testing...</div>
                    </div>
                  </div>

                  <div class="col-lg-3 col-md-6 mb-4">
                    <div class="metric-box fullscreen-metric" [class.active]="testStatus?.currentPhase === 'LATENCY_TEST'">
                      <h3>Latency</h3>
                      <p class="metric-value">{{ getDisplayValue('latency') }}</p>
                      <p class="metric-unit">ms</p>
                      <div class="phase-indicator" *ngIf="testStatus?.currentPhase === 'LATENCY_TEST'">Testing...</div>
                    </div>
                  </div>
                </div>
              </div>

              <div class="mt-5">
                <button class="btn btn-outline-danger btn-lg" (click)="cancelTest()">Cancel Test</button>
              </div>
            </div>
            
            <!-- Test Results -->
            <div *ngIf="testResult && !isTestRunning" class="test-results text-center">
              <div class="results-header mb-5">
                <h2 class="display-3 text-success mb-4">Test Complete!</h2>
                <p class="lead">{{ testResult.testTimestamp | date:'medium' }}</p>
              </div>

              <div class="results-grid">
                <div class="row justify-content-center">
                  <!-- Download Results -->
                  <div class="col-lg-3 col-md-4 mb-4" *ngIf="testResult.downloadMetrics">
                    <div class="result-card download fullscreen-result">
                      <div class="result-icon">📥</div>
                      <h3>Download</h3>
                      <div class="main-metric">{{ testResult.downloadMetrics.speedMbps | number:'1.1-1' }}</div>
                      <div class="metric-unit">Mbps</div>
                      <div class="sub-metrics">
                        <small>Peak: {{ testResult.downloadMetrics.peakSpeedMbps | number:'1.1-1' }} Mbps</small><br>
                        <small>Stability: {{ testResult.downloadMetrics.stabilityScore | number:'1.0-0' }}%</small>
                      </div>
                    </div>
                  </div>

                  <!-- Upload Results -->
                  <div class="col-lg-3 col-md-4 mb-4" *ngIf="testResult.uploadMetrics">
                    <div class="result-card upload fullscreen-result">
                      <div class="result-icon">📤</div>
                      <h3>Upload</h3>
                      <div class="main-metric">{{ testResult.uploadMetrics.speedMbps | number:'1.1-1' }}</div>
                      <div class="metric-unit">Mbps</div>
                      <div class="sub-metrics">
                        <small>Peak: {{ testResult.uploadMetrics.peakSpeedMbps | number:'1.1-1' }} Mbps</small><br>
                        <small>Stability: {{ testResult.uploadMetrics.stabilityScore | number:'1.0-0' }}%</small>
                      </div>
                    </div>
                  </div>

                  <!-- Latency Results -->
                  <div class="col-lg-3 col-md-4 mb-4" *ngIf="testResult.latencyMetrics">
                    <div class="result-card latency fullscreen-result">
                      <div class="result-icon">⚡</div>
                      <h3>Latency</h3>
                      <div class="main-metric">{{ testResult.latencyMetrics.pingMs | number:'1.0-0' }}</div>
                      <div class="metric-unit">ms</div>
                      <div class="sub-metrics">
                        <small>Jitter: {{ testResult.latencyMetrics.jitterMs | number:'1.1-1' }} ms</small><br>
                        <small>Loss: {{ testResult.latencyMetrics.packetLossPercent | number:'1.1-1' }}%</small>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div class="mt-5">
                <button class="btn btn-primary btn-lg me-3" (click)="runAnotherTest()">Run Another Test</button>
                <button class="btn btn-outline-light btn-lg me-3" (click)="exitFullScreen()">Exit Full Screen</button>
                <button class="btn btn-outline-secondary btn-lg" routerLink="/history">View History</button>
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
      min-height: 100vh;
      padding: 0;
    }

    .fullscreen-container {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
    }

    .test-card {
      background: transparent;
      border: none;
      padding: 3rem;
      width: 100%;
      max-width: none;
    }

    .fullscreen-card {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      justify-content: center;
    }

    .metric-box {
      background: rgba(255, 255, 255, 0.1);
      backdrop-filter: blur(10px);
      border: 1px solid rgba(255, 255, 255, 0.2);
      padding: 2rem;
      border-radius: 1rem;
      color: white;
      transition: all 0.3s ease;
      position: relative;
    }

    .metric-box.active {
      background: rgba(255, 255, 255, 0.2);
      border: 2px solid rgba(255, 255, 255, 0.6);
      box-shadow: 0 0 20px rgba(255, 255, 255, 0.3);
      transform: scale(1.05);
    }

    .fullscreen-metric {
      padding: 2.5rem;
    }

    .metric-value {
      font-size: 4rem;
      font-weight: bold;
      color: #fff;
      margin: 0.5rem 0;
      text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
    }

    .metric-unit {
      font-size: 1.5rem;
      color: rgba(255, 255, 255, 0.8);
      margin: 0;
    }

    .result-card {
      background: rgba(255, 255, 255, 0.1);
      backdrop-filter: blur(10px);
      border: 1px solid rgba(255, 255, 255, 0.2);
      border-radius: 1rem;
      padding: 2rem;
      text-align: center;
      height: 100%;
      color: white;
    }

    .fullscreen-result {
      padding: 3rem 2rem;
    }

    .result-card.download {
      border-left: 4px solid #28a745;
    }

    .result-card.upload {
      border-left: 4px solid #ffc107;
    }

    .result-card.latency {
      border-left: 4px solid #dc3545;
    }

    .result-icon {
      font-size: 3rem;
      margin-bottom: 1rem;
    }

    .main-metric {
      font-size: 3rem;
      font-weight: bold;
      color: #fff;
      margin: 1rem 0 0.5rem 0;
      text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
    }

    .sub-metrics {
      color: rgba(255, 255, 255, 0.8);
      font-size: 1rem;
      margin-top: 1rem;
    }

    .progress {
      background-color: rgba(255, 255, 255, 0.2);
      border-radius: 1rem;
    }

    .progress-bar {
      background: linear-gradient(90deg, #28a745, #20c997, #17a2b8);
      border-radius: 1rem;
    }

    .progress-header h2 {
      color: #fff;
      text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
    }

    .starting-state h3 {
      color: #fff;
      text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
    }

    .results-header h2 {
      text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
    }

    .btn-lg {
      padding: 1rem 2rem;
      font-size: 1.2rem;
      border-radius: 0.75rem;
      backdrop-filter: blur(10px);
      border: 1px solid rgba(255, 255, 255, 0.3);
    }

    .btn-outline-danger {
      color: white;
      border-color: #dc3545;
      background: rgba(220, 53, 69, 0.2);
    }

    .btn-outline-danger:hover {
      background: rgba(220, 53, 69, 0.4);
      border-color: #dc3545;
      color: white;
    }

    .btn-outline-light {
      color: white;
      border-color: rgba(255, 255, 255, 0.5);
      background: rgba(255, 255, 255, 0.1);
    }

    .btn-outline-light:hover {
      background: rgba(255, 255, 255, 0.2);
      border-color: white;
      color: white;
    }

    .btn-outline-secondary {
      color: white;
      border-color: rgba(255, 255, 255, 0.3);
      background: rgba(108, 117, 125, 0.2);
    }

    .btn-outline-secondary:hover {
      background: rgba(108, 117, 125, 0.4);
      border-color: rgba(255, 255, 255, 0.5);
      color: white;
    }

    .btn-primary {
      background: rgba(0, 123, 255, 0.8);
      border-color: #007bff;
      backdrop-filter: blur(10px);
    }

    .btn-primary:hover {
      background: rgba(0, 123, 255, 1);
      border-color: #0056b3;
    }

    .display-3 {
      font-weight: 300;
      text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
    }

    .display-4 {
      font-weight: 300;
      text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
    }

    .lead {
      color: rgba(255, 255, 255, 0.9);
    }

    .text-muted {
      color: rgba(255, 255, 255, 0.7) !important;
    }

    .phase-indicator {
      position: absolute;
      bottom: 0.5rem;
      left: 50%;
      transform: translateX(-50%);
      background: rgba(0, 123, 255, 0.8);
      color: white;
      padding: 0.25rem 0.75rem;
      border-radius: 1rem;
      font-size: 0.8rem;
      font-weight: bold;
      animation: pulse 1.5s infinite;
    }

    @keyframes pulse {
      0% { opacity: 1; }
      50% { opacity: 0.6; }
      100% { opacity: 1; }
    }
  `]
})
export class SpeedTestComponent implements OnInit, OnDestroy {
  testConfig: SpeedTestRequest = {
    testType: 'FULL',
    testDurationSeconds: 5,
    numberOfRuns: 1,
    concurrentConnections: 4
  };

  isStarting = false;
  isTestRunning = false;
  testStatus: SpeedTestResponse | null = null;
  testResult: SpeedTestResponse | null = null;
  error: string | null = null;
  
  private pollSubscription?: Subscription;

  constructor(private speedTestService: SpeedTestService) {}

  ngOnInit() {
    this.enterFullScreen();
    setTimeout(() => {
      this.startTest();
    }, 1000);
  }

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

  enterFullScreen() {
    const elem = document.documentElement;
    if (elem.requestFullscreen) {
      elem.requestFullscreen().catch(err => {
        console.log('Could not enter fullscreen mode:', err);
      });
    }
  }

  exitFullScreen() {
    if (document.exitFullscreen && document.fullscreenElement) {
      document.exitFullscreen().catch(err => {
        console.log('Could not exit fullscreen mode:', err);
      });
    }
  }

  getDisplayValue(metric: string): string {
    if (!this.testStatus) return '--';

    switch (metric) {
      case 'download':
        return this.testStatus.downloadMetrics
          ? Math.round(this.testStatus.downloadMetrics.speedMbps).toString()
          : '--';
      case 'upload':
        return this.testStatus.uploadMetrics
          ? Math.round(this.testStatus.uploadMetrics.speedMbps).toString()
          : '--';
      case 'latency':
        return this.testStatus.latencyMetrics
          ? Math.round(this.testStatus.latencyMetrics.pingMs).toString()
          : '--';
      default:
        return '--';
    }
  }
}
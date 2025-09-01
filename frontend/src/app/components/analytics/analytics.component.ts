import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgxEchartsModule } from 'ngx-echarts';
import { EChartsOption } from 'echarts';
import { SpeedTestService, TimeSeriesData } from '../../services/speed-test.service';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule, NgxEchartsModule],
  template: `
    <div class="analytics-container">
      <div class="page-header mb-4">
        <h2>Network Performance Analytics</h2>
        <p class="text-muted">Analyze your network performance trends over time</p>
      </div>
      
      <!-- Date Range Selector -->
      <div class="controls-card mb-4">
        <div class="row g-3 align-items-end">
          <div class="col-md-3">
            <label class="form-label">Start Date</label>
            <input type="date" class="form-control" [(ngModel)]="startDate" (change)="loadData()">
          </div>
          <div class="col-md-3">
            <label class="form-label">End Date</label>
            <input type="date" class="form-control" [(ngModel)]="endDate" (change)="loadData()">
          </div>
          <div class="col-md-3">
            <label class="form-label">Time Period</label>
            <select class="form-select" [(ngModel)]="selectedPeriod" (change)="onPeriodChange()">
              <option value="7">Last 7 days</option>
              <option value="30">Last 30 days</option>
              <option value="90">Last 3 months</option>
              <option value="365">Last year</option>
              <option value="custom">Custom range</option>
            </select>
          </div>
          <div class="col-md-3">
            <button class="btn btn-primary w-100" (click)="loadData()" [disabled]="loading">
              <span *ngIf="loading" class="spinner-border spinner-border-sm me-2"></span>
              {{ loading ? 'Loading...' : 'Update Charts' }}
            </button>
          </div>
        </div>
      </div>
      
      <!-- Charts -->
      <div class="charts-container">
        <!-- Combined Speed Chart -->
        <div class="chart-card mb-4">
          <div class="chart-header">
            <h4>Speed Performance Over Time</h4>
            <p class="text-muted">Download and Upload speeds in Mbps</p>
          </div>
          <div class="chart-content" *ngIf="speedChartOptions">
            <div echarts [options]="speedChartOptions" [loading]="loading" class="chart"></div>
          </div>
          <div *ngIf="!timeSeriesData || timeSeriesData.downloadSeries.length === 0" class="no-data">
            <p>No speed test data available for the selected time range.</p>
          </div>
        </div>
        
        <!-- Latency Chart -->
        <div class="chart-card mb-4">
          <div class="chart-header">
            <h4>Latency Performance Over Time</h4>
            <p class="text-muted">Network latency in milliseconds</p>
          </div>
          <div class="chart-content" *ngIf="latencyChartOptions">
            <div echarts [options]="latencyChartOptions" [loading]="loading" class="chart"></div>
          </div>
          <div *ngIf="!timeSeriesData || timeSeriesData.latencySeries.length === 0" class="no-data">
            <p>No latency data available for the selected time range.</p>
          </div>
        </div>
        
        <!-- Performance Summary -->
        <div class="summary-card" *ngIf="performanceSummary">
          <h4>Performance Summary</h4>
          <div class="row g-3">
            <div class="col-md-4">
              <div class="summary-item download">
                <h5>Download Speed</h5>
                <div class="summary-stats">
                  <div class="stat">
                    <label>Average:</label>
                    <span>{{ performanceSummary.download.average | number:'1.1-1' }} Mbps</span>
                  </div>
                  <div class="stat">
                    <label>Best:</label>
                    <span>{{ performanceSummary.download.max | number:'1.1-1' }} Mbps</span>
                  </div>
                  <div class="stat">
                    <label>Worst:</label>
                    <span>{{ performanceSummary.download.min | number:'1.1-1' }} Mbps</span>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="col-md-4">
              <div class="summary-item upload">
                <h5>Upload Speed</h5>
                <div class="summary-stats">
                  <div class="stat">
                    <label>Average:</label>
                    <span>{{ performanceSummary.upload.average | number:'1.1-1' }} Mbps</span>
                  </div>
                  <div class="stat">
                    <label>Best:</label>
                    <span>{{ performanceSummary.upload.max | number:'1.1-1' }} Mbps</span>
                  </div>
                  <div class="stat">
                    <label>Worst:</label>
                    <span>{{ performanceSummary.upload.min | number:'1.1-1' }} Mbps</span>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="col-md-4">
              <div class="summary-item latency">
                <h5>Latency</h5>
                <div class="summary-stats">
                  <div class="stat">
                    <label>Average:</label>
                    <span>{{ performanceSummary.latency.average | number:'1.0-0' }} ms</span>
                  </div>
                  <div class="stat">
                    <label>Best:</label>
                    <span>{{ performanceSummary.latency.min | number:'1.0-0' }} ms</span>
                  </div>
                  <div class="stat">
                    <label>Worst:</label>
                    <span>{{ performanceSummary.latency.max | number:'1.0-0' }} ms</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .analytics-container {
      max-width: 1400px;
      margin: 0 auto;
      padding: 2rem 1rem;
    }
    
    .controls-card, .chart-card, .summary-card {
      background: white;
      border-radius: 0.75rem;
      padding: 1.5rem;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }
    
    .chart-header {
      margin-bottom: 1rem;
    }
    
    .chart {
      height: 400px;
      width: 100%;
    }
    
    .no-data {
      text-align: center;
      padding: 3rem;
      color: #6c757d;
    }
    
    .summary-item {
      background: #f8f9fa;
      border-radius: 0.5rem;
      padding: 1rem;
      height: 100%;
      border-left: 4px solid #007bff;
    }
    
    .summary-item.download {
      border-left-color: #28a745;
    }
    
    .summary-item.upload {
      border-left-color: #ffc107;
    }
    
    .summary-item.latency {
      border-left-color: #dc3545;
    }
    
    .summary-stats {
      margin-top: 0.5rem;
    }
    
    .stat {
      display: flex;
      justify-content: space-between;
      margin-bottom: 0.25rem;
    }
    
    .stat label {
      font-weight: 500;
      color: #666;
    }
    
    .stat span {
      font-weight: bold;
      color: #333;
    }
    
    .page-header h2 {
      color: #333;
      margin-bottom: 0.5rem;
    }
  `]
})
export class AnalyticsComponent implements OnInit {
  startDate: string = '';
  endDate: string = '';
  selectedPeriod: string = '30';
  loading = false;
  
  timeSeriesData: TimeSeriesData | null = null;
  speedChartOptions: EChartsOption | null = null;
  latencyChartOptions: EChartsOption | null = null;
  performanceSummary: any = null;

  constructor(private speedTestService: SpeedTestService) {}

  ngOnInit() {
    this.setDefaultDateRange();
    this.loadData();
  }

  setDefaultDateRange() {
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(endDate.getDate() - parseInt(this.selectedPeriod));
    
    this.endDate = endDate.toISOString().split('T')[0];
    this.startDate = startDate.toISOString().split('T')[0];
  }

  onPeriodChange() {
    if (this.selectedPeriod !== 'custom') {
      this.setDefaultDateRange();
      this.loadData();
    }
  }

  loadData() {
    if (!this.startDate || !this.endDate) return;
    
    this.loading = true;
    
    const startDateTime = new Date(this.startDate).toISOString();
    const endDateTime = new Date(this.endDate).toISOString();
    
    this.speedTestService.getTimeSeriesData(startDateTime, endDateTime).subscribe({
      next: (data) => {
        this.timeSeriesData = data;
        this.updateCharts();
        this.calculatePerformanceSummary();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading analytics data:', err);
        this.loading = false;
      }
    });
  }

  updateCharts() {
    if (!this.timeSeriesData) return;
    
    this.updateSpeedChart();
    this.updateLatencyChart();
  }

  updateSpeedChart() {
    if (!this.timeSeriesData) return;
    
    const downloadData = this.timeSeriesData.downloadSeries.map(point => [
      new Date(point.timestamp).getTime(),
      point.value
    ]);
    
    const uploadData = this.timeSeriesData.uploadSeries.map(point => [
      new Date(point.timestamp).getTime(),
      point.value
    ]);

    this.speedChartOptions = {
      title: {
        text: 'Internet Speed Over Time',
        left: 'center'
      },
      tooltip: {
        trigger: 'axis',
        formatter: (params: any) => {
          const date = new Date(params[0].value[0]).toLocaleString();
          let tooltip = `<div><strong>${date}</strong></div>`;
          params.forEach((param: any) => {
            tooltip += `<div style="color: ${param.color}">
              ${param.seriesName}: ${param.value[1].toFixed(1)} Mbps
            </div>`;
          });
          return tooltip;
        }
      },
      legend: {
        data: ['Download Speed', 'Upload Speed'],
        top: 30
      },
      xAxis: {
        type: 'time',
        name: 'Time',
        nameLocation: 'middle',
        nameGap: 30
      },
      yAxis: {
        type: 'value',
        name: 'Speed (Mbps)',
        nameLocation: 'middle',
        nameGap: 50
      },
      series: [
        {
          name: 'Download Speed',
          type: 'line',
          data: downloadData,
          smooth: true,
          lineStyle: {
            color: '#28a745',
            width: 2
          },
          itemStyle: {
            color: '#28a745'
          },
          areaStyle: {
            color: 'rgba(40, 167, 69, 0.1)'
          }
        },
        {
          name: 'Upload Speed',
          type: 'line',
          data: uploadData,
          smooth: true,
          lineStyle: {
            color: '#ffc107',
            width: 2
          },
          itemStyle: {
            color: '#ffc107'
          },
          areaStyle: {
            color: 'rgba(255, 193, 7, 0.1)'
          }
        }
      ],
      grid: {
        top: 70,
        left: 60,
        right: 30,
        bottom: 60
      },
      dataZoom: [
        {
          type: 'inside',
          xAxisIndex: 0
        },
        {
          type: 'slider',
          xAxisIndex: 0,
          bottom: 10
        }
      ]
    };
  }

  updateLatencyChart() {
    if (!this.timeSeriesData) return;
    
    const latencyData = this.timeSeriesData.latencySeries.map(point => [
      new Date(point.timestamp).getTime(),
      point.value
    ]);

    this.latencyChartOptions = {
      title: {
        text: 'Network Latency Over Time',
        left: 'center'
      },
      tooltip: {
        trigger: 'axis',
        formatter: (params: any) => {
          const date = new Date(params[0].value[0]).toLocaleString();
          return `<div><strong>${date}</strong></div>
                  <div style="color: ${params[0].color}">
                    Latency: ${params[0].value[1].toFixed(0)} ms
                  </div>`;
        }
      },
      xAxis: {
        type: 'time',
        name: 'Time',
        nameLocation: 'middle',
        nameGap: 30
      },
      yAxis: {
        type: 'value',
        name: 'Latency (ms)',
        nameLocation: 'middle',
        nameGap: 50
      },
      series: [
        {
          name: 'Latency',
          type: 'line',
          data: latencyData,
          smooth: true,
          lineStyle: {
            color: '#dc3545',
            width: 2
          },
          itemStyle: {
            color: '#dc3545'
          },
          areaStyle: {
            color: 'rgba(220, 53, 69, 0.1)'
          }
        }
      ],
      grid: {
        top: 50,
        left: 60,
        right: 30,
        bottom: 60
      },
      dataZoom: [
        {
          type: 'inside',
          xAxisIndex: 0
        },
        {
          type: 'slider',
          xAxisIndex: 0,
          bottom: 10
        }
      ]
    };
  }

  calculatePerformanceSummary() {
    if (!this.timeSeriesData) return;
    
    const downloadValues = this.timeSeriesData.downloadSeries.map(p => p.value);
    const uploadValues = this.timeSeriesData.uploadSeries.map(p => p.value);
    const latencyValues = this.timeSeriesData.latencySeries.map(p => p.value);
    
    this.performanceSummary = {
      download: this.calculateStats(downloadValues),
      upload: this.calculateStats(uploadValues),
      latency: this.calculateStats(latencyValues)
    };
  }

  private calculateStats(values: number[]) {
    if (values.length === 0) {
      return { average: 0, min: 0, max: 0 };
    }
    
    const sum = values.reduce((a, b) => a + b, 0);
    return {
      average: sum / values.length,
      min: Math.min(...values),
      max: Math.max(...values)
    };
  }
}
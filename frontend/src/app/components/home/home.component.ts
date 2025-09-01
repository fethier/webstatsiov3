import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="home-container">
      <div class="hero-section text-center mb-5">
        <h1 class="display-4 mb-3">Welcome to WebStats.io</h1>
        <p class="lead mb-4">
          Professional speed testing with comprehensive network analytics and statistical reporting.
        </p>
        <button class="btn btn-primary btn-lg" routerLink="/test">
          Start Speed Test
        </button>
      </div>

      <div class="features-grid">
        <div class="row g-4">
          <div class="col-md-4">
            <div class="feature-card">
              <div class="feature-icon">
                <i class="bi bi-speedometer2"></i>
              </div>
              <h4>Comprehensive Testing</h4>
              <p>Test download speed, upload speed, and latency with multiple measurement runs for accurate results.</p>
            </div>
          </div>
          
          <div class="col-md-4">
            <div class="feature-card">
              <div class="feature-icon">
                <i class="bi bi-graph-up"></i>
              </div>
              <h4>Statistical Analysis</h4>
              <p>Get detailed statistical insights including median, percentile reporting, and stability scores.</p>
            </div>
          </div>
          
          <div class="col-md-4">
            <div class="feature-card">
              <div class="feature-icon">
                <i class="bi bi-clock-history"></i>
              </div>
              <h4>Historical Data</h4>
              <p>Track your network performance over time with interactive charts and trend analysis.</p>
            </div>
          </div>
        </div>
      </div>

      <div class="cta-section text-center mt-5 pt-5">
        <h3 class="mb-3">Ready to analyze your network performance?</h3>
        <div class="d-flex justify-content-center gap-3">
          <button class="btn btn-primary" routerLink="/test">Run Speed Test</button>
          <button class="btn btn-outline-secondary" routerLink="/history">View History</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .home-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 2rem 1rem;
    }
    
    .hero-section {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      padding: 4rem 2rem;
      border-radius: 1rem;
      margin-bottom: 3rem;
    }
    
    .feature-card {
      background: white;
      padding: 2rem;
      border-radius: 0.75rem;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
      height: 100%;
      text-align: center;
      transition: transform 0.2s;
    }
    
    .feature-card:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 12px rgba(0, 0, 0, 0.15);
    }
    
    .feature-icon {
      font-size: 2.5rem;
      color: #007bff;
      margin-bottom: 1rem;
    }
    
    .cta-section {
      border-top: 1px solid #dee2e6;
    }
    
    .btn-lg {
      padding: 0.75rem 2rem;
      font-size: 1.1rem;
    }
  `]
})
export class HomeComponent {}
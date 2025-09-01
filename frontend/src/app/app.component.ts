import { Component } from '@angular/core';
import { RouterOutlet, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterModule, CommonModule],
  template: `
    <div class="app-container">
      <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
        <div class="container">
          <a class="navbar-brand" routerLink="/">
            <strong>WebStats.io</strong>
          </a>
          <div class="navbar-nav">
            <a class="nav-link" routerLink="/test" routerLinkActive="active">Speed Test</a>
            <a class="nav-link" routerLink="/history" routerLinkActive="active">History</a>
            <a class="nav-link" routerLink="/analytics" routerLinkActive="active">Analytics</a>
          </div>
        </div>
      </nav>
      
      <main class="container mt-4">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    .app-container {
      min-height: 100vh;
      background-color: #f8f9fa;
    }
    
    .navbar-brand {
      font-size: 1.5rem;
    }
    
    .nav-link.active {
      color: #fff !important;
      font-weight: bold;
    }
  `]
})
export class AppComponent {
  title = 'WebStats.io - Speed Test Application';
}
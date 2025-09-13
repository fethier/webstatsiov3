import { Routes } from '@angular/router';
import { SpeedTestComponent } from './components/speed-test/speed-test.component';
import { HistoryComponent } from './components/history/history.component';
import { AnalyticsComponent } from './components/analytics/analytics.component';
import { HomeComponent } from './components/home/home.component';

export const routes: Routes = [
  { path: '', component: SpeedTestComponent },
  { path: 'home', component: HomeComponent },
  { path: 'test', component: SpeedTestComponent },
  { path: 'history', component: HistoryComponent },
  { path: 'analytics', component: AnalyticsComponent },
  { path: '**', redirectTo: '' }
];
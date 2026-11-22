import { Component, computed, inject, signal } from '@angular/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DateRange } from '../../core/models/overview.model';
import { NavigationService } from '../../core/services/navigation.service';
import { OverviewMockService } from '../../core/services/overview-mock.service';
import { KpiCardComponent } from './components/kpi-card/kpi-card.component';
import { TrafficChartComponent } from './components/traffic-chart/traffic-chart.component';
import { TopApisTableComponent } from './components/top-apis-table/top-apis-table.component';
import { RecentActivityComponent } from './components/recent-activity/recent-activity.component';

@Component({
  selector: 'app-overview',
  imports: [
    MatTooltipModule,
    KpiCardComponent,
    TrafficChartComponent,
    TopApisTableComponent,
    RecentActivityComponent
  ],
  templateUrl: './overview.component.html',
  styleUrl: './overview.component.css'
})
export class OverviewComponent {
  private readonly navService = inject(NavigationService);
  private readonly overviewService = inject(OverviewMockService);

  readonly currentOrg = this.navService.currentOrganization;
  readonly selectedDateRange = signal<DateRange>('7d');
  readonly isRefreshing = signal<boolean>(false);

  readonly dateRanges: { id: DateRange; label: string }[] = [
    { id: '24h', label: 'Last 24 hours' },
    { id: '7d', label: 'Last 7 days' },
    { id: '30d', label: 'Last 30 days' }
  ];

  readonly greeting = computed<string>(() => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
  });

  readonly kpiMetrics = computed(() =>
    this.overviewService.getKpiMetrics(this.selectedDateRange())
  );

  readonly trafficData = computed(() =>
    this.overviewService.getTrafficData(this.selectedDateRange())
  );

  readonly topApis = computed(() =>
    this.overviewService.getTopApis()
  );

  readonly recentActivities = computed(() =>
    this.overviewService.getRecentActivities()
  );

  setDateRange(range: DateRange): void {
    this.selectedDateRange.set(range);
  }

  refreshDashboard(): void {
    if (this.isRefreshing()) return;
    this.isRefreshing.set(true);
    // Simulating quick interactive refresh
    setTimeout(() => {
      this.isRefreshing.set(false);
    }, 450);
  }
}

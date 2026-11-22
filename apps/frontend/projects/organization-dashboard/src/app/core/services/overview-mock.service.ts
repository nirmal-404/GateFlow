import { Injectable } from '@angular/core';
import { ActivityItem, DateRange, KpiMetric, TopApi, TrafficSeries } from '../models/overview.model';

@Injectable({
  providedIn: 'root'
})
export class OverviewMockService {
  getKpiMetrics(range: DateRange): KpiMetric[] {
    switch (range) {
      case '24h':
        return [
          {
            id: 'requests',
            title: 'Total API Requests',
            value: '142,850',
            change: '+8.4%',
            isPositive: true,
            periodLabel: 'vs. yesterday',
            icon: 'requests'
          },
          {
            id: 'success',
            title: 'Success Rate',
            value: '99.91%',
            change: '+0.2%',
            isPositive: true,
            periodLabel: 'vs. yesterday',
            icon: 'success'
          },
          {
            id: 'latency',
            title: 'Average Latency',
            value: '118 ms',
            change: '-12.4%',
            isPositive: true, // Lower latency is positive
            periodLabel: 'vs. yesterday',
            icon: 'latency'
          },
          {
            id: 'keys',
            title: 'Active API Keys',
            value: '24',
            change: '+1',
            isPositive: true,
            periodLabel: 'Active keys across applications',
            icon: 'keys',
            supportingText: 'Active keys across applications.'
          }
        ];

      case '30d':
        return [
          {
            id: 'requests',
            title: 'Total API Requests',
            value: '5,420,180',
            change: '+16.2%',
            isPositive: true,
            periodLabel: 'vs. previous 30 days',
            icon: 'requests'
          },
          {
            id: 'success',
            title: 'Success Rate',
            value: '99.76%',
            change: '+0.1%',
            isPositive: true,
            periodLabel: 'vs. previous 30 days',
            icon: 'success'
          },
          {
            id: 'latency',
            title: 'Average Latency',
            value: '132 ms',
            change: '-5.1%',
            isPositive: true,
            periodLabel: 'vs. previous 30 days',
            icon: 'latency'
          },
          {
            id: 'keys',
            title: 'Active API Keys',
            value: '24',
            change: '+4',
            isPositive: true,
            periodLabel: 'Active keys across applications',
            icon: 'keys',
            supportingText: 'Active keys across applications.'
          }
        ];

      case '7d':
      default:
        return [
          {
            id: 'requests',
            title: 'Total API Requests',
            value: '1,284,592',
            change: '+12.8%',
            isPositive: true,
            periodLabel: 'vs. last week',
            icon: 'requests'
          },
          {
            id: 'success',
            title: 'Success Rate',
            value: '99.82%',
            change: '+0.4%',
            isPositive: true,
            periodLabel: 'vs. last week',
            icon: 'success'
          },
          {
            id: 'latency',
            title: 'Average Latency',
            value: '124 ms',
            change: '-8.2%',
            isPositive: true, // Lower latency is positive
            periodLabel: 'vs. last week',
            icon: 'latency'
          },
          {
            id: 'keys',
            title: 'Active API Keys',
            value: '24',
            change: '+2',
            isPositive: true,
            periodLabel: 'Active keys across applications',
            icon: 'keys',
            supportingText: 'Active keys across applications.'
          }
        ];
    }
  }

  getTrafficData(range: DateRange): TrafficSeries {
    switch (range) {
      case '24h':
        return {
          categories: ['00:00', '03:00', '06:00', '09:00', '12:00', '15:00', '18:00', '21:00'],
          total: [12400, 8900, 11200, 24500, 29800, 26300, 18400, 11350],
          successful: [12380, 8890, 11180, 24450, 29720, 26230, 18360, 11330],
          failed: [20, 10, 20, 50, 80, 70, 40, 20]
        };

      case '30d':
        return {
          categories: ['Week 1', 'Week 2', 'Week 3', 'Week 4', 'Week 5'],
          total: [1120000, 1250000, 1340000, 1420000, 1290180],
          successful: [1117500, 1247000, 1336800, 1416600, 1287100],
          failed: [2500, 3000, 3200, 3400, 3080]
        };

      case '7d':
      default:
        return {
          categories: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
          total: [168000, 194500, 215400, 208900, 232100, 142300, 123392],
          successful: [167650, 194120, 215010, 208530, 231680, 142080, 123170],
          failed: [350, 380, 390, 370, 420, 220, 222]
        };
    }
  }

  getTopApis(): TopApi[] {
    return [
      {
        id: 'api-1',
        name: 'Process Payment Charges',
        path: '/v1/payments/charges',
        method: 'POST',
        requests: '482,910',
        requestsCount: 482910,
        successRate: 99.94,
        avgLatencyMs: 64
      },
      {
        id: 'api-2',
        name: 'Verify Customer Identity',
        path: '/v1/customers/verify',
        method: 'POST',
        requests: '312,420',
        requestsCount: 312420,
        successRate: 99.88,
        avgLatencyMs: 88
      },
      {
        id: 'api-3',
        name: 'Retrieve Invoices',
        path: '/v1/invoices',
        method: 'GET',
        requests: '245,180',
        requestsCount: 245180,
        successRate: 99.96,
        avgLatencyMs: 52
      },
      {
        id: 'api-4',
        name: 'Webhook Event Dispatch',
        path: '/v2/webhooks/dispatch',
        method: 'POST',
        requests: '138,400',
        requestsCount: 138400,
        successRate: 99.45,
        avgLatencyMs: 142
      },
      {
        id: 'api-5',
        name: 'List Active Subscriptions',
        path: '/v1/subscriptions',
        method: 'GET',
        requests: '105,682',
        requestsCount: 105682,
        successRate: 99.89,
        avgLatencyMs: 48
      }
    ];
  }

  getRecentActivities(): ActivityItem[] {
    return [
      {
        id: 'act-1',
        type: 'key_created',
        title: 'API key created',
        description: 'Production Server Key generated for Payment Microservice',
        timestamp: '14 minutes ago',
        user: 'Sarah Connor',
        badgeLabel: 'Created',
        badgeType: 'success'
      },
      {
        id: 'act-2',
        type: 'app_registered',
        title: 'Application registered',
        description: 'iOS Mobile Checkout v3.2 registered in staging & prod',
        timestamp: '1 hour ago',
        user: 'Alex Rivers',
        badgeLabel: 'Registered',
        badgeType: 'info'
      },
      {
        id: 'act-3',
        type: 'sub_activated',
        title: 'API subscription activated',
        description: 'Billing & Invoicing Webhook stream enabled',
        timestamp: '3 hours ago',
        user: 'Sarah Connor',
        badgeLabel: 'Activated',
        badgeType: 'info'
      },
      {
        id: 'act-4',
        type: 'key_revoked',
        title: 'API key revoked',
        description: 'Legacy Staging Sandbox Key revoked for security compliance',
        timestamp: 'Yesterday at 4:32 PM',
        user: 'David Chen',
        badgeLabel: 'Revoked',
        badgeType: 'danger'
      },
      {
        id: 'act-5',
        type: 'policy_updated',
        title: 'Rate limit adjusted',
        description: 'Tier-1 gateway burst threshold upgraded to 2,500 req/sec',
        timestamp: '2 days ago',
        user: 'Sarah Connor',
        badgeLabel: 'Updated',
        badgeType: 'warning'
      }
    ];
  }
}

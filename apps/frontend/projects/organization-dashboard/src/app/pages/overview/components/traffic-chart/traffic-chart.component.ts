import { Component, computed, input } from '@angular/core';
import { NgApexchartsModule } from 'ng-apexcharts';
import {
  ApexAxisChartSeries,
  ApexChart,
  ApexXAxis,
  ApexYAxis,
  ApexStroke,
  ApexTooltip,
  ApexDataLabels,
  ApexLegend,
  ApexGrid,
  ApexFill
} from 'apexcharts';
import { DateRange, TrafficSeries } from '../../../../core/models/overview.model';

@Component({
  selector: 'app-traffic-chart',
  imports: [NgApexchartsModule],
  templateUrl: './traffic-chart.component.html',
  styleUrl: './traffic-chart.component.css'
})
export class TrafficChartComponent {
  readonly trafficData = input.required<TrafficSeries>();
  readonly range = input<DateRange>('7d');

  readonly chartSeries = computed<ApexAxisChartSeries>(() => {
    const data = this.trafficData();
    return [
      {
        name: 'Total Requests',
        data: data.total
      },
      {
        name: 'Successful',
        data: data.successful
      },
      {
        name: 'Failed',
        data: data.failed
      }
    ];
  });

  readonly chartOptions = computed<ApexChart>(() => ({
    type: 'area',
    height: 320,
    fontFamily: 'Inter, -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif',
    toolbar: {
      show: false
    },
    zoom: {
      enabled: false
    },
    animations: {
      enabled: true,
      speed: 400
    }
  }));

  readonly colors: string[] = ['#3b82f6', '#10b981', '#ef4444'];

  readonly stroke: ApexStroke = {
    curve: 'smooth',
    width: [2.5, 2, 1.5]
  };

  readonly dataLabels: ApexDataLabels = {
    enabled: false
  };

  readonly fill: ApexFill = {
    type: 'gradient',
    gradient: {
      shadeIntensity: 1,
      opacityFrom: 0.22,
      opacityTo: 0.02,
      stops: [0, 90, 100]
    }
  };

  readonly grid: ApexGrid = {
    borderColor: '#f1f5f9',
    strokeDashArray: 3,
    padding: {
      top: 0,
      right: 12,
      bottom: 0,
      left: 12
    }
  };

  readonly xaxis = computed<ApexXAxis>(() => ({
    categories: this.trafficData().categories,
    axisBorder: { show: false },
    axisTicks: { show: false },
    labels: {
      style: {
        colors: '#94a3b8',
        fontSize: '11px',
        fontWeight: 500
      }
    }
  }));

  readonly yaxis: ApexYAxis = {
    labels: {
      style: {
        colors: '#94a3b8',
        fontSize: '11px',
        fontWeight: 500
      },
      formatter: (value: number) => {
        if (value >= 1000000) {
          return (value / 1000000).toFixed(1) + 'M';
        }
        if (value >= 1000) {
          return (value / 1000).toFixed(0) + 'k';
        }
        return value.toString();
      }
    }
  };

  readonly tooltip: ApexTooltip = {
    theme: 'light',
    shared: true,
    intersect: false,
    y: {
      formatter: (val: number) => val.toLocaleString() + ' reqs'
    }
  };

  readonly legend: ApexLegend = {
    position: 'top',
    horizontalAlign: 'right',
    fontSize: '12px',
    fontWeight: 500,
    fontFamily: 'Inter, sans-serif',
    labels: {
      colors: '#64748b'
    },
    markers: {
      size: 6,
      strokeWidth: 0,
      shape: 'circle'
    },
    itemMargin: {
      horizontal: 10,
      vertical: 0
    }
  };

  readonly totalPeriodRequests = computed<string>(() => {
    const totalSum = this.trafficData().total.reduce((acc, curr) => acc + curr, 0);
    return totalSum.toLocaleString();
  });

  readonly errorRate = computed<string>(() => {
    const totalSum = this.trafficData().total.reduce((acc, curr) => acc + curr, 0);
    const failedSum = this.trafficData().failed.reduce((acc, curr) => acc + curr, 0);
    if (totalSum === 0) return '0.00%';
    return ((failedSum / totalSum) * 100).toFixed(2) + '%';
  });
}

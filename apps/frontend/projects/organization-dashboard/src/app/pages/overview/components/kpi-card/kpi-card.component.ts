import { Component, input } from '@angular/core';
import { KpiMetric } from '../../../../core/models/overview.model';

@Component({
  selector: 'app-kpi-card',
  imports: [],
  templateUrl: './kpi-card.component.html',
  styleUrl: './kpi-card.component.css'
})
export class KpiCardComponent {
  readonly metric = input.required<KpiMetric>();
}

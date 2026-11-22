import { Component, input } from '@angular/core';
import { ActivityItem } from '../../../../core/models/overview.model';

@Component({
  selector: 'app-recent-activity',
  imports: [],
  templateUrl: './recent-activity.component.html',
  styleUrl: './recent-activity.component.css'
})
export class RecentActivityComponent {
  readonly activities = input.required<ActivityItem[]>();
}

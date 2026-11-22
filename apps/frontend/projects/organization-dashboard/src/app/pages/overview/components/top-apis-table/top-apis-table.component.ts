import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TopApi } from '../../../../core/models/overview.model';

@Component({
  selector: 'app-top-apis-table',
  imports: [RouterLink],
  templateUrl: './top-apis-table.component.html',
  styleUrl: './top-apis-table.component.css'
})
export class TopApisTableComponent {
  readonly apis = input.required<TopApi[]>();
}

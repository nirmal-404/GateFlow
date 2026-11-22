import { Component, input } from '@angular/core';

@Component({
  selector: 'app-placeholder',
  imports: [],
  templateUrl: './placeholder.component.html',
  styleUrl: './placeholder.component.css'
})
export class PlaceholderComponent {
  readonly title = input<string>('Feature Section');
  readonly description = input<string>('This section will be populated in upcoming milestones.');
}

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ActivityLogEntry } from '../models/activity.model';

@Injectable({ providedIn: 'root' })
export class ActivityService {
  private readonly http = inject(HttpClient);
  private readonly projectsUrl = `${environment.apiBaseUrl}/projects`;

  listForProject(projectId: number): Observable<ActivityLogEntry[]> {
    return this.http.get<ActivityLogEntry[]>(`${this.projectsUrl}/${projectId}/activity`);
  }
}

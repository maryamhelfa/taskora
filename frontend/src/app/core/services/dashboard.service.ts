import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Dashboard } from '../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/dashboard`;

  getDashboard(teamId?: number): Observable<Dashboard> {
    const params = teamId != null ? new HttpParams().set('teamId', teamId) : undefined;
    return this.http.get<Dashboard>(this.baseUrl, { params });
  }
}

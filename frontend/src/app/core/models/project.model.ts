export type ProjectStatus = 'ACTIVE' | 'ARCHIVED';

export interface Project {
  id: number;
  teamId: number;
  name: string;
  description: string | null;
  status: ProjectStatus;
  createdAt: string;
}

export interface CreateProjectRequest {
  teamId: number;
  name: string;
  description?: string | null;
}

export interface UpdateProjectRequest {
  name: string;
  description?: string | null;
  status: ProjectStatus;
}

export interface AiSummaryResponse {
  summary: string;
  generatedAt: string;
}

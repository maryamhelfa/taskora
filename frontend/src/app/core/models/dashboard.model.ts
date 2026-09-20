import { ProjectStatus } from './project.model';

export interface ProjectDashboard {
  projectId: number;
  projectName: string;
  teamId: number;
  status: ProjectStatus;
  todoCount: number;
  inProgressCount: number;
  doneCount: number;
  overdueCount: number;
}

export interface Dashboard {
  projects: ProjectDashboard[];
  totalTasks: number;
  totalOverdueTasks: number;
}

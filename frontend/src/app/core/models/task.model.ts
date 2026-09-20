export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface Subtask {
  id: number;
  taskId: number;
  title: string;
  done: boolean;
}

export interface TaskComment {
  id: number;
  taskId: number;
  authorId: number;
  authorEmail: string;
  authorFirstName: string;
  authorLastName: string;
  content: string;
  createdAt: string;
}

export interface Task {
  id: number;
  projectId: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  assigneeId: number | null;
  assigneeEmail: string | null;
  assigneeFirstName: string | null;
  assigneeLastName: string | null;
  dueDate: string | null;
  createdAt: string;
  updatedAt: string;
  subtasks: Subtask[];
  comments: TaskComment[];
}

export interface CreateTaskRequest {
  projectId: number;
  title: string;
  description?: string | null;
  priority?: TaskPriority | null;
  assigneeId?: number | null;
  dueDate?: string | null;
}

export interface UpdateTaskStatusRequest {
  status: TaskStatus;
}

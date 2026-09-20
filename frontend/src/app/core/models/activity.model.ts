export type ActivityActionType =
  | 'PROJECT_CREATED'
  | 'PROJECT_UPDATED'
  | 'TASK_CREATED'
  | 'TASK_UPDATED'
  | 'TASK_STATUS_CHANGED'
  | 'TASK_DELETED'
  | 'COMMENT_ADDED';

export interface ActivityLogEntry {
  id: number;
  projectId: number | null;
  taskId: number | null;
  userId: number;
  userFirstName: string;
  userLastName: string;
  actionType: ActivityActionType;
  description: string;
  createdAt: string;
}

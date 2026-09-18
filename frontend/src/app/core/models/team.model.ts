export type TeamRole = 'ADMIN' | 'MEMBER';

export interface Team {
  id: number;
  name: string;
  ownerId: number;
  createdAt: string;
}

export interface TeamMember {
  id: number;
  teamId: number;
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  role: TeamRole;
  createdAt: string;
}

export interface CreateTeamRequest {
  name: string;
}

export interface AddTeamMemberRequest {
  email: string;
  role?: TeamRole | null;
}

export interface AuthUser {
  email: string;
  role: 'ADMIN' | 'VIEWER';
  token: string;
}

export interface Technology {
  id: number;
  name: string;
}

export interface DeveloperRole {
  id: number;
  name: string;
}

export interface Tag {
  id: number;
  name: string;
  slug: string;
}

export interface AllocationDto {
  id: number;
  personId: number;
  personName: string;
  squadId: number;
  squadName: string;
  roles: string[];
  technologies: string[];
  allocationPercent: number;
  publicComment?: string;
  adminNote?: string;
  startDate?: string;
  endDate?: string;
  active: boolean;
}

export interface PersonDto {
  id: number;
  name: string;
  email?: string;
  active: boolean;
  tags: string[];
  adminNote?: string;
  totalAllocation: number;
  allocations: AllocationDto[];
}

export interface SquadDto {
  id: number;
  name: string;
  description?: string;
  allocations: AllocationDto[];
  totalHeadcount: number;
  totalAllocationPercent: number;
}

export interface CreateAllocationRequest {
  personId: number;
  squadId: number;
  roleIds: number[];
  technologyIds: number[];
  allocationPercent: number;
  publicComment?: string;
  adminNote?: string;
  startDate?: string;
  endDate?: string;
}

export interface UserDto {
  id: number;
  email: string;
  role: 'ADMIN' | 'VIEWER';
  active: boolean;
}

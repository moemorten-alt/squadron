import type { AllocationDto, CreateAllocationRequest, DeveloperRole, PersonDto, SquadDto, Tag, Technology, UserDto } from '@/types';

const BASE = '/api';

function getToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('squadron_token');
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const token = getToken();
  const res = await fetch(`${BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
  });

  if (res.status === 401) {
    window.dispatchEvent(new CustomEvent('auth:expired'));
    throw new Error('Session expired. Please sign in again.');
  }
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.error ?? `${res.status} ${res.statusText}`);
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

const json = (data: unknown) => JSON.stringify(data);

export const api = {
  auth: {
    login: (email: string, password: string) =>
      request<{ token: string; email: string; role: string }>('/auth/login', {
        method: 'POST',
        body: json({ email, password }),
      }),
  },

  persons: {
    list: () => request<PersonDto[]>('/persons'),
    get:  (id: number) => request<PersonDto>(`/persons/${id}`),
    create: (data: { name: string; email?: string; adminNote?: string; tagIds?: number[] }) =>
      request<PersonDto>('/persons', { method: 'POST', body: json(data) }),
    update: (id: number, data: { name: string; email?: string; adminNote?: string; tagIds?: number[] }) =>
      request<PersonDto>(`/persons/${id}`, { method: 'PUT', body: json(data) }),
    deactivate: (id: number) =>
      request<void>(`/persons/${id}`, { method: 'DELETE' }),
  },

  squads: {
    list: () => request<SquadDto[]>('/squads'),
    get:  (id: number) => request<SquadDto>(`/squads/${id}`),
    create: (data: { name: string; description?: string }) =>
      request<SquadDto>('/squads', { method: 'POST', body: json(data) }),
    update: (id: number, data: { name: string; description?: string }) =>
      request<SquadDto>(`/squads/${id}`, { method: 'PUT', body: json(data) }),
    delete: (id: number) =>
      request<void>(`/squads/${id}`, { method: 'DELETE' }),
  },

  allocations: {
    list: () => request<AllocationDto[]>('/allocations'),
    create: (data: CreateAllocationRequest) =>
      request<AllocationDto>('/allocations', { method: 'POST', body: json(data) }),
    update: (id: number, data: CreateAllocationRequest) =>
      request<AllocationDto>(`/allocations/${id}`, { method: 'PUT', body: json(data) }),
    delete: (id: number) =>
      request<void>(`/allocations/${id}`, { method: 'DELETE' }),
  },

  lookup: {
    technologies: () => request<Technology[]>('/lookup/technologies'),
    roles:        () => request<DeveloperRole[]>('/lookup/roles'),
    tags:         () => request<Tag[]>('/lookup/tags'),
  },

  users: {
    list: () => request<UserDto[]>('/users'),
    create: (data: { email: string; password: string; role: string }) =>
      request<UserDto>('/users', { method: 'POST', body: json(data) }),
    update: (id: number, data: { password?: string; role?: string; active?: boolean }) =>
      request<UserDto>(`/users/${id}`, { method: 'PUT', body: json(data) }),
    delete: (id: number) =>
      request<void>(`/users/${id}`, { method: 'DELETE' }),
  },
};

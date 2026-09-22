// Typed API wrapper for the Core CRM backend.

export type Gender = 'FEMALE' | 'MALE' | 'OTHER' | 'UNSPECIFIED';
export type PreferredChannel = 'SMS' | 'WHATSAPP' | 'EMAIL' | 'NONE';
export type ClientStatus = 'ACTIVE' | 'ARCHIVED';
export type Segment = 'NEW' | 'AT_RISK' | 'LAPSED' | 'VIP' | 'BIRTHDAY_THIS_MONTH';

export interface ClientDto {
  id: string;
  firstName: string;
  lastName?: string;
  phone: string;
  email?: string;
  dateOfBirth?: string;
  gender?: Gender;
  allergies?: string;
  notes?: string;
  preferences?: string;
  tags: string[];
  smsOptIn: boolean;
  whatsappOptIn: boolean;
  emailOptIn: boolean;
  marketingConsentAt?: string;
  preferredChannel: PreferredChannel;
  status: ClientStatus;
  lastVisitDate?: string;
  visitCount: number;
  totalSpend: number;
  segments: string[];
  createdAt: string;
  updatedAt: string;
}

export interface ClientRequest {
  firstName: string;
  lastName?: string;
  phone: string;
  email?: string;
  dateOfBirth?: string;
  gender?: Gender;
  allergies?: string;
  notes?: string;
  preferences?: string;
  tags?: string[];
  smsOptIn?: boolean;
  whatsappOptIn?: boolean;
  emailOptIn?: boolean;
  marketingConsentAt?: string;
  preferredChannel?: PreferredChannel;
}

export interface ConsentRequest {
  smsOptIn: boolean;
  whatsappOptIn: boolean;
  emailOptIn: boolean;
  marketingConsentAt?: string | null;
  preferredChannel?: PreferredChannel;
}

export interface VisitDto {
  id: string;
  clientId: string;
  visitDate: string;
  stylistName?: string;
  services: string[];
  products: string[];
  amount?: number;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface VisitRequest {
  visitDate: string;
  stylistName?: string;
  services?: string[];
  products?: string[];
  amount?: number;
  notes?: string;
}

export interface FormulaCardDto {
  id: string;
  clientId: string;
  serviceName: string;
  formula?: string;
  notes?: string;
  recordedBy?: string;
  recordedAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface FormulaCardRequest {
  serviceName: string;
  formula?: string;
  notes?: string;
  recordedBy?: string;
  recordedAt: string;
}

export interface TimelineEntry {
  type: 'VISIT' | 'FORMULA';
  id: string;
  date: string;
  title: string;
  subtitle?: string;
  amount?: number;
  items: string[];
  notes?: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface SegmentCount {
  segment: string;
  count: number;
}

export interface TagCount {
  tag: string;
  count: number;
}

export interface ImportResult {
  imported: number;
  skipped: number;
  errors: { row: number; message: string }[];
}

const BASE = '/api/v1';

export class ApiError extends Error {
  status: number;
  detail: string;
  title?: string;

  constructor(status: number, detail: string, title?: string) {
    super(detail || title || `HTTP ${status}`);
    this.status = status;
    this.detail = detail;
    this.title = title;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: init?.body ? { 'Content-Type': 'application/json' } : undefined,
    ...init,
  });
  if (!res.ok) {
    let detail = res.statusText;
    let title: string | undefined;
    try {
      const problem = await res.json();
      detail = problem.detail ?? problem.title ?? detail;
      title = problem.title;
    } catch {
      // not a ProblemDetail body
    }
    throw new ApiError(res.status, detail, title);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export interface ClientListParams {
  search?: string;
  tag?: string;
  segment?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export const api = {
  listClients(params: ClientListParams = {}): Promise<Page<ClientDto>> {
    const q = new URLSearchParams();
    if (params.search) q.set('search', params.search);
    if (params.tag) q.set('tag', params.tag);
    if (params.segment) q.set('segment', params.segment);
    q.set('page', String(params.page ?? 0));
    q.set('size', String(params.size ?? 20));
    if (params.sort) q.set('sort', params.sort);
    return request(`/clients?${q}`);
  },
  getClient: (id: string) => request<ClientDto>(`/clients/${id}`),
  createClient: (body: ClientRequest) =>
    request<ClientDto>('/clients', { method: 'POST', body: JSON.stringify(body) }),
  updateClient: (id: string, body: ClientRequest) =>
    request<ClientDto>(`/clients/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteClient: (id: string) => request<void>(`/clients/${id}`, { method: 'DELETE' }),
  updateTags: (id: string, tags: string[]) =>
    request<ClientDto>(`/clients/${id}/tags`, {
      method: 'PUT',
      body: JSON.stringify({ tags }),
    }),
  updateConsent: (id: string, body: ConsentRequest) =>
    request<ClientDto>(`/clients/${id}/consent`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  listVisits: (id: string) => request<VisitDto[]>(`/clients/${id}/visits`),
  addVisit: (id: string, body: VisitRequest) =>
    request<VisitDto>(`/clients/${id}/visits`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  deleteVisit: (id: string, visitId: string) =>
    request<void>(`/clients/${id}/visits/${visitId}`, { method: 'DELETE' }),
  listFormulas: (id: string) => request<FormulaCardDto[]>(`/clients/${id}/formulas`),
  addFormula: (id: string, body: FormulaCardRequest) =>
    request<FormulaCardDto>(`/clients/${id}/formulas`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  updateFormula: (id: string, formulaId: string, body: FormulaCardRequest) =>
    request<FormulaCardDto>(`/clients/${id}/formulas/${formulaId}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  deleteFormula: (id: string, formulaId: string) =>
    request<void>(`/clients/${id}/formulas/${formulaId}`, { method: 'DELETE' }),
  timeline: (id: string) => request<TimelineEntry[]>(`/clients/${id}/timeline`),
  segments: () => request<SegmentCount[]>('/segments'),
  tags: () => request<TagCount[]>('/tags'),
  async importCsv(file: File): Promise<ImportResult> {
    const form = new FormData();
    form.append('file', file);
    const res = await fetch(`${BASE}/clients/import`, { method: 'POST', body: form });
    if (!res.ok) {
      const problem = await res.json().catch(() => ({}));
      throw new ApiError(res.status, problem.detail ?? res.statusText, problem.title);
    }
    return res.json();
  },
  templateUrl: `${BASE}/clients/import/template`,
};

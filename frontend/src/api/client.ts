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

export type StaffRole = 'STYLIST' | 'THERAPIST' | 'RECEPTIONIST' | 'MANAGER';
export type AppointmentStatus = 'BOOKED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';
export type AppointmentSource = 'DESK' | 'ONLINE';

export interface StaffDto {
  id: string;
  name: string;
  role: StaffRole;
  phone?: string;
  colorHex?: string;
  active: boolean;
  workingHours: { dayOfWeek: string; startTime: string; endTime: string }[];
}

export interface StaffRequest {
  name: string;
  role?: StaffRole;
  phone?: string;
  colorHex?: string;
  active?: boolean;
}

export interface ServiceItemDto {
  id: string;
  name: string;
  category?: string;
  durationMinutes: number;
  processingMinutes: number;
  price: number;
  active: boolean;
}

export interface ServiceItemRequest {
  name: string;
  category?: string;
  durationMinutes: number;
  processingMinutes?: number;
  price: number;
  active?: boolean;
}

export interface AppointmentDto {
  id: string;
  clientId: string;
  clientName: string;
  clientPhone: string;
  staffId: string;
  staffName: string;
  colorHex?: string;
  startTime: string;
  endTime: string;
  services: { id: string; name: string; durationMinutes: number; price: number }[];
  status: AppointmentStatus;
  source: AppointmentSource;
  notes?: string;
  totalPrice: number;
}

export interface AppointmentRequest {
  clientId: string;
  staffId: string;
  startTime: string;
  serviceIds: string[];
  source?: AppointmentSource;
  notes?: string;
}

export interface AvailabilitySlot {
  start: string;
  end: string;
}

// Virtual Room (walk-in queue over WhatsApp)

export type QueueTicketStatus =
  | 'WAITING'
  | 'CALLED'
  | 'IN_SERVICE'
  | 'COMPLETED'
  | 'SKIPPED'
  | 'CANCELLED'
  | 'EXPIRED';
export type PaymentStatus = 'NONE' | 'PENDING' | 'PAID';
export type PaymentMode = 'UPI_LINK' | 'UPI_MANUAL' | 'CASH' | 'CARD' | 'OTHER';

export interface QueueTicketDto {
  id: string;
  tokenNumber: number;
  queueDate: string;
  status: QueueTicketStatus;
  clientId: string;
  clientName: string;
  clientPhone: string;
  staffId?: string;
  staffName?: string;
  services: { id: string; name: string; durationMinutes: number; price: number }[];
  position?: number;
  etaMinutes?: number;
  peopleAhead?: number;
  skipCount: number;
  source?: string;
  joinedAt?: string;
  calledAt?: string;
  startedAt?: string;
  finishedAt?: string;
  amount?: number;
  paymentStatus: PaymentStatus;
  paymentMode?: PaymentMode;
  paymentLink?: string;
  paidAt?: string;
  rating?: number;
  reviewComment?: string;
  publicUrl?: string;
}

export interface QueueBoardDto {
  waiting: QueueTicketDto[];
  inService: QueueTicketDto[];
  completed: QueueTicketDto[];
  activeStaff: number;
  asOf: string;
}

export interface OutboundMessageDto {
  id: string;
  toPhone: string;
  body: string;
  channel: string;
  status: string;
  providerMessageId?: string;
  error?: string;
  ticketId?: string;
  createdAt: string;
}

export interface JoinQueueRequest {
  phone: string;
  name?: string;
  serviceIds: string[];
  staffId?: string;
  source?: string;
}

export interface SimulateResponse {
  reply: string;
  messages: OutboundMessageDto[];
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

  // Scheduling
  listStaff: (active?: boolean) =>
    request<StaffDto[]>(`/staff${active !== undefined ? `?active=${active}` : ''}`),
  createStaff: (body: StaffRequest) =>
    request<StaffDto>('/staff', { method: 'POST', body: JSON.stringify(body) }),
  updateStaff: (id: string, body: StaffRequest) =>
    request<StaffDto>(`/staff/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  setWorkingHours: (
    id: string,
    hours: { dayOfWeek: string; startTime: string; endTime: string }[],
  ) =>
    request<StaffDto>(`/staff/${id}/working-hours`, {
      method: 'PUT',
      body: JSON.stringify({ hours }),
    }),
  listServices: (active?: boolean) =>
    request<ServiceItemDto[]>(
      `/services${active !== undefined ? `?active=${active}` : ''}`,
    ),
  createService: (body: ServiceItemRequest) =>
    request<ServiceItemDto>('/services', { method: 'POST', body: JSON.stringify(body) }),
  updateService: (id: string, body: ServiceItemRequest) =>
    request<ServiceItemDto>(`/services/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  listAppointments: (params: {
    from: string;
    to: string;
    staffId?: string;
    status?: AppointmentStatus;
    clientId?: string;
  }) => {
    const q = new URLSearchParams({ from: params.from, to: params.to });
    if (params.staffId) q.set('staffId', params.staffId);
    if (params.status) q.set('status', params.status);
    if (params.clientId) q.set('clientId', params.clientId);
    return request<AppointmentDto[]>(`/appointments?${q}`);
  },
  createAppointment: (body: AppointmentRequest) =>
    request<AppointmentDto>('/appointments', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  updateAppointment: (id: string, body: AppointmentRequest) =>
    request<AppointmentDto>(`/appointments/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  setAppointmentStatus: (id: string, status: AppointmentStatus) =>
    request<AppointmentDto>(`/appointments/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    }),
  availability: (staffId: string, date: string, serviceIds: string[]) =>
    request<{ slots: AvailabilitySlot[] }>(
      `/appointments/availability?staffId=${staffId}&date=${date}&serviceIds=${serviceIds.join(',')}`,
    ),

  // Virtual Room / queue
  queueBoard: () => request<QueueBoardDto>('/queue/board'),
  listQueue: (status?: QueueTicketStatus[]) =>
    request<QueueTicketDto[]>(
      `/queue${status?.length ? `?status=${status.join(',')}` : ''}`,
    ),
  joinQueue: (body: JoinQueueRequest) =>
    request<QueueTicketDto>('/queue', { method: 'POST', body: JSON.stringify(body) }),
  getTicket: (id: string) => request<QueueTicketDto>(`/queue/${id}`),
  getTicketByPhone: (phone: string) =>
    request<QueueTicketDto>(`/queue/by-phone/${phone}`),
  publicTicket: (id: string) => request<QueueTicketDto>(`/public/queue/${id}`),
  callTicket: (id: string) =>
    request<QueueTicketDto>(`/queue/${id}/call`, { method: 'POST' }),
  startTicket: (id: string) =>
    request<QueueTicketDto>(`/queue/${id}/start`, { method: 'POST' }),
  finishTicket: (id: string) =>
    request<QueueTicketDto>(`/queue/${id}/finish`, { method: 'POST' }),
  skipTicket: (id: string) =>
    request<QueueTicketDto>(`/queue/${id}/skip`, { method: 'POST' }),
  cancelTicket: (id: string, reason?: string) =>
    request<QueueTicketDto>(`/queue/${id}/cancel`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    }),
  leaveTicket: (id: string) =>
    request<QueueTicketDto>(`/queue/${id}/leave`, { method: 'POST' }),
  publicLeaveTicket: (id: string) =>
    request<QueueTicketDto>(`/public/queue/${id}/leave`, { method: 'POST' }),
  markTicketPaid: (id: string, mode: PaymentMode, reference?: string) =>
    request<QueueTicketDto>(`/queue/${id}/payments/mark-paid`, {
      method: 'POST',
      body: JSON.stringify({ mode, reference }),
    }),
  submitReview: (id: string, rating: number, comment?: string) =>
    request<QueueTicketDto>(`/queue/${id}/review`, {
      method: 'POST',
      body: JSON.stringify({ rating, comment }),
    }),
  publicSubmitReview: (id: string, rating: number, comment?: string) =>
    request<QueueTicketDto>(`/public/queue/${id}/review`, {
      method: 'POST',
      body: JSON.stringify({ rating, comment }),
    }),
  requestReview: (id: string) =>
    request<QueueTicketDto>(`/queue/${id}/review/request`, { method: 'POST' }),
  whatsappSimulate: (from: string, text: string, profileName?: string) =>
    request<SimulateResponse>('/whatsapp/simulate', {
      method: 'POST',
      body: JSON.stringify({ from, text, profileName }),
    }),
  whatsappMessages: (phone?: string) =>
    request<OutboundMessageDto[]>(
      `/whatsapp/messages${phone ? `?phone=${encodeURIComponent(phone)}` : ''}`,
    ),
};

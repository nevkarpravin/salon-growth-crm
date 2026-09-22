import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { CalendarDays, ChevronLeft, ChevronRight, Plus } from 'lucide-react'
import {
  api,
  type AppointmentDto,
  type AppointmentStatus,
  type ClientDto,
  type ServiceItemDto,
  type StaffDto,
} from '../api/client'
import Modal from '../components/Modal'

const DAY_START = 9 * 60 // 09:00
const DAY_END = 21 * 60 // 21:00
const HOUR_PX = 56

const STATUS_STYLES: Record<AppointmentStatus, string> = {
  BOOKED: 'bg-blue-100 text-blue-700',
  CONFIRMED: 'bg-green-100 text-green-700',
  COMPLETED: 'bg-gray-200 text-gray-600',
  CANCELLED: 'bg-red-100 text-red-600 line-through',
  NO_SHOW: 'bg-orange-100 text-orange-700',
}

function toISODate(d: Date) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function minutesOf(iso: string) {
  const t = new Date(iso)
  return t.getHours() * 60 + t.getMinutes()
}

function fmtTime(iso: string) {
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

export default function CalendarPage() {
  const [date, setDate] = useState(() => toISODate(new Date()))
  const [staff, setStaff] = useState<StaffDto[]>([])
  const [staffFilter, setStaffFilter] = useState('')
  const [appointments, setAppointments] = useState<AppointmentDto[]>([])
  const [services, setServices] = useState<ServiceItemDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showBooking, setShowBooking] = useState(false)
  const [reschedule, setReschedule] = useState<AppointmentDto | null>(null)
  const [selected, setSelected] = useState<AppointmentDto | null>(null)

  useEffect(() => {
    api.listStaff(true).then(setStaff).catch(() => {})
    api.listServices(true).then(setServices).catch(() => {})
  }, [])

  const load = useCallback(() => {
    setLoading(true)
    setError('')
    api
      .listAppointments({
        from: `${date}T00:00:00`,
        to: `${date}T23:59:59`,
        staffId: staffFilter || undefined,
      })
      .then(setAppointments)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [date, staffFilter])

  useEffect(() => {
    load()
  }, [load])

  const visibleStaff = useMemo(
    () => staff.filter((s) => !staffFilter || s.id === staffFilter),
    [staff, staffFilter],
  )

  const byStaff = useMemo(() => {
    const map = new Map<string, AppointmentDto[]>()
    for (const s of visibleStaff) map.set(s.id, [])
    for (const a of appointments) map.get(a.staffId)?.push(a)
    return map
  }, [appointments, visibleStaff])

  const shift = (days: number) => {
    const d = new Date(date + 'T00:00:00')
    d.setDate(d.getDate() + days)
    setDate(toISODate(d))
  }

  const statusAction = async (a: AppointmentDto, status: AppointmentStatus) => {
    await api.setAppointmentStatus(a.id, status)
    setSelected(null)
    load()
  }

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <h1 className="text-2xl font-bold text-gray-900">Calendar</h1>
        </div>
        <button
          onClick={() => setShowBooking(true)}
          className="inline-flex items-center gap-1.5 rounded-lg bg-rose-600 px-4 py-2 text-sm font-medium text-white hover:bg-rose-700"
        >
          <Plus className="h-4 w-4" /> New appointment
        </button>
      </div>

      {/* Date controls */}
      <div className="mb-4 flex flex-wrap items-center gap-2">
        <button onClick={() => shift(-1)} aria-label="Previous day"
          className="rounded-lg border border-gray-300 p-2 hover:bg-gray-50">
          <ChevronLeft className="h-4 w-4" />
        </button>
        <input
          type="date"
          value={date}
          onChange={(e) => e.target.value && setDate(e.target.value)}
          className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm"
        />
        <button onClick={() => shift(1)} aria-label="Next day"
          className="rounded-lg border border-gray-300 p-2 hover:bg-gray-50">
          <ChevronRight className="h-4 w-4" />
        </button>
        <button
          onClick={() => setDate(toISODate(new Date()))}
          className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm hover:bg-gray-50"
        >
          Today
        </button>
      </div>

      {/* Staff filter chips */}
      <div className="mb-4 flex flex-wrap gap-1.5">
        <button
          onClick={() => setStaffFilter('')}
          className={`rounded-full px-3 py-1 text-xs font-medium ${
            !staffFilter ? 'bg-rose-600 text-white' : 'bg-white text-gray-600 ring-1 ring-gray-200'
          }`}
        >
          All staff
        </button>
        {staff.map((s) => (
          <button
            key={s.id}
            onClick={() => setStaffFilter(staffFilter === s.id ? '' : s.id)}
            className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium ${
              staffFilter === s.id
                ? 'bg-rose-600 text-white'
                : 'bg-white text-gray-600 ring-1 ring-gray-200'
            }`}
          >
            <span className="h-2 w-2 rounded-full" style={{ background: s.colorHex ?? '#999' }} />
            {s.name}
          </button>
        ))}
      </div>

      {error && <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</div>}
      {loading && <div className="py-10 text-center text-sm text-gray-400">Loading…</div>}

      {!loading && (
        <>
          {/* Desktop time grid */}
          <div className="hidden overflow-x-auto rounded-xl border border-gray-200 bg-white md:block">
            <div className="flex min-w-max">
              <div className="w-14 shrink-0 border-r border-gray-200">
                <div className="h-10 border-b border-gray-200" />
                {Array.from({ length: (DAY_END - DAY_START) / 60 }, (_, i) => (
                  <div key={i} className="relative text-right" style={{ height: HOUR_PX }}>
                    <span className="absolute -top-2 right-1 text-[10px] text-gray-400">
                      {String(9 + i).padStart(2, '0')}:00
                    </span>
                  </div>
                ))}
              </div>
              {visibleStaff.map((s) => (
                <div key={s.id} className="w-52 shrink-0 border-r border-gray-100 last:border-r-0">
                  <div className="flex h-10 items-center gap-2 border-b border-gray-200 px-3">
                    <span className="h-2.5 w-2.5 rounded-full" style={{ background: s.colorHex ?? '#999' }} />
                    <span className="truncate text-sm font-medium text-gray-800">{s.name}</span>
                  </div>
                  <div className="relative" style={{ height: ((DAY_END - DAY_START) / 60) * HOUR_PX }}>
                    {Array.from({ length: (DAY_END - DAY_START) / 60 }, (_, i) => (
                      <div key={i} className="border-b border-gray-50" style={{ height: HOUR_PX }} />
                    ))}
                    {(byStaff.get(s.id) ?? []).map((a) => {
                      const top = ((minutesOf(a.startTime) - DAY_START) / 60) * HOUR_PX
                      const height =
                        ((minutesOf(a.endTime) - minutesOf(a.startTime)) / 60) * HOUR_PX
                      return (
                        <button
                          key={a.id}
                          onClick={() => setSelected(a)}
                          className="absolute inset-x-1 overflow-hidden rounded-lg p-1.5 text-left text-white shadow-sm"
                          style={{
                            top,
                            height: Math.max(height, 20),
                            background: a.status === 'CANCELLED' ? '#d1d5db' : s.colorHex ?? '#64748b',
                            opacity: a.status === 'CANCELLED' || a.status === 'NO_SHOW' ? 0.6 : 0.92,
                          }}
                        >
                          <div className="truncate text-[11px] font-semibold">{a.clientName}</div>
                          <div className="truncate text-[10px]">
                            {a.services.map((x) => x.name).join(', ')}
                          </div>
                          <div className="text-[10px] opacity-80">
                            {fmtTime(a.startTime)}–{fmtTime(a.endTime)}
                          </div>
                          <span className={`mt-0.5 inline-block rounded px-1 text-[9px] font-medium ${STATUS_STYLES[a.status]}`}>
                            {a.status}
                          </span>
                        </button>
                      )
                    })}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Mobile chronological list grouped by staff */}
          <div className="space-y-4 md:hidden">
            {visibleStaff.map((s) => {
              const list = (byStaff.get(s.id) ?? []).sort((a, b) =>
                a.startTime.localeCompare(b.startTime),
              )
              if (list.length === 0) return null
              return (
                <div key={s.id}>
                  <div className="mb-2 flex items-center gap-2">
                    <span className="h-2.5 w-2.5 rounded-full" style={{ background: s.colorHex ?? '#999' }} />
                    <span className="text-sm font-semibold text-gray-800">{s.name}</span>
                  </div>
                  <div className="space-y-2">
                    {list.map((a) => (
                      <button
                        key={a.id}
                        onClick={() => setSelected(a)}
                        className="flex w-full items-center justify-between rounded-xl border border-gray-200 bg-white p-3 text-left"
                      >
                        <div>
                          <div className="text-sm font-semibold text-gray-900">{a.clientName}</div>
                          <div className="text-xs text-gray-500">
                            {fmtTime(a.startTime)}–{fmtTime(a.endTime)} ·{' '}
                            {a.services.map((x) => x.name).join(', ')}
                          </div>
                        </div>
                        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_STYLES[a.status]}`}>
                          {a.status}
                        </span>
                      </button>
                    ))}
                  </div>
                </div>
              )
            })}
            {appointments.length === 0 && (
              <div className="flex flex-col items-center gap-2 py-12 text-gray-400">
                <CalendarDays className="h-8 w-8" />
                <p className="text-sm">No appointments this day</p>
              </div>
            )}
          </div>
        </>
      )}

      {(showBooking || reschedule) && (
        <BookingModal
          services={services}
          staff={staff}
          appointment={reschedule ?? undefined}
          defaultDate={date}
          onClose={() => {
            setShowBooking(false)
            setReschedule(null)
          }}
          onSaved={() => {
            setShowBooking(false)
            setReschedule(null)
            load()
          }}
        />
      )}

      {selected && (
        <Modal title="Appointment" onClose={() => setSelected(null)}>
          <div className="space-y-3 text-sm">
            <div>
              <Link to={`/clients/${selected.clientId}`} className="font-semibold text-rose-600 hover:underline">
                {selected.clientName}
              </Link>
              <div className="text-gray-500">{selected.clientPhone}</div>
            </div>
            <div className="text-gray-700">
              {fmtTime(selected.startTime)}–{fmtTime(selected.endTime)} · {selected.staffName}
            </div>
            <div className="text-gray-700">{selected.services.map((s) => s.name).join(', ')}</div>
            <div className="font-medium text-gray-900">
              ₹{Number(selected.totalPrice ?? 0).toLocaleString('en-IN')}
            </div>
            {selected.notes && <p className="text-gray-500">{selected.notes}</p>}
            <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_STYLES[selected.status]}`}>
              {selected.status}
            </span>
            <div className="flex flex-wrap gap-2 pt-2">
              {(selected.status === 'BOOKED' || selected.status === 'CONFIRMED' || selected.status === 'COMPLETED')
                && selected.saleStatus !== 'PAID' && (
                <Link
                  to={`/checkout?appointmentId=${selected.id}`}
                  className="rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-emerald-700"
                >
                  Checkout
                </Link>
              )}
              {selected.saleStatus === 'PAID' && selected.saleId && (
                <Link
                  to={`/sales/${selected.saleId}`}
                  className="rounded-lg border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50"
                >
                  View invoice
                </Link>
              )}
              {selected.status === 'BOOKED' && (
                <ActionBtn label="Confirm" onClick={() => statusAction(selected, 'CONFIRMED')} />
              )}
              {(selected.status === 'BOOKED' || selected.status === 'CONFIRMED') && (
                <>
                  <ActionBtn label="Complete" onClick={() => statusAction(selected, 'COMPLETED')} />
                  <ActionBtn label="No-show" onClick={() => statusAction(selected, 'NO_SHOW')} />
                  <ActionBtn label="Cancel" danger onClick={() => statusAction(selected, 'CANCELLED')} />
                  <ActionBtn
                    label="Reschedule"
                    onClick={() => {
                      setReschedule(selected)
                      setSelected(null)
                    }}
                  />
                </>
              )}
            </div>
          </div>
        </Modal>
      )}
    </div>
  )
}

function ActionBtn({
  label,
  danger,
  onClick,
}: {
  label: string
  danger?: boolean
  onClick: () => void
}) {
  return (
    <button
      onClick={onClick}
      className={`rounded-lg px-3 py-1.5 text-xs font-medium ${
        danger
          ? 'border border-red-200 text-red-600 hover:bg-red-50'
          : 'bg-rose-600 text-white hover:bg-rose-700'
      }`}
    >
      {label}
    </button>
  )
}

function BookingModal({
  services,
  staff,
  appointment,
  defaultDate,
  onClose,
  onSaved,
}: {
  services: ServiceItemDto[]
  staff: StaffDto[]
  appointment?: AppointmentDto
  defaultDate: string
  onClose: () => void
  onSaved: () => void
}) {
  const [clientQuery, setClientQuery] = useState('')
  const [clientResults, setClientResults] = useState<ClientDto[]>([])
  const [client, setClient] = useState<ClientDto | null>(null)
  const [serviceIds, setServiceIds] = useState<string[]>(
    appointment?.services.map((s) => s.id) ?? [],
  )
  const [staffId, setStaffId] = useState(appointment?.staffId ?? '')
  const [date, setDate] = useState(appointment?.startTime.slice(0, 10) ?? defaultDate)
  const [slot, setSlot] = useState(appointment?.startTime ?? '')
  const [slots, setSlots] = useState<{ start: string; end: string }[]>([])
  const [notes, setNotes] = useState(appointment?.notes ?? '')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (appointment) {
      // pre-fill client via getClient
      api.getClient(appointment.clientId).then(setClient).catch(() => {})
    }
  }, [appointment])

  useEffect(() => {
    if (!clientQuery.trim() || client) {
      setClientResults([])
      return
    }
    const t = setTimeout(() => {
      api.listClients({ search: clientQuery, size: 6 }).then((p) => setClientResults(p.content))
    }, 250)
    return () => clearTimeout(t)
  }, [clientQuery, client])

  useEffect(() => {
    if (!staffId || !date || serviceIds.length === 0) {
      setSlots([])
      return
    }
    api
      .availability(staffId, date, serviceIds)
      .then((r) => setSlots(r.slots))
      .catch(() => setSlots([]))
  }, [staffId, date, serviceIds])

  const selectedServices = services.filter((s) => serviceIds.includes(s.id))
  const totalMinutes = selectedServices.reduce(
    (n, s) => n + s.durationMinutes + s.processingMinutes,
    0,
  )
  const totalPrice = selectedServices.reduce((n, s) => n + Number(s.price), 0)

  const submit = async () => {
    if (!client || !staffId || !slot || serviceIds.length === 0) {
      setError('Pick a client, services, staff and a time slot')
      return
    }
    setSaving(true)
    setError('')
    try {
      const body = {
        clientId: client.id,
        staffId,
        startTime: slot,
        serviceIds,
        notes: notes || undefined,
      }
      if (appointment) await api.updateAppointment(appointment.id, body)
      else await api.createAppointment(body)
      onSaved()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  const input =
    'w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-rose-500 focus:outline-none focus:ring-1 focus:ring-rose-500'

  return (
    <Modal title={appointment ? 'Reschedule appointment' : 'New appointment'} onClose={onClose}>
      {error && <div className="mb-3 rounded-lg bg-red-50 p-2 text-sm text-red-700">{error}</div>}
      <div className="space-y-3">
        {/* Client typeahead */}
        <div className="relative">
          <label className="mb-1 block text-xs font-medium text-gray-600">Client *</label>
          {client ? (
            <div className="flex items-center justify-between rounded-lg border border-gray-300 px-3 py-2 text-sm">
              <span>
                {client.firstName} {client.lastName} — {client.phone}
              </span>
              <button type="button" onClick={() => setClient(null)} className="text-xs text-rose-600">
                Change
              </button>
            </div>
          ) : (
            <>
              <input
                className={input}
                placeholder="Search name or phone"
                value={clientQuery}
                onChange={(e) => setClientQuery(e.target.value)}
              />
              {clientResults.length > 0 && (
                <ul className="absolute z-10 mt-1 w-full overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg">
                  {clientResults.map((c) => (
                    <li key={c.id}>
                      <button
                        type="button"
                        onClick={() => setClient(c)}
                        className="block w-full px-3 py-2 text-left text-sm hover:bg-gray-50"
                      >
                        {c.firstName} {c.lastName} — {c.phone}
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </>
          )}
        </div>

        {/* Services multi-select */}
        <div>
          <label className="mb-1 block text-xs font-medium text-gray-600">Services *</label>
          <div className="flex flex-wrap gap-1.5">
            {services.map((s) => {
              const on = serviceIds.includes(s.id)
              return (
                <button
                  key={s.id}
                  type="button"
                  onClick={() =>
                    setServiceIds(on ? serviceIds.filter((x) => x !== s.id) : [...serviceIds, s.id])
                  }
                  className={`rounded-full px-3 py-1 text-xs font-medium ${
                    on ? 'bg-rose-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                  }`}
                >
                  {s.name} · {s.durationMinutes + s.processingMinutes}m · ₹{s.price}
                </button>
              )
            })}
          </div>
          {serviceIds.length > 0 && (
            <p className="mt-1 text-xs text-gray-500">
              Total: {totalMinutes} min · ₹{totalPrice.toLocaleString('en-IN')}
            </p>
          )}
        </div>

        {/* Staff + date */}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="mb-1 block text-xs font-medium text-gray-600">Staff *</label>
            <select className={input} value={staffId} onChange={(e) => { setStaffId(e.target.value); setSlot('') }}>
              <option value="">Select</option>
              {staff.map((s) => (
                <option key={s.id} value={s.id}>{s.name}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-gray-600">Date *</label>
            <input type="date" className={input} value={date} onChange={(e) => { setDate(e.target.value); setSlot('') }} />
          </div>
        </div>

        {/* Slot picker */}
        <div>
          <label className="mb-1 block text-xs font-medium text-gray-600">Time slot *</label>
          {!staffId || serviceIds.length === 0 ? (
            <p className="text-xs text-gray-400">Choose services and staff to see availability</p>
          ) : slots.length === 0 ? (
            <p className="text-xs text-gray-400">No free slots on this date</p>
          ) : (
            <div className="grid max-h-40 grid-cols-4 gap-1.5 overflow-y-auto">
              {slots.map((s) => (
                <button
                  key={s.start}
                  type="button"
                  onClick={() => setSlot(s.start)}
                  className={`rounded-lg px-2 py-1.5 text-xs font-medium ${
                    slot === s.start
                      ? 'bg-rose-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  {fmtTime(s.start)}
                </button>
              ))}
            </div>
          )}
        </div>

        <textarea
          placeholder="Notes"
          rows={2}
          className={input}
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
        />
        <button
          onClick={submit}
          disabled={saving}
          className="w-full rounded-lg bg-rose-600 py-2 text-sm font-medium text-white hover:bg-rose-700 disabled:opacity-50"
        >
          {saving ? 'Saving…' : appointment ? 'Save changes' : 'Book appointment'}
        </button>
      </div>
    </Modal>
  )
}

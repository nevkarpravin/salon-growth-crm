import { useCallback, useEffect, useState } from 'react'
import { Plus } from 'lucide-react'
import {
  api,
  type PaymentMode,
  type QueueBoardDto,
  type QueueTicketDto,
  type ServiceItemDto,
  type StaffDto,
} from '../api/client'
import Modal from '../components/Modal'

export default function QueuePage() {
  const [board, setBoard] = useState<QueueBoardDto | null>(null)
  const [error, setError] = useState('')
  const [showForm, setShowForm] = useState(false)

  const load = useCallback(() => {
    api
      .queueBoard()
      .then(setBoard)
      .catch((e) => setError(e.message))
  }, [])

  useEffect(() => {
    load()
    const t = setInterval(load, 10000)
    return () => clearInterval(t)
  }, [load])

  const act = async (fn: () => Promise<unknown>) => {
    setError('')
    try {
      await fn()
      load()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Action failed')
    }
  }

  const columns: { title: string; tickets: QueueTicketDto[] }[] = [
    { title: 'Waiting', tickets: board?.waiting ?? [] },
    { title: 'In service', tickets: board?.inService ?? [] },
    { title: 'Done today', tickets: board?.completed ?? [] },
  ]

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Queue</h1>
          {board && (
            <p className="text-xs text-gray-500">
              {board.activeStaff} staff active · updated{' '}
              {new Date(board.asOf).toLocaleTimeString()}
            </p>
          )}
        </div>
        <button
          onClick={() => setShowForm(true)}
          className="inline-flex items-center gap-1.5 rounded-lg bg-rose-600 px-4 py-2 text-sm font-medium text-white hover:bg-rose-700"
        >
          <Plus className="h-4 w-4" /> Add customer
        </button>
      </div>

      {error && (
        <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</div>
      )}

      <div className="grid gap-4 md:grid-cols-3">
        {columns.map((col) => (
          <div key={col.title}>
            <h2 className="mb-2 text-sm font-semibold uppercase text-gray-500">
              {col.title}{' '}
              <span className="text-gray-400">({col.tickets.length})</span>
            </h2>
            <div className="space-y-3">
              {col.tickets.map((t) => (
                <TicketCard key={t.id} ticket={t} act={act} />
              ))}
              {col.tickets.length === 0 && (
                <div className="rounded-xl border border-dashed border-gray-200 p-4 text-center text-xs text-gray-400">
                  Empty
                </div>
              )}
            </div>
          </div>
        ))}
      </div>

      {showForm && (
        <JoinForm
          onClose={() => setShowForm(false)}
          onJoined={() => {
            setShowForm(false)
            load()
          }}
        />
      )}
    </div>
  )
}

function TicketCard({
  ticket,
  act,
}: {
  ticket: QueueTicketDto
  act: (fn: () => Promise<unknown>) => void
}) {
  const [payMode, setPayMode] = useState<PaymentMode>('UPI_MANUAL')
  const elapsed =
    ticket.startedAt &&
    Math.max(0, Math.round((Date.now() - new Date(ticket.startedAt).getTime()) / 60000))

  const btn =
    'rounded-md border border-gray-200 px-2 py-1 text-xs font-medium text-gray-700 hover:bg-gray-100'
  const btnRose =
    'rounded-md bg-rose-600 px-2 py-1 text-xs font-medium text-white hover:bg-rose-700'

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-4">
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-2">
          <span className="flex h-8 w-8 items-center justify-center rounded-full bg-rose-100 text-sm font-bold text-rose-700">
            #{ticket.tokenNumber}
          </span>
          <div>
            <div className="font-semibold text-gray-900">{ticket.clientName}</div>
            <div className="text-xs text-gray-500">
              {ticket.services.map((s) => s.name).join(', ')}
            </div>
          </div>
        </div>
        <span
          className={`rounded-full px-2 py-0.5 text-xs font-medium ${statusPill(ticket.status)}`}
        >
          {ticket.status}
        </span>
      </div>

      <div className="mt-2 text-xs text-gray-500">
        {ticket.staffName ?? 'Any staff'}
        {(ticket.status === 'WAITING' || ticket.status === 'CALLED') &&
          ticket.position != null && (
            <>
              {' · '}position {ticket.position}
              {ticket.etaMinutes != null && ` · ~${ticket.etaMinutes} min`}
            </>
          )}
        {ticket.status === 'IN_SERVICE' && elapsed != null && ` · ${elapsed} min in`}
      </div>

      {ticket.status === 'COMPLETED' && (
        <div className="mt-2">
          <span
            className={`rounded-full px-2 py-0.5 text-xs font-medium ${
              ticket.paymentStatus === 'PAID'
                ? 'bg-green-100 text-green-700'
                : 'bg-amber-100 text-amber-700'
            }`}
          >
            {ticket.paymentStatus === 'PAID' ? 'Paid' : 'Payment pending'} · ₹
            {Number(ticket.amount ?? 0).toLocaleString('en-IN')}
          </span>
        </div>
      )}

      <div className="mt-3 flex flex-wrap items-center gap-2">
        {ticket.status === 'WAITING' && (
          <>
            <button className={btnRose} onClick={() => act(() => api.callTicket(ticket.id))}>
              Call
            </button>
            <button className={btn} onClick={() => act(() => api.startTicket(ticket.id))}>
              Start
            </button>
            <button className={btn} onClick={() => act(() => api.skipTicket(ticket.id))}>
              Skip
            </button>
            <button
              className={btn}
              onClick={() => act(() => api.cancelTicket(ticket.id, 'SALON'))}
            >
              Cancel
            </button>
          </>
        )}
        {ticket.status === 'CALLED' && (
          <>
            <button className={btnRose} onClick={() => act(() => api.startTicket(ticket.id))}>
              Start
            </button>
            <button className={btn} onClick={() => act(() => api.skipTicket(ticket.id))}>
              Skip
            </button>
          </>
        )}
        {ticket.status === 'IN_SERVICE' && (
          <button className={btnRose} onClick={() => act(() => api.finishTicket(ticket.id))}>
            Finish
          </button>
        )}
        {ticket.status === 'COMPLETED' && ticket.paymentStatus === 'PENDING' && (
          <>
            <select
              className="rounded-md border border-gray-200 px-1.5 py-1 text-xs"
              value={payMode}
              onChange={(e) => setPayMode(e.target.value as PaymentMode)}
            >
              <option value="UPI_MANUAL">UPI (manual)</option>
              <option value="CASH">Cash</option>
              <option value="CARD">Card</option>
              <option value="UPI_LINK">UPI link</option>
            </select>
            <button
              className={btnRose}
              onClick={() => act(() => api.markTicketPaid(ticket.id, payMode))}
            >
              Mark paid
            </button>
          </>
        )}
        {ticket.status === 'COMPLETED' &&
          ticket.paymentStatus === 'PAID' &&
          ticket.rating == null && (
            <button className={btn} onClick={() => act(() => api.requestReview(ticket.id))}>
              Request review
            </button>
          )}
      </div>
    </div>
  )
}

function statusPill(status: string) {
  switch (status) {
    case 'WAITING':
      return 'bg-blue-100 text-blue-700'
    case 'CALLED':
      return 'bg-amber-100 text-amber-700'
    case 'IN_SERVICE':
      return 'bg-rose-100 text-rose-700'
    case 'COMPLETED':
      return 'bg-green-100 text-green-700'
    default:
      return 'bg-gray-200 text-gray-500'
  }
}

function JoinForm({
  onClose,
  onJoined,
}: {
  onClose: () => void
  onJoined: () => void
}) {
  const [services, setServices] = useState<ServiceItemDto[]>([])
  const [staff, setStaff] = useState<StaffDto[]>([])
  const [phone, setPhone] = useState('')
  const [name, setName] = useState('')
  const [selected, setSelected] = useState<string[]>([])
  const [staffId, setStaffId] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    api.listServices(true).then(setServices).catch((e) => setError(e.message))
    api
      .listStaff(true)
      .then((list) =>
        setStaff(list.filter((s) => s.role === 'STYLIST' || s.role === 'THERAPIST')),
      )
      .catch((e) => setError(e.message))
  }, [])

  const input =
    'w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-rose-500 focus:outline-none focus:ring-1 focus:ring-rose-500'

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setError('')
    try {
      await api.joinQueue({
        phone,
        name: name || undefined,
        serviceIds: selected,
        staffId: staffId || undefined,
        source: 'DESK',
      })
      onJoined()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to join queue')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title="Add customer to queue" onClose={onClose}>
      {error && (
        <div className="mb-3 rounded-lg bg-red-50 p-2 text-sm text-red-700">{error}</div>
      )}
      <form onSubmit={submit} className="space-y-3">
        <input
          required
          placeholder="Phone *"
          className={input}
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
        />
        <input
          placeholder="Name (optional)"
          className={input}
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <div>
          <div className="mb-1 text-xs font-medium text-gray-600">Services *</div>
          <div className="max-h-48 space-y-1 overflow-y-auto rounded-lg border border-gray-200 p-2">
            {services.map((s) => (
              <label key={s.id} className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  className="h-4 w-4 accent-rose-600"
                  checked={selected.includes(s.id)}
                  onChange={(e) =>
                    setSelected(
                      e.target.checked
                        ? [...selected, s.id]
                        : selected.filter((id) => id !== s.id),
                    )
                  }
                />
                <span className="flex-1">{s.name}</span>
                <span className="text-xs text-gray-400">
                  {s.durationMinutes}m · ₹{Number(s.price).toLocaleString('en-IN')}
                </span>
              </label>
            ))}
          </div>
        </div>
        <select className={input} value={staffId} onChange={(e) => setStaffId(e.target.value)}>
          <option value="">Any available staff</option>
          {staff.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name}
            </option>
          ))}
        </select>
        <button
          disabled={saving || selected.length === 0}
          className="w-full rounded-lg bg-rose-600 py-2 text-sm font-medium text-white hover:bg-rose-700 disabled:opacity-50"
        >
          {saving ? 'Adding…' : 'Add to queue'}
        </button>
      </form>
    </Modal>
  )
}

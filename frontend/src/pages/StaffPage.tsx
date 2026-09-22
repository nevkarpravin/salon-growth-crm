import { useEffect, useState } from 'react'
import { Pencil, Plus } from 'lucide-react'
import { api, type StaffDto, type StaffRequest, type StaffRole } from '../api/client'
import Modal from '../components/Modal'

const ROLES: StaffRole[] = ['STYLIST', 'THERAPIST', 'RECEPTIONIST', 'MANAGER']
const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
const DAY_LABELS: Record<string, string> = {
  MONDAY: 'Mon', TUESDAY: 'Tue', WEDNESDAY: 'Wed', THURSDAY: 'Thu',
  FRIDAY: 'Fri', SATURDAY: 'Sat', SUNDAY: 'Sun',
}

type HoursRow = { enabled: boolean; startTime: string; endTime: string }

export default function StaffPage() {
  const [staff, setStaff] = useState<StaffDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [editing, setEditing] = useState<StaffDto | null>(null)
  const [showForm, setShowForm] = useState(false)

  const load = () => {
    setLoading(true)
    api.listStaff().then(setStaff).catch((e) => setError(e.message)).finally(() => setLoading(false))
  }

  useEffect(load, [])

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Staff</h1>
        <button
          onClick={() => { setEditing(null); setShowForm(true) }}
          className="inline-flex items-center gap-1.5 rounded-lg bg-rose-600 px-4 py-2 text-sm font-medium text-white hover:bg-rose-700"
        >
          <Plus className="h-4 w-4" /> New staff
        </button>
      </div>

      {error && <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</div>}
      {loading && <div className="py-10 text-center text-sm text-gray-400">Loading…</div>}

      {!loading && (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {staff.map((s) => (
            <div key={s.id} className="rounded-xl border border-gray-200 bg-white p-4">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <span
                    className="flex h-10 w-10 items-center justify-center rounded-full text-sm font-bold text-white"
                    style={{ background: s.colorHex ?? '#64748b' }}
                  >
                    {s.name.split(' ').map((n) => n[0]).join('').slice(0, 2)}
                  </span>
                  <div>
                    <div className="font-semibold text-gray-900">{s.name}</div>
                    <div className="text-xs text-gray-500">{s.role}{s.phone ? ` · ${s.phone}` : ''}</div>
                  </div>
                </div>
                <button
                  onClick={() => { setEditing(s); setShowForm(true) }}
                  aria-label={`Edit ${s.name}`}
                  className="p-1 text-gray-400 hover:text-gray-700"
                >
                  <Pencil className="h-4 w-4" />
                </button>
              </div>
              <div className="mt-3 flex flex-wrap gap-1">
                {DAYS.map((d) => {
                  const wh = s.workingHours.find((w) => w.dayOfWeek === d)
                  return (
                    <span
                      key={d}
                      title={wh ? `${wh.startTime}–${wh.endTime}` : 'Off'}
                      className={`rounded px-1.5 py-0.5 text-[10px] font-medium ${
                        wh ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-400'
                      }`}
                    >
                      {DAY_LABELS[d]}
                    </span>
                  )
                })}
              </div>
              {!s.active && (
                <span className="mt-2 inline-block rounded-full bg-gray-200 px-2 py-0.5 text-xs text-gray-500">
                  Inactive
                </span>
              )}
            </div>
          ))}
        </div>
      )}

      {showForm && (
        <StaffForm
          staff={editing ?? undefined}
          onClose={() => setShowForm(false)}
          onSaved={() => { setShowForm(false); load() }}
        />
      )}
    </div>
  )
}

function StaffForm({
  staff,
  onClose,
  onSaved,
}: {
  staff?: StaffDto
  onClose: () => void
  onSaved: () => void
}) {
  const [form, setForm] = useState<StaffRequest>({
    name: staff?.name ?? '',
    role: staff?.role ?? 'STYLIST',
    phone: staff?.phone ?? '',
    colorHex: staff?.colorHex ?? '#e11d48',
    active: staff?.active ?? true,
  })
  const [hours, setHours] = useState<Record<string, HoursRow>>(() => {
    const init: Record<string, HoursRow> = {}
    for (const d of DAYS) {
      const wh = staff?.workingHours.find((w) => w.dayOfWeek === d)
      init[d] = wh
        ? { enabled: true, startTime: wh.startTime.slice(0, 5), endTime: wh.endTime.slice(0, 5) }
        : { enabled: false, startTime: '10:00', endTime: '20:00' }
    }
    return init
  })
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const input =
    'w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-rose-500 focus:outline-none focus:ring-1 focus:ring-rose-500'

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setError('')
    try {
      const saved = staff
        ? await api.updateStaff(staff.id, form)
        : await api.createStaff(form)
      const enabled = DAYS.filter((d) => hours[d].enabled).map((d) => ({
        dayOfWeek: d,
        startTime: hours[d].startTime,
        endTime: hours[d].endTime,
      }))
      await api.setWorkingHours(saved.id, enabled)
      onSaved()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title={staff ? 'Edit staff' : 'New staff'} onClose={onClose}>
      {error && <div className="mb-3 rounded-lg bg-red-50 p-2 text-sm text-red-700">{error}</div>}
      <form onSubmit={submit} className="space-y-3">
        <input required placeholder="Name *" className={input} value={form.name}
          onChange={(e) => setForm({ ...form, name: e.target.value })} />
        <div className="grid grid-cols-2 gap-3">
          <select className={input} value={form.role}
            onChange={(e) => setForm({ ...form, role: e.target.value as StaffRole })}>
            {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
          </select>
          <input placeholder="Phone" className={input} value={form.phone}
            onChange={(e) => setForm({ ...form, phone: e.target.value })} />
        </div>
        <label className="flex items-center gap-2 text-sm">
          Colour
          <input type="color" className="h-8 w-12 cursor-pointer rounded border border-gray-300"
            value={form.colorHex} onChange={(e) => setForm({ ...form, colorHex: e.target.value })} />
        </label>

        <div>
          <span className="mb-1 block text-xs font-medium text-gray-600">Working hours</span>
          <div className="space-y-1.5">
            {DAYS.map((d) => {
              const row = hours[d]
              return (
                <div key={d} className="flex items-center gap-2 text-sm">
                  <label className="flex w-24 items-center gap-1.5">
                    <input
                      type="checkbox"
                      className="h-3.5 w-3.5 accent-rose-600"
                      checked={row.enabled}
                      onChange={(e) =>
                        setHours({ ...hours, [d]: { ...row, enabled: e.target.checked } })
                      }
                    />
                    {DAY_LABELS[d]}
                  </label>
                  <input
                    type="time"
                    disabled={!row.enabled}
                    className="rounded border border-gray-300 px-1.5 py-1 text-xs disabled:opacity-40"
                    value={row.startTime}
                    onChange={(e) =>
                      setHours({ ...hours, [d]: { ...row, startTime: e.target.value } })
                    }
                  />
                  <span className="text-xs text-gray-400">to</span>
                  <input
                    type="time"
                    disabled={!row.enabled}
                    className="rounded border border-gray-300 px-1.5 py-1 text-xs disabled:opacity-40"
                    value={row.endTime}
                    onChange={(e) =>
                      setHours({ ...hours, [d]: { ...row, endTime: e.target.value } })
                    }
                  />
                </div>
              )
            })}
          </div>
        </div>

        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" className="h-4 w-4 accent-rose-600" checked={form.active ?? true}
            onChange={(e) => setForm({ ...form, active: e.target.checked })} />
          Active
        </label>
        <button disabled={saving}
          className="w-full rounded-lg bg-rose-600 py-2 text-sm font-medium text-white hover:bg-rose-700 disabled:opacity-50">
          {saving ? 'Saving…' : 'Save staff'}
        </button>
      </form>
    </Modal>
  )
}

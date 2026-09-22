import { useEffect, useState } from 'react'
import { Pencil, Plus } from 'lucide-react'
import { api, type ServiceItemDto, type ServiceItemRequest } from '../api/client'
import Modal from '../components/Modal'

export default function ServicesPage() {
  const [services, setServices] = useState<ServiceItemDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [editing, setEditing] = useState<ServiceItemDto | null>(null)
  const [showForm, setShowForm] = useState(false)

  const load = () => {
    setLoading(true)
    api
      .listServices()
      .then(setServices)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  const toggleActive = async (s: ServiceItemDto) => {
    await api.updateService(s.id, {
      name: s.name,
      category: s.category,
      durationMinutes: s.durationMinutes,
      processingMinutes: s.processingMinutes,
      price: s.price,
      active: !s.active,
    })
    load()
  }

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Services</h1>
        <button
          onClick={() => { setEditing(null); setShowForm(true) }}
          className="inline-flex items-center gap-1.5 rounded-lg bg-rose-600 px-4 py-2 text-sm font-medium text-white hover:bg-rose-700"
        >
          <Plus className="h-4 w-4" /> New service
        </button>
      </div>

      {error && <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</div>}
      {loading && <div className="py-10 text-center text-sm text-gray-400">Loading…</div>}

      {!loading && (
        <>
          <div className="hidden overflow-hidden rounded-xl border border-gray-200 bg-white md:block">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-gray-200 bg-gray-50 text-xs uppercase text-gray-500">
                <tr>
                  <th className="px-4 py-3">Name</th>
                  <th className="px-4 py-3">Category</th>
                  <th className="px-4 py-3 text-right">Duration</th>
                  <th className="px-4 py-3 text-right">Processing</th>
                  <th className="px-4 py-3 text-right">Price</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {services.map((s) => (
                  <tr key={s.id}>
                    <td className="px-4 py-3 font-medium text-gray-900">{s.name}</td>
                    <td className="px-4 py-3 text-gray-600">{s.category ?? '—'}</td>
                    <td className="px-4 py-3 text-right text-gray-600">{s.durationMinutes}m</td>
                    <td className="px-4 py-3 text-right text-gray-600">{s.processingMinutes}m</td>
                    <td className="px-4 py-3 text-right text-gray-600">
                      ₹{Number(s.price).toLocaleString('en-IN')}
                    </td>
                    <td className="px-4 py-3">
                      <ActiveBadge active={s.active} onClick={() => toggleActive(s)} />
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => { setEditing(s); setShowForm(true) }}
                        aria-label={`Edit ${s.name}`}
                        className="p-1 text-gray-400 hover:text-gray-700"
                      >
                        <Pencil className="h-4 w-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="space-y-3 md:hidden">
            {services.map((s) => (
              <div key={s.id} className="rounded-xl border border-gray-200 bg-white p-4">
                <div className="flex items-start justify-between">
                  <div>
                    <div className="font-semibold text-gray-900">{s.name}</div>
                    <div className="text-xs text-gray-500">
                      {s.category ?? '—'} · {s.durationMinutes + s.processingMinutes}m · ₹
                      {Number(s.price).toLocaleString('en-IN')}
                    </div>
                  </div>
                  <ActiveBadge active={s.active} onClick={() => toggleActive(s)} />
                </div>
                <button
                  onClick={() => { setEditing(s); setShowForm(true) }}
                  className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-rose-600"
                >
                  <Pencil className="h-3 w-3" /> Edit
                </button>
              </div>
            ))}
          </div>
        </>
      )}

      {showForm && (
        <ServiceForm
          service={editing ?? undefined}
          onClose={() => setShowForm(false)}
          onSaved={() => { setShowForm(false); load() }}
        />
      )}
    </div>
  )
}

function ActiveBadge({ active, onClick }: { active: boolean; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className={`rounded-full px-2.5 py-1 text-xs font-medium ${
        active ? 'bg-green-100 text-green-700' : 'bg-gray-200 text-gray-500'
      }`}
    >
      {active ? 'Active' : 'Inactive'}
    </button>
  )
}

function ServiceForm({
  service,
  onClose,
  onSaved,
}: {
  service?: ServiceItemDto
  onClose: () => void
  onSaved: () => void
}) {
  const [form, setForm] = useState<ServiceItemRequest>({
    name: service?.name ?? '',
    category: service?.category ?? '',
    durationMinutes: service?.durationMinutes ?? 60,
    processingMinutes: service?.processingMinutes ?? 0,
    price: service?.price ?? 0,
    active: service?.active ?? true,
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
      if (service) await api.updateService(service.id, form)
      else await api.createService(form)
      onSaved()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title={service ? 'Edit service' : 'New service'} onClose={onClose}>
      {error && <div className="mb-3 rounded-lg bg-red-50 p-2 text-sm text-red-700">{error}</div>}
      <form onSubmit={submit} className="space-y-3">
        <input required placeholder="Service name *" className={input} value={form.name}
          onChange={(e) => setForm({ ...form, name: e.target.value })} />
        <input placeholder="Category" className={input} value={form.category}
          onChange={(e) => setForm({ ...form, category: e.target.value })} />
        <div className="grid grid-cols-3 gap-3">
          <label className="text-xs text-gray-600">
            Duration (min)
            <input required type="number" min="1" className={input} value={form.durationMinutes}
              onChange={(e) => setForm({ ...form, durationMinutes: Number(e.target.value) })} />
          </label>
          <label className="text-xs text-gray-600">
            Processing (min)
            <input type="number" min="0" className={input} value={form.processingMinutes}
              onChange={(e) => setForm({ ...form, processingMinutes: Number(e.target.value) })} />
          </label>
          <label className="text-xs text-gray-600">
            Price (₹)
            <input required type="number" min="0" step="0.01" className={input} value={form.price}
              onChange={(e) => setForm({ ...form, price: Number(e.target.value) })} />
          </label>
        </div>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" className="h-4 w-4 accent-rose-600" checked={form.active ?? true}
            onChange={(e) => setForm({ ...form, active: e.target.checked })} />
          Active
        </label>
        <button disabled={saving}
          className="w-full rounded-lg bg-rose-600 py-2 text-sm font-medium text-white hover:bg-rose-700 disabled:opacity-50">
          {saving ? 'Saving…' : 'Save service'}
        </button>
      </form>
    </Modal>
  )
}

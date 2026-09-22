import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  Cake,
  Calendar,
  Pencil,
  Phone,
  Plus,
  Scissors,
  Trash2,
  Mail,
} from 'lucide-react'
import {
  api,
  type ClientDto,
  type FormulaCardDto,
  type PreferredChannel,
  type TimelineEntry,
  type VisitDto,
} from '../api/client'
import Modal from '../components/Modal'
import SegmentBadge from '../components/SegmentBadge'
import TagInput from '../components/TagInput'

type Tab = 'timeline' | 'visits' | 'formulas' | 'consent'

export default function ClientDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [client, setClient] = useState<ClientDto | null>(null)
  const [tab, setTab] = useState<Tab>('timeline')
  const [timeline, setTimeline] = useState<TimelineEntry[]>([])
  const [visits, setVisits] = useState<VisitDto[]>([])
  const [formulas, setFormulas] = useState<FormulaCardDto[]>([])
  const [error, setError] = useState('')
  const [showVisit, setShowVisit] = useState(false)
  const [showFormula, setShowFormula] = useState(false)

  const reload = useCallback(() => {
    if (!id) return
    api.getClient(id).then(setClient).catch((e) => setError(e.message))
    api.timeline(id).then(setTimeline).catch(() => {})
    api.listVisits(id).then(setVisits).catch(() => {})
    api.listFormulas(id).then(setFormulas).catch(() => {})
  }, [id])

  useEffect(() => {
    reload()
  }, [reload])

  if (error) return <div className="rounded-lg bg-red-50 p-4 text-red-700">{error}</div>
  if (!client) return <div className="py-10 text-center text-gray-400">Loading…</div>

  const tabs: { key: Tab; label: string }[] = [
    { key: 'timeline', label: 'Timeline' },
    { key: 'visits', label: `Visits (${visits.length})` },
    { key: 'formulas', label: `Formula Cards (${formulas.length})` },
    { key: 'consent', label: 'Consent & Preferences' },
  ]

  const archive = async () => {
    await api.deleteClient(client.id)
    navigate('/clients')
  }

  return (
    <div>
      {/* Header */}
      <div className="mb-5 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            {client.firstName} {client.lastName}
          </h1>
          <div className="mt-1 flex flex-wrap items-center gap-3 text-sm text-gray-500">
            <span className="flex items-center gap-1">
              <Phone className="h-3.5 w-3.5" /> {client.phone}
            </span>
            {client.email && (
              <span className="flex items-center gap-1">
                <Mail className="h-3.5 w-3.5" /> {client.email}
              </span>
            )}
            {client.dateOfBirth && (
              <span className="flex items-center gap-1">
                <Cake className="h-3.5 w-3.5" /> {client.dateOfBirth}
              </span>
            )}
          </div>
          <div className="mt-2 flex flex-wrap gap-1.5">
            {client.segments.map((s) => (
              <SegmentBadge key={s} segment={s} />
            ))}
          </div>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => setShowVisit(true)}
            className="inline-flex items-center gap-1 rounded-lg bg-rose-600 px-3 py-2 text-sm font-medium text-white hover:bg-rose-700"
          >
            <Plus className="h-4 w-4" /> Visit
          </button>
          <button
            onClick={() => setShowFormula(true)}
            className="inline-flex items-center gap-1 rounded-lg border border-gray-300 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            <Plus className="h-4 w-4" /> Formula
          </button>
          <Link
            to={`/clients/${client.id}/edit`}
            className="inline-flex items-center gap-1 rounded-lg border border-gray-300 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            <Pencil className="h-4 w-4" /> Edit
          </Link>
          <button
            onClick={archive}
            className="inline-flex items-center gap-1 rounded-lg border border-red-200 px-3 py-2 text-sm font-medium text-red-600 hover:bg-red-50"
          >
            <Trash2 className="h-4 w-4" /> Archive
          </button>
        </div>
      </div>

      {/* Stats */}
      <div className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-3">
        <Stat label="Visits" value={String(client.visitCount)} />
        <Stat label="Total spend" value={`₹${Number(client.totalSpend ?? 0).toLocaleString('en-IN')}`} />
        <Stat label="Last visit" value={client.lastVisitDate ?? '—'} />
      </div>

      {/* Tabs */}
      <div className="mb-4 flex gap-1 overflow-x-auto border-b border-gray-200">
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium ${
              tab === t.key
                ? 'border-rose-600 text-rose-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'timeline' && <TimelineTab entries={timeline} />}
      {tab === 'visits' && (
        <VisitsTab visits={visits} clientId={client.id} onChange={reload} />
      )}
      {tab === 'formulas' && (
        <FormulasTab formulas={formulas} clientId={client.id} onChange={reload} />
      )}
      {tab === 'consent' && <ConsentTab client={client} onChange={reload} />}

      {showVisit && (
        <VisitModal clientId={client.id} onClose={() => setShowVisit(false)} onSaved={() => { setShowVisit(false); reload() }} />
      )}
      {showFormula && (
        <FormulaModal clientId={client.id} onClose={() => setShowFormula(false)} onSaved={() => { setShowFormula(false); reload() }} />
      )}
    </div>
  )
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-3">
      <div className="text-xs text-gray-500">{label}</div>
      <div className="mt-0.5 truncate text-lg font-bold text-gray-900">{value}</div>
    </div>
  )
}

function TimelineTab({ entries }: { entries: TimelineEntry[] }) {
  if (entries.length === 0)
    return <p className="py-8 text-center text-sm text-gray-400">No activity yet</p>
  return (
    <ol className="relative space-y-4 border-l-2 border-gray-200 pl-5">
      {entries.map((e) => (
        <li key={`${e.type}-${e.id}`} className="relative">
          <span
            className={`absolute -left-[27px] top-1 flex h-4 w-4 items-center justify-center rounded-full ring-4 ring-gray-50 ${
              e.type === 'VISIT' ? 'bg-rose-500' : 'bg-violet-500'
            }`}
          />
          <div className="rounded-xl border border-gray-200 bg-white p-3">
            <div className="flex items-center justify-between gap-2">
              <span className="text-sm font-semibold text-gray-900">{e.title}</span>
              <span className="text-xs text-gray-400">{e.date}</span>
            </div>
            {e.items.length > 0 && (
              <div className="mt-1 text-sm text-gray-600">{e.items.join(' · ')}</div>
            )}
            {e.amount != null && (
              <div className="mt-1 text-sm font-medium text-gray-700">
                ₹{Number(e.amount).toLocaleString('en-IN')}
              </div>
            )}
            {e.notes && <p className="mt-1 text-xs text-gray-500">{e.notes}</p>}
          </div>
        </li>
      ))}
    </ol>
  )
}

function VisitsTab({
  visits,
  clientId,
  onChange,
}: {
  visits: VisitDto[]
  clientId: string
  onChange: () => void
}) {
  if (visits.length === 0)
    return <p className="py-8 text-center text-sm text-gray-400">No visits recorded</p>
  return (
    <div className="space-y-3">
      {visits.map((v) => (
        <div key={v.id} className="flex items-start justify-between rounded-xl border border-gray-200 bg-white p-4">
          <div>
            <div className="flex items-center gap-2 text-sm font-semibold text-gray-900">
              <Calendar className="h-4 w-4 text-gray-400" /> {v.visitDate}
              {v.stylistName && <span className="text-gray-500">— {v.stylistName}</span>}
            </div>
            {v.services.length > 0 && (
              <div className="mt-1 flex items-center gap-1 text-sm text-gray-600">
                <Scissors className="h-3.5 w-3.5" /> {v.services.join(', ')}
              </div>
            )}
            {v.products.length > 0 && (
              <div className="mt-0.5 text-xs text-gray-500">Products: {v.products.join(', ')}</div>
            )}
            {v.notes && <p className="mt-1 text-xs text-gray-500">{v.notes}</p>}
          </div>
          <div className="flex items-center gap-2">
            {v.amount != null && (
              <span className="text-sm font-semibold text-gray-900">
                ₹{Number(v.amount).toLocaleString('en-IN')}
              </span>
            )}
            <button
              onClick={() => api.deleteVisit(clientId, v.id).then(onChange)}
              aria-label="Delete visit"
              className="p-1 text-gray-400 hover:text-red-600"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          </div>
        </div>
      ))}
    </div>
  )
}

function FormulasTab({
  formulas,
  clientId,
  onChange,
}: {
  formulas: FormulaCardDto[]
  clientId: string
  onChange: () => void
}) {
  if (formulas.length === 0)
    return <p className="py-8 text-center text-sm text-gray-400">No formula cards</p>
  return (
    <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
      {formulas.map((f) => (
        <div key={f.id} className="rounded-xl border border-gray-200 bg-white p-4">
          <div className="flex items-start justify-between">
            <div className="text-sm font-semibold text-gray-900">{f.serviceName}</div>
            <button
              onClick={() => api.deleteFormula(clientId, f.id).then(onChange)}
              aria-label="Delete formula"
              className="p-1 text-gray-400 hover:text-red-600"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          </div>
          {f.formula && (
            <pre className="mt-2 whitespace-pre-wrap rounded-lg bg-violet-50 p-2 font-mono text-xs text-violet-800">
              {f.formula}
            </pre>
          )}
          <div className="mt-2 text-xs text-gray-400">
            {f.recordedAt} {f.recordedBy && `· ${f.recordedBy}`}
          </div>
          {f.notes && <p className="mt-1 text-xs text-gray-500">{f.notes}</p>}
        </div>
      ))}
    </div>
  )
}

function ConsentTab({ client, onChange }: { client: ClientDto; onChange: () => void }) {
  const [sms, setSms] = useState(client.smsOptIn)
  const [wa, setWa] = useState(client.whatsappOptIn)
  const [em, setEm] = useState(client.emailOptIn)
  const [channel, setChannel] = useState<PreferredChannel>(client.preferredChannel)
  const [tags, setTags] = useState<string[]>(client.tags)
  const [saved, setSaved] = useState('')

  const save = async () => {
    await api.updateConsent(client.id, {
      smsOptIn: sms,
      whatsappOptIn: wa,
      emailOptIn: em,
      marketingConsentAt:
        sms || wa || em ? (client.marketingConsentAt ?? new Date().toISOString()) : null,
      preferredChannel: channel,
    })
    await api.updateTags(client.id, tags)
    setSaved('Saved')
    setTimeout(() => setSaved(''), 2000)
    onChange()
  }

  return (
    <div className="max-w-xl space-y-5 rounded-xl border border-gray-200 bg-white p-5">
      <div>
        <h3 className="mb-2 text-sm font-semibold text-gray-900">Marketing consent</h3>
        <div className="space-y-2">
          {(
            [
              ['SMS', sms, setSms],
              ['WhatsApp', wa, setWa],
              ['Email', em, setEm],
            ] as const
          ).map(([text, val, setter]) => (
            <label key={text} className="flex cursor-pointer items-center gap-2 text-sm">
              <input
                type="checkbox"
                className="h-4 w-4 rounded accent-rose-600"
                checked={val}
                onChange={(e) => setter(e.target.checked)}
              />
              {text}
            </label>
          ))}
        </div>
        <label className="mt-3 flex items-center gap-2 text-sm">
          Preferred channel
          <select
            className="rounded-lg border border-gray-300 px-2 py-1 text-sm"
            value={channel}
            onChange={(e) => setChannel(e.target.value as PreferredChannel)}
          >
            {['NONE', 'SMS', 'WHATSAPP', 'EMAIL'].map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </label>
        {client.marketingConsentAt && (
          <p className="mt-2 text-xs text-gray-400">
            Consent recorded: {new Date(client.marketingConsentAt).toLocaleString()}
          </p>
        )}
      </div>
      <div>
        <h3 className="mb-2 text-sm font-semibold text-gray-900">Tags</h3>
        <TagInput tags={tags} onChange={setTags} />
      </div>
      <div>
        <h3 className="mb-2 text-sm font-semibold text-gray-900">Profile notes</h3>
        <dl className="space-y-1 text-sm text-gray-600">
          {client.allergies && <div><dt className="inline font-medium">Allergies: </dt><dd className="inline">{client.allergies}</dd></div>}
          {client.preferences && <div><dt className="inline font-medium">Preferences: </dt><dd className="inline">{client.preferences}</dd></div>}
          {client.notes && <div><dt className="inline font-medium">Notes: </dt><dd className="inline">{client.notes}</dd></div>}
          {!client.allergies && !client.preferences && !client.notes && (
            <p className="text-gray-400">None recorded</p>
          )}
        </dl>
      </div>
      <button
        onClick={save}
        className="rounded-lg bg-rose-600 px-4 py-2 text-sm font-medium text-white hover:bg-rose-700"
      >
        {saved || 'Save'}
      </button>
    </div>
  )
}

function VisitModal({
  clientId,
  onClose,
  onSaved,
}: {
  clientId: string
  onClose: () => void
  onSaved: () => void
}) {
  const [visitDate, setVisitDate] = useState(new Date().toISOString().slice(0, 10))
  const [stylistName, setStylistName] = useState('')
  const [services, setServices] = useState('')
  const [products, setProducts] = useState('')
  const [amount, setAmount] = useState('')
  const [notes, setNotes] = useState('')
  const [error, setError] = useState('')

  const input =
    'w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-rose-500 focus:outline-none focus:ring-1 focus:ring-rose-500'

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await api.addVisit(clientId, {
        visitDate,
        stylistName: stylistName || undefined,
        services: services.split(',').map((s) => s.trim()).filter(Boolean),
        products: products.split(',').map((s) => s.trim()).filter(Boolean),
        amount: amount ? Number(amount) : undefined,
        notes: notes || undefined,
      })
      onSaved()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed')
    }
  }

  return (
    <Modal title="Add visit" onClose={onClose}>
      {error && <div className="mb-3 rounded-lg bg-red-50 p-2 text-sm text-red-700">{error}</div>}
      <form onSubmit={submit} className="space-y-3">
        <input type="date" required className={input} value={visitDate} onChange={(e) => setVisitDate(e.target.value)} />
        <input placeholder="Stylist name" className={input} value={stylistName} onChange={(e) => setStylistName(e.target.value)} />
        <input placeholder="Services (comma separated)" className={input} value={services} onChange={(e) => setServices(e.target.value)} />
        <input placeholder="Products used (comma separated)" className={input} value={products} onChange={(e) => setProducts(e.target.value)} />
        <input type="number" min="0" step="0.01" placeholder="Amount (₹)" className={input} value={amount} onChange={(e) => setAmount(e.target.value)} />
        <textarea placeholder="Notes" rows={2} className={input} value={notes} onChange={(e) => setNotes(e.target.value)} />
        <button className="w-full rounded-lg bg-rose-600 py-2 text-sm font-medium text-white hover:bg-rose-700">
          Save visit
        </button>
      </form>
    </Modal>
  )
}

function FormulaModal({
  clientId,
  onClose,
  onSaved,
}: {
  clientId: string
  onClose: () => void
  onSaved: () => void
}) {
  const [serviceName, setServiceName] = useState('')
  const [formula, setFormula] = useState('')
  const [recordedBy, setRecordedBy] = useState('')
  const [recordedAt, setRecordedAt] = useState(new Date().toISOString().slice(0, 10))
  const [notes, setNotes] = useState('')
  const [error, setError] = useState('')

  const input =
    'w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-rose-500 focus:outline-none focus:ring-1 focus:ring-rose-500'

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await api.addFormula(clientId, {
        serviceName,
        formula: formula || undefined,
        recordedBy: recordedBy || undefined,
        recordedAt,
        notes: notes || undefined,
      })
      onSaved()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed')
    }
  }

  return (
    <Modal title="Add formula card" onClose={onClose}>
      {error && <div className="mb-3 rounded-lg bg-red-50 p-2 text-sm text-red-700">{error}</div>}
      <form onSubmit={submit} className="space-y-3">
        <input placeholder="Service name *" required className={input} value={serviceName} onChange={(e) => setServiceName(e.target.value)} />
        <textarea placeholder="Formula" rows={3} className={`${input} font-mono`} value={formula} onChange={(e) => setFormula(e.target.value)} />
        <input placeholder="Recorded by" className={input} value={recordedBy} onChange={(e) => setRecordedBy(e.target.value)} />
        <input type="date" required className={input} value={recordedAt} onChange={(e) => setRecordedAt(e.target.value)} />
        <textarea placeholder="Notes" rows={2} className={input} value={notes} onChange={(e) => setNotes(e.target.value)} />
        <button className="w-full rounded-lg bg-rose-600 py-2 text-sm font-medium text-white hover:bg-rose-700">
          Save formula
        </button>
      </form>
    </Modal>
  )
}

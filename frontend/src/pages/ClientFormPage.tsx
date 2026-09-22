import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api, type ClientRequest, type Gender, type PreferredChannel } from '../api/client'
import TagInput from '../components/TagInput'
import { ApiError } from '../api/client'

const GENDERS: Gender[] = ['FEMALE', 'MALE', 'OTHER', 'UNSPECIFIED']
const CHANNELS: PreferredChannel[] = ['NONE', 'SMS', 'WHATSAPP', 'EMAIL']

export default function ClientFormPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const isEdit = Boolean(id)

  const [form, setForm] = useState<ClientRequest>({
    firstName: '',
    lastName: '',
    phone: '',
    email: '',
    dateOfBirth: '',
    gender: 'UNSPECIFIED',
    allergies: '',
    notes: '',
    preferences: '',
    tags: [],
    smsOptIn: false,
    whatsappOptIn: false,
    emailOptIn: false,
    preferredChannel: 'NONE',
  })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [serverError, setServerError] = useState('')
  const [loading, setLoading] = useState(isEdit)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!id) return
    api
      .getClient(id)
      .then((c) => {
        setForm({
          firstName: c.firstName,
          lastName: c.lastName ?? '',
          phone: c.phone,
          email: c.email ?? '',
          dateOfBirth: c.dateOfBirth ?? '',
          gender: c.gender ?? 'UNSPECIFIED',
          allergies: c.allergies ?? '',
          notes: c.notes ?? '',
          preferences: c.preferences ?? '',
          tags: c.tags ?? [],
          smsOptIn: c.smsOptIn,
          whatsappOptIn: c.whatsappOptIn,
          emailOptIn: c.emailOptIn,
          preferredChannel: c.preferredChannel,
        })
      })
      .catch((e) => setServerError(e.message))
      .finally(() => setLoading(false))
  }, [id])

  const set = <K extends keyof ClientRequest>(key: K, value: ClientRequest[K]) =>
    setForm((f) => ({ ...f, [key]: value }))

  const validate = () => {
    const errs: Record<string, string> = {}
    if (!form.firstName.trim()) errs.firstName = 'First name is required'
    if (!form.phone.trim()) errs.phone = 'Phone is required'
    else if (!/^[0-9+\-() ]{6,20}$/.test(form.phone.trim()))
      errs.phone = 'Enter a valid phone number'
    if (form.email && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email))
      errs.email = 'Enter a valid email'
    setErrors(errs)
    return Object.keys(errs).length === 0
  }

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!validate()) return
    setSaving(true)
    setServerError('')
    try {
      const saved = isEdit
        ? await api.updateClient(id!, form)
        : await api.createClient(form)
      navigate(`/clients/${saved.id}`)
    } catch (err) {
      setServerError(err instanceof ApiError ? err.detail : 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <div className="py-10 text-center text-gray-400">Loading…</div>

  const input =
    'w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-rose-500 focus:outline-none focus:ring-1 focus:ring-rose-500'
  const label = 'mb-1 block text-sm font-medium text-gray-700'

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="mb-6 text-2xl font-bold text-gray-900">
        {isEdit ? 'Edit Client' : 'New Client'}
      </h1>
      {serverError && (
        <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{serverError}</div>
      )}
      <form onSubmit={submit} className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <Field label="First name *" error={errors.firstName}>
          <input className={input} value={form.firstName}
            onChange={(e) => set('firstName', e.target.value)} />
        </Field>
        <Field label="Last name">
          <input className={input} value={form.lastName}
            onChange={(e) => set('lastName', e.target.value)} />
        </Field>
        <Field label="Phone *" error={errors.phone}>
          <input className={input} value={form.phone}
            onChange={(e) => set('phone', e.target.value)} />
        </Field>
        <Field label="Email" error={errors.email}>
          <input className={input} type="email" value={form.email}
            onChange={(e) => set('email', e.target.value)} />
        </Field>
        <Field label="Date of birth">
          <input className={input} type="date" value={form.dateOfBirth}
            onChange={(e) => set('dateOfBirth', e.target.value)} />
        </Field>
        <Field label="Gender">
          <select className={input} value={form.gender}
            onChange={(e) => set('gender', e.target.value as Gender)}>
            {GENDERS.map((g) => (
              <option key={g} value={g}>{g}</option>
            ))}
          </select>
        </Field>
        <Field label="Tags" className="md:col-span-2">
          <TagInput tags={form.tags ?? []} onChange={(t) => set('tags', t)} />
        </Field>
        <Field label="Allergies" className="md:col-span-2">
          <textarea className={input} rows={2} value={form.allergies}
            onChange={(e) => set('allergies', e.target.value)} />
        </Field>
        <Field label="Preferences">
          <textarea className={input} rows={2} value={form.preferences}
            onChange={(e) => set('preferences', e.target.value)} />
        </Field>
        <Field label="Notes">
          <textarea className={input} rows={2} value={form.notes}
            onChange={(e) => set('notes', e.target.value)} />
        </Field>

        <div className="md:col-span-2">
          <span className={label}>Consent</span>
          <div className="flex flex-wrap gap-4 rounded-xl border border-gray-200 bg-white p-4">
            {(
              [
                ['smsOptIn', 'SMS'],
                ['whatsappOptIn', 'WhatsApp'],
                ['emailOptIn', 'Email'],
              ] as const
            ).map(([key, text]) => (
              <label key={key} className="flex cursor-pointer items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  className="h-4 w-4 rounded accent-rose-600"
                  checked={Boolean(form[key])}
                  onChange={(e) => set(key, e.target.checked)}
                />
                {text}
              </label>
            ))}
            <label className="flex items-center gap-2 text-sm">
              Preferred channel
              <select
                className="rounded-lg border border-gray-300 px-2 py-1 text-sm"
                value={form.preferredChannel}
                onChange={(e) => set('preferredChannel', e.target.value as PreferredChannel)}
              >
                {CHANNELS.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </label>
          </div>
        </div>

        <div className="flex gap-3 md:col-span-2">
          <button
            type="submit"
            disabled={saving}
            className="rounded-lg bg-rose-600 px-5 py-2 text-sm font-medium text-white hover:bg-rose-700 disabled:opacity-50"
          >
            {saving ? 'Saving…' : isEdit ? 'Save changes' : 'Create client'}
          </button>
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="rounded-lg border border-gray-300 px-5 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}

function Field({
  label,
  error,
  className = '',
  children,
}: {
  label?: string
  error?: string
  className?: string
  children: React.ReactNode
}) {
  return (
    <div className={className}>
      {label && <label className="mb-1 block text-sm font-medium text-gray-700">{label}</label>}
      {children}
      {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
    </div>
  )
}

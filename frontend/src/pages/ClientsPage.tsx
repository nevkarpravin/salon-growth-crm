import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ChevronLeft, ChevronRight, Plus, Search, Users } from 'lucide-react'
import { api, type ClientDto, type Page, type SegmentCount, type TagCount } from '../api/client'
import SegmentBadge, { segmentLabel } from '../components/SegmentBadge'

const SEGMENT_ORDER = ['NEW', 'AT_RISK', 'LAPSED', 'VIP', 'BIRTHDAY_THIS_MONTH']
const PAGE_SIZE = 15

export default function ClientsPage() {
  const navigate = useNavigate()
  const [data, setData] = useState<Page<ClientDto> | null>(null)
  const [segments, setSegments] = useState<SegmentCount[]>([])
  const [tags, setTags] = useState<TagCount[]>([])
  const [search, setSearch] = useState('')
  const [debounced, setDebounced] = useState('')
  const [segment, setSegment] = useState('')
  const [tag, setTag] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const t = setTimeout(() => setDebounced(search), 300)
    return () => clearTimeout(t)
  }, [search])

  useEffect(() => {
    api.segments().then(setSegments).catch(() => {})
    api.tags().then(setTags).catch(() => {})
  }, [])

  useEffect(() => {
    setPage(0)
  }, [debounced, segment, tag])

  const load = useCallback(() => {
    setLoading(true)
    setError('')
    api
      .listClients({
        search: debounced || undefined,
        tag: tag || undefined,
        segment: segment || undefined,
        page,
        size: PAGE_SIZE,
        sort: 'createdAt,desc',
      })
      .then(setData)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [debounced, tag, segment, page])

  useEffect(() => {
    load()
  }, [load])

  const segCount = (s: string) => segments.find((x) => x.segment === s)?.count ?? 0

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-gray-900">Clients</h1>
        <Link
          to="/clients/new"
          className="inline-flex items-center gap-1.5 rounded-lg bg-rose-600 px-4 py-2 text-sm font-medium text-white hover:bg-rose-700"
        >
          <Plus className="h-4 w-4" /> New Client
        </Link>
      </div>

      {/* Segment dashboard cards */}
      <div className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
        {SEGMENT_ORDER.map((s) => (
          <button
            key={s}
            onClick={() => setSegment(segment === s ? '' : s)}
            className={`rounded-xl border p-3 text-left transition-colors ${
              segment === s
                ? 'border-rose-500 bg-rose-50'
                : 'border-gray-200 bg-white hover:border-gray-300'
            }`}
          >
            <div className="text-xs font-medium uppercase tracking-wide text-gray-500">
              {segmentLabel(s)}
            </div>
            <div className="mt-1 text-2xl font-bold text-gray-900">{segCount(s)}</div>
          </button>
        ))}
      </div>

      {/* Search + tag chips */}
      <div className="mb-4 space-y-3">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
          <input
            className="w-full rounded-lg border border-gray-300 py-2 pl-10 pr-3 text-sm focus:border-rose-500 focus:outline-none focus:ring-1 focus:ring-rose-500"
            placeholder="Search name, phone or email"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        {tags.length > 0 && (
          <div className="flex flex-wrap gap-1.5">
            {tags.map(({ tag: t, count }) => (
              <button
                key={t}
                onClick={() => setTag(tag === t ? '' : t)}
                className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
                  tag === t
                    ? 'bg-rose-600 text-white'
                    : 'bg-white text-gray-600 ring-1 ring-gray-200 hover:bg-gray-50'
                }`}
              >
                {t} ({count})
              </button>
            ))}
          </div>
        )}
      </div>

      {error && (
        <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</div>
      )}

      {/* Table (md+) */}
      <div className="hidden overflow-x-auto rounded-xl border border-gray-200 bg-white md:block">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-gray-200 bg-gray-50 text-xs uppercase text-gray-500">
            <tr>
              <th className="px-4 py-3">Name</th>
              <th className="px-4 py-3">Phone</th>
              <th className="px-4 py-3">Segments</th>
              <th className="px-4 py-3">Tags</th>
              <th className="px-4 py-3 text-right">Visits</th>
              <th className="px-4 py-3 text-right">Spend</th>
              <th className="px-4 py-3">Last visit</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {data?.content.map((c) => (
              <tr
                key={c.id}
                className="cursor-pointer hover:bg-gray-50"
                onClick={() => navigate(`/clients/${c.id}`)}
              >
                <td className="px-4 py-3 font-medium text-gray-900">
                  {c.firstName} {c.lastName}
                </td>
                <td className="px-4 py-3 text-gray-600">{c.phone}</td>
                <td className="px-4 py-3">
                  <div className="flex flex-wrap gap-1">
                    {c.segments.map((s) => (
                      <SegmentBadge key={s} segment={s} />
                    ))}
                  </div>
                </td>
                <td className="px-4 py-3 text-gray-600">{c.tags.join(', ')}</td>
                <td className="px-4 py-3 text-right text-gray-600">{c.visitCount}</td>
                <td className="px-4 py-3 text-right text-gray-600">
                  ₹{Number(c.totalSpend ?? 0).toLocaleString('en-IN')}
                </td>
                <td className="px-4 py-3 text-gray-600">{c.lastVisitDate ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {data && data.content.length === 0 && !loading && (
          <Empty label="No clients found" />
        )}
      </div>

      {/* Cards (<md) */}
      <div className="space-y-3 md:hidden">
        {data?.content.map((c) => (
          <Link
            key={c.id}
            to={`/clients/${c.id}`}
            className="block rounded-xl border border-gray-200 bg-white p-4"
          >
            <div className="flex items-start justify-between">
              <div>
                <div className="font-semibold text-gray-900">
                  {c.firstName} {c.lastName}
                </div>
                <div className="text-sm text-gray-500">{c.phone}</div>
              </div>
              <div className="text-right text-xs text-gray-500">
                <div>{c.visitCount} visits</div>
                <div>₹{Number(c.totalSpend ?? 0).toLocaleString('en-IN')}</div>
              </div>
            </div>
            {c.segments.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-1">
                {c.segments.map((s) => (
                  <SegmentBadge key={s} segment={s} />
                ))}
              </div>
            )}
          </Link>
        ))}
        {data && data.content.length === 0 && !loading && <Empty label="No clients found" />}
      </div>

      {loading && (
        <div className="flex items-center justify-center py-10 text-gray-400">
          <Users className="mr-2 h-5 w-5 animate-pulse" /> Loading…
        </div>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-center gap-3">
          <button
            disabled={page === 0}
            onClick={() => setPage(page - 1)}
            className="rounded-lg border border-gray-300 p-2 disabled:opacity-40"
            aria-label="Previous page"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>
          <span className="text-sm text-gray-600">
            Page {data.number + 1} of {data.totalPages}
          </span>
          <button
            disabled={page >= data.totalPages - 1}
            onClick={() => setPage(page + 1)}
            className="rounded-lg border border-gray-300 p-2 disabled:opacity-40"
            aria-label="Next page"
          >
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      )}
    </div>
  )
}

function Empty({ label }: { label: string }) {
  return <div className="py-10 text-center text-sm text-gray-400">{label}</div>
}

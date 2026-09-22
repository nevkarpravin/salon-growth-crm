import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, type Page, type SaleDto, type SaleSummary } from '../api/client'

function today() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
function daysAgo(n: number) {
  const d = new Date()
  d.setDate(d.getDate() - n)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const STATUS_STYLE: Record<string, string> = {
  PAID: 'bg-green-100 text-green-700',
  DRAFT: 'bg-blue-100 text-blue-700',
  VOID: 'bg-gray-200 text-gray-500',
  REFUNDED: 'bg-amber-100 text-amber-700',
}

export default function SalesPage() {
  const [from, setFrom] = useState(daysAgo(30))
  const [to, setTo] = useState(today())
  const [summary, setSummary] = useState<SaleSummary | null>(null)
  const [sales, setSales] = useState<Page<SaleDto> | null>(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)

  const load = useCallback(() => {
    setLoading(true)
    api.saleSummary(from, to).then(setSummary).catch(() => {})
    api.listSales({ from, to, size: 25, page }).then(setSales).catch(() => {}).finally(() => setLoading(false))
  }, [from, to, page])

  useEffect(() => { load() }, [load])

  const retailPct = summary && summary.revenue > 0
    ? Math.round((summary.retailRevenue / (summary.serviceRevenue + summary.retailRevenue || 1)) * 100)
    : 0

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-gray-900">Sales</h1>
        <div className="flex items-center gap-2 text-sm">
          <input type="date" value={from} onChange={(e) => { setFrom(e.target.value); setPage(0) }}
            className="rounded-lg border border-gray-300 px-2 py-1.5" />
          <span className="text-gray-400">to</span>
          <input type="date" value={to} onChange={(e) => { setTo(e.target.value); setPage(0) }}
            className="rounded-lg border border-gray-300 px-2 py-1.5" />
        </div>
      </div>

      {summary && (
        <div className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
          <Card label="Revenue" value={`₹${Number(summary.revenue).toLocaleString('en-IN')}`} />
          <Card label="Sales" value={String(summary.saleCount)} />
          <Card label="Avg ticket" value={`₹${Number(summary.avgTicket).toLocaleString('en-IN')}`} />
          <Card label="Retail share" value={`${retailPct}%`} />
        </div>
      )}

      {loading && <div className="py-10 text-center text-sm text-gray-400">Loading…</div>}

      {!loading && sales && (
        <>
          <div className="hidden overflow-hidden rounded-xl border border-gray-200 bg-white md:block">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-gray-200 bg-gray-50 text-xs uppercase text-gray-500">
                <tr>
                  <th className="px-4 py-3">Invoice</th>
                  <th className="px-4 py-3">Date</th>
                  <th className="px-4 py-3">Client</th>
                  <th className="px-4 py-3">Items</th>
                  <th className="px-4 py-3 text-right">Total</th>
                  <th className="px-4 py-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {sales.content.map((s) => (
                  <tr key={s.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3">
                      <Link to={`/sales/${s.id}`} className="font-medium text-rose-600 hover:underline">
                        {s.invoiceNumber ?? 'Draft'}
                      </Link>
                    </td>
                    <td className="px-4 py-3 text-gray-600">
                      {new Date(s.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-4 py-3 text-gray-600">{s.clientName ?? 'Walk-in'}</td>
                    <td className="px-4 py-3 text-gray-600">{s.lines.length}</td>
                    <td className="px-4 py-3 text-right font-medium">
                      ₹{Number(s.total).toLocaleString('en-IN')}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_STYLE[s.status]}`}>
                        {s.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="space-y-2 md:hidden">
            {sales.content.map((s) => (
              <Link key={s.id} to={`/sales/${s.id}`}
                className="flex items-center justify-between rounded-xl border border-gray-200 bg-white p-3">
                <div>
                  <div className="text-sm font-semibold text-gray-900">
                    {s.invoiceNumber ?? 'Draft'}
                  </div>
                  <div className="text-xs text-gray-500">
                    {s.clientName ?? 'Walk-in'} · {new Date(s.createdAt).toLocaleDateString()}
                  </div>
                </div>
                <div className="text-right">
                  <div className="text-sm font-bold">₹{Number(s.total).toLocaleString('en-IN')}</div>
                  <span className={`rounded-full px-2 py-0.5 text-[10px] font-medium ${STATUS_STYLE[s.status]}`}>
                    {s.status}
                  </span>
                </div>
              </Link>
            ))}
          </div>
          {sales.content.length === 0 && (
            <p className="py-10 text-center text-sm text-gray-400">No sales in this period</p>
          )}
        </>
      )}
    </div>
  )
}

function Card({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-4">
      <div className="text-xs text-gray-500">{label}</div>
      <div className="mt-1 text-xl font-bold text-gray-900">{value}</div>
    </div>
  )
}

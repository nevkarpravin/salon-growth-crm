import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Printer } from 'lucide-react'
import { api, type SaleDto } from '../api/client'

export default function SaleDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [sale, setSale] = useState<SaleDto | null>(null)
  const [error, setError] = useState('')

  const load = () => id && api.getSale(id).then(setSale).catch((e) => setError(e.message))
  useEffect(() => { load() }, [id])

  if (error) return <div className="rounded-lg bg-red-50 p-4 text-red-700">{error}</div>
  if (!sale) return <div className="py-10 text-center text-gray-400">Loading…</div>

  const act = async (fn: (id: string) => Promise<SaleDto>) => {
    try {
      setSale(await fn(sale.id))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Action failed')
    }
  }

  return (
    <div className="mx-auto max-w-2xl">
      <div className="mb-4 flex items-center justify-between print:hidden">
        <button onClick={() => navigate(-1)} className="text-sm text-gray-500 hover:text-gray-700">
          ← Back
        </button>
        <div className="flex gap-2">
          <button
            onClick={() => window.print()}
            className="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            <Printer className="h-4 w-4" /> Print
          </button>
          {(sale.status === 'PAID' || sale.status === 'DRAFT') && (
            <button
              onClick={() => act(api.voidSale)}
              className="rounded-lg border border-red-200 px-3 py-1.5 text-sm font-medium text-red-600 hover:bg-red-50"
            >
              Void
            </button>
          )}
          {sale.status === 'PAID' && (
            <button
              onClick={() => act(api.refundSale)}
              className="rounded-lg border border-amber-300 px-3 py-1.5 text-sm font-medium text-amber-700 hover:bg-amber-50"
            >
              Refund
            </button>
          )}
        </div>
      </div>

      {/* Invoice */}
      <div className="rounded-2xl border border-gray-200 bg-white p-6 print:border-0 print:shadow-none">
        <div className="flex items-start justify-between border-b border-gray-200 pb-4">
          <div>
            <h1 className="text-xl font-bold text-gray-900">Salon Growth CRM</h1>
            <p className="text-sm text-gray-500">Tax invoice</p>
          </div>
          <div className="text-right">
            <div className="font-mono text-lg font-bold text-gray-900">
              {sale.invoiceNumber ?? 'DRAFT'}
            </div>
            <div className="text-xs text-gray-500">
              {sale.paidAt ? new Date(sale.paidAt).toLocaleString() : new Date(sale.createdAt).toLocaleString()}
            </div>
            <span className={`mt-1 inline-block rounded-full px-2 py-0.5 text-xs font-medium ${
              sale.status === 'PAID' ? 'bg-green-100 text-green-700'
              : sale.status === 'REFUNDED' ? 'bg-amber-100 text-amber-700'
              : 'bg-gray-200 text-gray-600'
            }`}>
              {sale.status}
            </span>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4 py-4 text-sm">
          <div>
            <div className="text-xs font-medium uppercase text-gray-400">Billed to</div>
            <div className="mt-1 font-medium text-gray-900">
              {sale.clientId ? (
                <Link to={`/clients/${sale.clientId}`} className="text-rose-600 hover:underline">
                  {sale.clientName}
                </Link>
              ) : (
                'Walk-in customer'
              )}
            </div>
            {sale.clientPhone && <div className="text-gray-500">{sale.clientPhone}</div>}
          </div>
          <div className="text-right">
            <div className="text-xs font-medium uppercase text-gray-400">Served by</div>
            <div className="mt-1 text-gray-900">{sale.staffName ?? '—'}</div>
          </div>
        </div>

        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-gray-200 text-left text-xs uppercase text-gray-400">
              <th className="py-2">Item</th>
              <th className="py-2 text-center">Qty</th>
              <th className="py-2 text-right">Rate</th>
              <th className="py-2 text-right">Disc</th>
              <th className="py-2 text-right">Amount</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {sale.lines.map((l) => (
              <tr key={l.id}>
                <td className="py-2 text-gray-900">
                  {l.name}
                  <span className="ml-1.5 rounded bg-gray-100 px-1 text-[10px] text-gray-500">{l.type}</span>
                </td>
                <td className="py-2 text-center text-gray-600">{l.quantity}</td>
                <td className="py-2 text-right text-gray-600">₹{Number(l.unitPrice).toLocaleString('en-IN')}</td>
                <td className="py-2 text-right text-gray-600">
                  {l.discountAmount > 0 ? `−₹${Number(l.discountAmount).toLocaleString('en-IN')}` : '—'}
                </td>
                <td className="py-2 text-right font-medium">₹{Number(l.lineTotal).toLocaleString('en-IN')}</td>
              </tr>
            ))}
          </tbody>
        </table>

        <div className="mt-4 space-y-1 border-t border-gray-200 pt-3 text-sm">
          <div className="flex justify-between text-gray-600"><span>Subtotal</span><span>₹{Number(sale.subtotal).toLocaleString('en-IN')}</span></div>
          {sale.discountAmount > 0 && (
            <div className="flex justify-between text-gray-600"><span>Discount</span><span>−₹{Number(sale.discountAmount).toLocaleString('en-IN')}</span></div>
          )}
          <div className="flex justify-between text-gray-600"><span>GST {sale.taxRate}%</span><span>₹{Number(sale.taxAmount).toLocaleString('en-IN')}</span></div>
          {sale.tipAmount > 0 && (
            <div className="flex justify-between text-gray-600"><span>Tip</span><span>₹{Number(sale.tipAmount).toLocaleString('en-IN')}</span></div>
          )}
          <div className="flex justify-between pt-1 text-lg font-bold text-gray-900">
            <span>Total</span><span>₹{Number(sale.total).toLocaleString('en-IN')}</span>
          </div>
        </div>

        {sale.payments.length > 0 && (
          <div className="mt-4 border-t border-gray-200 pt-3 text-sm">
            <div className="mb-1 text-xs font-medium uppercase text-gray-400">Payments</div>
            {sale.payments.map((p) => (
              <div key={p.id} className="flex justify-between text-gray-600">
                <span>{p.method}{p.reference ? ` · ${p.reference}` : ''}</span>
                <span>₹{Number(p.amount).toLocaleString('en-IN')}</span>
              </div>
            ))}
          </div>
        )}

        {sale.notes && <p className="mt-4 text-xs text-gray-500">Notes: {sale.notes}</p>}
        <p className="mt-6 text-center text-xs text-gray-400">Thank you for visiting!</p>
      </div>
    </div>
  )
}

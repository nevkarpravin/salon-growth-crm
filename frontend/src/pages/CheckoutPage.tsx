import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { Check, Minus, Plus, Search, ShoppingCart, X } from 'lucide-react'
import {
  api,
  ApiError,
  type ClientDto,
  type PaymentMethod,
  type ProductDto,
  type SaleDto,
  type SaleLineRequest,
  type ServiceItemDto,
  type StaffDto,
} from '../api/client'
import Modal from '../components/Modal'

type CartLine = SaleLineRequest & { name: string; unitPrice: number }
const METHODS: PaymentMethod[] = ['CASH', 'CARD', 'UPI', 'WALLET', 'OTHER']

export default function CheckoutPage() {
  const { saleId } = useParams()
  const [params] = useSearchParams()
  const appointmentId = params.get('appointmentId')
  const navigate = useNavigate()

  const [sale, setSale] = useState<SaleDto | null>(null)
  const [services, setServices] = useState<ServiceItemDto[]>([])
  const [products, setProducts] = useState<ProductDto[]>([])
  const [staff, setStaff] = useState<StaffDto[]>([])
  const [tab, setTab] = useState<'services' | 'products'>('services')
  const [search, setSearch] = useState('')

  const [lines, setLines] = useState<CartLine[]>([])
  const [client, setClient] = useState<ClientDto | null>(null)
  const [clientQuery, setClientQuery] = useState('')
  const [clientResults, setClientResults] = useState<ClientDto[]>([])
  const [walkIn, setWalkIn] = useState(false)
  const [staffId, setStaffId] = useState('')
  const [orderDiscount, setOrderDiscount] = useState('0')
  const [tip, setTip] = useState('0')
  const [payMethod, setPayMethod] = useState<PaymentMethod>('CASH')
  const [payAmount, setPayAmount] = useState('')
  const [cartOpen, setCartOpen] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [paidSale, setPaidSale] = useState<SaleDto | null>(null)

  const catalogRef = useRef(false)

  useEffect(() => {
    if (catalogRef.current) return
    catalogRef.current = true
    api.listServices(true).then(setServices).catch(() => {})
    api.listProducts({ active: true }).then(setProducts).catch(() => {})
    api.listStaff(true).then(setStaff).catch(() => {})
  }, [])

  // load existing draft or create from appointment
  useEffect(() => {
    setError('')
    if (saleId) {
      api.getSale(saleId).then((s) => {
        setSale(s)
        setLines(s.lines.map((l) => ({
          type: l.type, refId: l.refId, name: l.name,
          quantity: l.quantity, discountAmount: l.discountAmount, unitPrice: l.unitPrice,
        })))
        setOrderDiscount(String(s.discountAmount))
        setTip(String(s.tipAmount))
        if (s.staffId) setStaffId(s.staffId)
        if (s.clientId) api.getClient(s.clientId).then(setClient).catch(() => {})
        else setWalkIn(true)
      }).catch((e) => setError(e.message))
    } else if (appointmentId) {
      api.saleFromAppointment(appointmentId).then((s) => {
        setSale(s)
        navigate(`/checkout/${s.id}`, { replace: true })
      }).catch((e) => setError(e.message))
    }
  }, [saleId, appointmentId, navigate])

  // client typeahead
  useEffect(() => {
    if (!clientQuery.trim() || client || walkIn) {
      setClientResults([])
      return
    }
    const t = setTimeout(() => {
      api.listClients({ search: clientQuery, size: 6 }).then((p) => setClientResults(p.content))
    }, 250)
    return () => clearTimeout(t)
  }, [clientQuery, client, walkIn])

  const addLine = (type: 'SERVICE' | 'PRODUCT', refId: string, name: string, price: number) => {
    setLines((ls) => {
      const i = ls.findIndex((l) => l.refId === refId && l.type === type)
      if (i >= 0) {
        const copy = [...ls]
        copy[i] = { ...copy[i], quantity: copy[i].quantity + 1 }
        return copy
      }
      return [...ls, { type, refId, name, quantity: 1, discountAmount: 0, unitPrice: price }]
    })
  }

  const setQty = (i: number, qty: number) =>
    setLines((ls) => qty <= 0 ? ls.filter((_, x) => x !== i)
      : ls.map((l, x) => (x === i ? { ...l, quantity: qty } : l)))

  const setLineDiscount = (i: number, d: number) =>
    setLines((ls) => ls.map((l, x) => (x === i ? { ...l, discountAmount: d } : l)))

  const subtotal = useMemo(
    () => lines.reduce((n, l) => n + l.unitPrice * l.quantity - (l.discountAmount ?? 0), 0),
    [lines],
  )
  const taxable = Math.max(subtotal - (Number(orderDiscount) || 0), 0)
  const tax = Math.round(taxable * 0.18 * 100) / 100
  const tipNum = Number(tip) || 0
  const total = taxable + tax + tipNum
  const paidSoFar = sale?.payments.reduce((n, p) => n + Number(p.amount), 0) ?? 0
  const remaining = Math.max(total - paidSoFar, 0)

  const persist = useCallback(async () => {
    if (!sale) return null
    const s = await api.updateSale(sale.id, {
      clientId: walkIn ? undefined : client?.id,
      staffId: staffId || undefined,
      appointmentId: sale.appointmentId,
      lines: lines.map(({ type, refId, quantity, discountAmount }) => ({
        type, refId, quantity, discountAmount,
      })),
      discountAmount: Number(orderDiscount) || 0,
      tipAmount: tipNum,
    })
    setSale(s)
    return s
  }, [sale, walkIn, client, staffId, lines, orderDiscount, tipNum])

  const ensureDraft = async (): Promise<SaleDto> => {
    if (sale) return (await persist()) ?? sale
    const s = await api.createSale({
      clientId: walkIn ? undefined : client?.id,
      staffId: staffId || undefined,
      lines: lines.map(({ type, refId, quantity, discountAmount }) => ({
        type, refId, quantity, discountAmount,
      })),
      discountAmount: Number(orderDiscount) || 0,
      tipAmount: tipNum,
    })
    setSale(s)
    navigate(`/checkout/${s.id}`, { replace: true })
    return s
  }

  const addPayment = async () => {
    const amt = Number(payAmount)
    if (!amt || amt <= 0) return
    setLoading(true)
    setError('')
    try {
      const s = await ensureDraft()
      setSale(await api.addPayment(s.id, payMethod, amt))
      setPayAmount('')
    } catch (e) {
      setError(e instanceof ApiError ? e.detail : 'Failed')
    } finally {
      setLoading(false)
    }
  }

  const charge = async () => {
    setLoading(true)
    setError('')
    try {
      const s = await ensureDraft()
      const paid = await api.paySale(s.id)
      setPaidSale(paid)
    } catch (e) {
      setError(e instanceof ApiError ? e.detail : 'Charge failed')
    } finally {
      setLoading(false)
    }
  }

  const newSale = () => {
    setSale(null); setLines([]); setClient(null); setWalkIn(false)
    setOrderDiscount('0'); setTip('0'); setPaidSale(null)
    navigate('/checkout', { replace: true })
  }

  if (paidSale) {
    return (
      <div className="mx-auto max-w-md py-16 text-center">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
          <Check className="h-8 w-8 text-green-600" />
        </div>
        <h1 className="text-2xl font-bold text-gray-900">Payment complete</h1>
        <p className="mt-1 text-gray-500">
          Invoice <span className="font-semibold">{paidSale.invoiceNumber}</span> · ₹
          {Number(paidSale.total).toLocaleString('en-IN')}
        </p>
        {paidSale.warnings?.length > 0 && (
          <div className="mt-3 rounded-lg bg-amber-50 p-3 text-sm text-amber-700">
            {paidSale.warnings.map((w) => <p key={w}>{w}</p>)}
          </div>
        )}
        <div className="mt-6 flex justify-center gap-3">
          <Link to={`/sales/${paidSale.id}`}
            className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50">
            View invoice
          </Link>
          <button onClick={newSale}
            className="rounded-lg bg-rose-600 px-4 py-2 text-sm font-medium text-white hover:bg-rose-700">
            New sale
          </button>
        </div>
      </div>
    )
  }

  const cartPanel = (
    <div className="flex flex-col gap-4">
      {/* client */}
      <div className="relative">
        {walkIn ? (
          <div className="flex items-center justify-between rounded-lg bg-gray-100 px-3 py-2 text-sm">
            <span className="font-medium text-gray-700">Walk-in customer</span>
            <button onClick={() => setWalkIn(false)} className="text-xs text-rose-600">Change</button>
          </div>
        ) : client ? (
          <div className="flex items-center justify-between rounded-lg bg-rose-50 px-3 py-2 text-sm">
            <span className="font-medium text-rose-700">
              {client.firstName} {client.lastName} — {client.phone}
            </span>
            <button onClick={() => setClient(null)} className="text-xs text-rose-600">Change</button>
          </div>
        ) : (
          <div className="flex gap-2">
            <div className="relative flex-1">
              <input
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
                placeholder="Search client…"
                value={clientQuery}
                onChange={(e) => setClientQuery(e.target.value)}
              />
              {clientResults.length > 0 && (
                <ul className="absolute z-10 mt-1 w-full rounded-lg border border-gray-200 bg-white shadow-lg">
                  {clientResults.map((c) => (
                    <li key={c.id}>
                      <button onClick={() => setClient(c)}
                        className="block w-full px-3 py-2 text-left text-sm hover:bg-gray-50">
                        {c.firstName} {c.lastName} — {c.phone}
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
            <button onClick={() => setWalkIn(true)}
              className="rounded-lg border border-gray-300 px-3 text-xs font-medium text-gray-600">
              Walk-in
            </button>
          </div>
        )}
      </div>

      <select value={staffId} onChange={(e) => setStaffId(e.target.value)}
        className="rounded-lg border border-gray-300 px-3 py-2 text-sm">
        <option value="">Staff (optional)</option>
        {staff.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
      </select>

      {/* lines */}
      <div className="space-y-2">
        {lines.length === 0 && <p className="py-4 text-center text-sm text-gray-400">Cart is empty</p>}
        {lines.map((l, i) => (
          <div key={`${l.type}-${l.refId}`} className="rounded-lg border border-gray-200 p-2.5">
            <div className="flex items-center justify-between gap-2">
              <span className="min-w-0 flex-1 truncate text-sm font-medium text-gray-800">{l.name}</span>
              <div className="flex items-center gap-1">
                <button onClick={() => setQty(i, l.quantity - 1)}
                  className="rounded p-1 hover:bg-gray-100"><Minus className="h-3.5 w-3.5" /></button>
                <span className="w-6 text-center text-sm">{l.quantity}</span>
                <button onClick={() => setQty(i, l.quantity + 1)}
                  className="rounded p-1 hover:bg-gray-100"><Plus className="h-3.5 w-3.5" /></button>
              </div>
              <span className="w-16 text-right text-sm font-medium">
                ₹{(l.unitPrice * l.quantity - (l.discountAmount ?? 0)).toLocaleString('en-IN')}
              </span>
            </div>
            <div className="mt-1 flex items-center gap-2 text-xs text-gray-500">
              <span>₹{l.unitPrice} each</span>
              <label className="ml-auto flex items-center gap-1">
                Disc ₹
                <input type="number" min="0" className="w-16 rounded border border-gray-300 px-1 py-0.5"
                  value={l.discountAmount ?? 0}
                  onChange={(e) => setLineDiscount(i, Number(e.target.value) || 0)} />
              </label>
              <button onClick={() => setQty(i, 0)} className="text-gray-400 hover:text-red-500">
                <X className="h-3.5 w-3.5" />
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* discounts & tip */}
      <div className="grid grid-cols-2 gap-3">
        <label className="text-xs text-gray-600">
          Order discount ₹
          <input type="number" min="0" className="mt-1 w-full rounded-lg border border-gray-300 px-2 py-1.5 text-sm"
            value={orderDiscount} onChange={(e) => setOrderDiscount(e.target.value)} />
        </label>
        <div className="text-xs text-gray-600">
          Tip
          <div className="mt-1 flex gap-1">
            {[0, 5, 10].map((pct) => (
              <button key={pct}
                onClick={() => setTip(String(Math.round(taxable * pct) / 100))}
                className={`flex-1 rounded-lg border px-1 py-1.5 text-xs ${
                  Number(tip) === Math.round(taxable * pct) / 100
                    ? 'border-rose-500 bg-rose-50 text-rose-600'
                    : 'border-gray-300 text-gray-600'
                }`}
              >
                {pct === 0 ? 'No tip' : `${pct}%`}
              </button>
            ))}
            <input type="number" min="0" className="w-16 rounded-lg border border-gray-300 px-1.5 text-xs"
              value={tip} onChange={(e) => setTip(e.target.value)} />
          </div>
        </div>
      </div>

      {/* totals */}
      <div className="space-y-1 rounded-lg bg-gray-50 p-3 text-sm">
        <Row label="Subtotal" value={subtotal} />
        <Row label="Discount" value={-Number(orderDiscount || 0)} />
        <Row label={`GST ${sale?.taxRate ?? 18}%`} value={tax} />
        <Row label="Tip" value={tipNum} />
        <div className="mt-1 flex justify-between border-t border-gray-200 pt-2 font-bold text-gray-900">
          <span>Total</span><span>₹{total.toLocaleString('en-IN')}</span>
        </div>
      </div>

      {/* payments */}
      <div>
        <div className="mb-1.5 text-xs font-medium text-gray-600">Payments</div>
        {sale?.payments.map((p) => (
          <div key={p.id} className="flex items-center justify-between py-1 text-sm">
            <span className="rounded bg-gray-100 px-2 py-0.5 text-xs font-medium">{p.method}</span>
            <span className="flex items-center gap-2">
              ₹{Number(p.amount).toLocaleString('en-IN')}
              <button onClick={() => sale && api.removePayment(sale.id, p.id).then(setSale)}
                className="text-gray-400 hover:text-red-500"><X className="h-3.5 w-3.5" /></button>
            </span>
          </div>
        ))}
        <div className="mt-2 flex gap-1.5">
          {METHODS.map((m) => (
            <button key={m} onClick={() => setPayMethod(m)}
              className={`flex-1 rounded-lg border px-1 py-1.5 text-xs font-medium ${
                payMethod === m ? 'border-rose-500 bg-rose-50 text-rose-600' : 'border-gray-300 text-gray-600'
              }`}>
              {m}
            </button>
          ))}
        </div>
        <div className="mt-2 flex gap-2">
          <input type="number" min="0" placeholder={`₹${remaining.toFixed(0)}`}
            className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm"
            value={payAmount} onChange={(e) => setPayAmount(e.target.value)} />
          <button onClick={addPayment} disabled={loading || !payAmount}
            className="rounded-lg border border-gray-300 px-3 text-sm font-medium text-gray-700 disabled:opacity-40">
            Add
          </button>
        </div>
      </div>

      <button
        onClick={charge}
        disabled={loading || lines.length === 0 || remaining <= 0}
        className="rounded-lg bg-rose-600 py-3 text-sm font-bold text-white hover:bg-rose-700 disabled:opacity-40"
      >
        {remaining <= 0 ? 'Fully paid' : `Charge ₹${remaining.toLocaleString('en-IN')}`}
      </button>
      {paidSoFar > 0 && remaining > 0 && (
        <p className="text-center text-xs text-gray-400">
          Paid ₹{paidSoFar.toLocaleString('en-IN')} of ₹{total.toLocaleString('en-IN')}
        </p>
      )}
    </div>
  )

  const catalogue = (
    <>
      <div className="mb-3 flex gap-1 rounded-lg bg-gray-100 p-1">
        {(['services', 'products'] as const).map((t) => (
          <button key={t} onClick={() => setTab(t)}
            className={`flex-1 rounded-md py-1.5 text-sm font-medium capitalize ${
              tab === t ? 'bg-white shadow-sm' : 'text-gray-500'
            }`}>
            {t}
          </button>
        ))}
      </div>
      <div className="relative mb-3">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
        <input className="w-full rounded-lg border border-gray-300 py-2 pl-9 pr-3 text-sm"
          placeholder="Search…" value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>
      <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
        {tab === 'services' &&
          services
            .filter((s) => s.name.toLowerCase().includes(search.toLowerCase()))
            .map((s) => (
              <button key={s.id} onClick={() => addLine('SERVICE', s.id, s.name, s.price)}
                className="rounded-xl border border-gray-200 bg-white p-3 text-left hover:border-rose-300">
                <div className="text-sm font-medium text-gray-900">{s.name}</div>
                <div className="text-xs text-gray-500">{s.durationMinutes + s.processingMinutes}m</div>
                <div className="mt-1 text-sm font-semibold text-rose-600">₹{Number(s.price).toLocaleString('en-IN')}</div>
              </button>
            ))}
        {tab === 'products' &&
          products
            .filter((p) => p.name.toLowerCase().includes(search.toLowerCase()))
            .map((p) => (
              <button key={p.id} onClick={() => addLine('PRODUCT', p.id, p.name, p.price)}
                disabled={p.stockQty <= 0}
                className="rounded-xl border border-gray-200 bg-white p-3 text-left hover:border-rose-300 disabled:opacity-50">
                <div className="text-sm font-medium text-gray-900">{p.name}</div>
                <div className={`text-xs ${p.lowStock ? 'font-medium text-amber-600' : 'text-gray-500'}`}>
                  {p.stockQty} in stock{p.lowStock ? ' — low' : ''}
                </div>
                <div className="mt-1 text-sm font-semibold text-rose-600">₹{Number(p.price).toLocaleString('en-IN')}</div>
              </button>
            ))}
      </div>
    </>
  )

  return (
    <div>
      <h1 className="mb-4 text-2xl font-bold text-gray-900">Checkout</h1>
      {error && <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</div>}

      {/* Desktop: catalogue left, cart right */}
      <div className="hidden gap-6 lg:flex">
        <div className="min-w-0 flex-1">{catalogue}</div>
        <div className="w-96 shrink-0 rounded-2xl border border-gray-200 bg-white p-4">{cartPanel}</div>
      </div>

      {/* Mobile: catalogue + sticky cart bar + bottom sheet */}
      <div className="lg:hidden">
        {catalogue}
        <button
          onClick={() => setCartOpen(true)}
          className="fixed inset-x-4 bottom-20 z-30 flex items-center justify-between rounded-xl bg-rose-600 px-4 py-3 text-sm font-bold text-white shadow-lg"
        >
          <span className="flex items-center gap-2">
            <ShoppingCart className="h-4 w-4" /> {lines.length} items
          </span>
          <span>₹{total.toLocaleString('en-IN')} →</span>
        </button>
        {cartOpen && (
          <Modal title="Cart" onClose={() => setCartOpen(false)}>{cartPanel}</Modal>
        )}
      </div>
    </div>
  )
}

function Row({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex justify-between text-gray-600">
      <span>{label}</span>
      <span>{value < 0 ? '−' : ''}₹{Math.abs(value).toLocaleString('en-IN')}</span>
    </div>
  )
}

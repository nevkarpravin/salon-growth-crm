import { useEffect, useState } from 'react'
import { Pencil, Plus } from 'lucide-react'
import { api, type ProductDto, type ProductRequest } from '../api/client'
import Modal from '../components/Modal'

export default function ProductsPage() {
  const [products, setProducts] = useState<ProductDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [editing, setEditing] = useState<ProductDto | null>(null)
  const [showForm, setShowForm] = useState(false)

  const load = () => {
    setLoading(true)
    api.listProducts().then(setProducts).catch((e) => setError(e.message)).finally(() => setLoading(false))
  }
  useEffect(load, [])

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Products</h1>
        <button onClick={() => { setEditing(null); setShowForm(true) }}
          className="inline-flex items-center gap-1.5 rounded-lg bg-rose-600 px-4 py-2 text-sm font-medium text-white hover:bg-rose-700">
          <Plus className="h-4 w-4" /> New product
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
                  <th className="px-4 py-3">SKU</th>
                  <th className="px-4 py-3">Category</th>
                  <th className="px-4 py-3 text-right">Price</th>
                  <th className="px-4 py-3 text-right">Stock</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {products.map((p) => (
                  <tr key={p.id}>
                    <td className="px-4 py-3 font-medium text-gray-900">{p.name}</td>
                    <td className="px-4 py-3 font-mono text-xs text-gray-500">{p.sku ?? '—'}</td>
                    <td className="px-4 py-3 text-gray-600">{p.category ?? '—'}</td>
                    <td className="px-4 py-3 text-right text-gray-600">₹{Number(p.price).toLocaleString('en-IN')}</td>
                    <td className="px-4 py-3 text-right">
                      <span className={p.lowStock ? 'font-semibold text-amber-600' : 'text-gray-600'}>
                        {p.stockQty}
                      </span>
                      {p.lowStock && (
                        <span className="ml-1.5 rounded-full bg-amber-100 px-1.5 py-0.5 text-[10px] font-medium text-amber-700">LOW</span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                        p.active ? 'bg-green-100 text-green-700' : 'bg-gray-200 text-gray-500'
                      }`}>{p.active ? 'Active' : 'Inactive'}</span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button onClick={() => { setEditing(p); setShowForm(true) }}
                        aria-label={`Edit ${p.name}`} className="p-1 text-gray-400 hover:text-gray-700">
                        <Pencil className="h-4 w-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="space-y-2 md:hidden">
            {products.map((p) => (
              <div key={p.id} className="rounded-xl border border-gray-200 bg-white p-4">
                <div className="flex items-start justify-between">
                  <div>
                    <div className="font-semibold text-gray-900">{p.name}</div>
                    <div className="text-xs text-gray-500">
                      {p.sku ?? '—'} · {p.category ?? '—'} · ₹{Number(p.price).toLocaleString('en-IN')}
                    </div>
                  </div>
                  <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                    p.lowStock ? 'bg-amber-100 text-amber-700' : 'bg-green-100 text-green-700'
                  }`}>
                    {p.stockQty} left
                  </span>
                </div>
                <button onClick={() => { setEditing(p); setShowForm(true) }}
                  className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-rose-600">
                  <Pencil className="h-3 w-3" /> Edit
                </button>
              </div>
            ))}
          </div>
        </>
      )}

      {showForm && (
        <ProductForm product={editing ?? undefined}
          onClose={() => setShowForm(false)}
          onSaved={() => { setShowForm(false); load() }} />
      )}
    </div>
  )
}

function ProductForm({
  product, onClose, onSaved,
}: {
  product?: ProductDto
  onClose: () => void
  onSaved: () => void
}) {
  const [form, setForm] = useState<ProductRequest>({
    name: product?.name ?? '',
    sku: product?.sku ?? '',
    category: product?.category ?? '',
    price: product?.price ?? 0,
    stockQty: product?.stockQty ?? 0,
    lowStockThreshold: product?.lowStockThreshold ?? 5,
    active: product?.active ?? true,
  })
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const input = 'w-full rounded-lg border border-gray-300 px-3 py-2 text-sm'

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setError('')
    try {
      if (product) await api.updateProduct(product.id, form)
      else await api.createProduct(form)
      onSaved()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title={product ? 'Edit product' : 'New product'} onClose={onClose}>
      {error && <div className="mb-3 rounded-lg bg-red-50 p-2 text-sm text-red-700">{error}</div>}
      <form onSubmit={submit} className="space-y-3">
        <input required placeholder="Name *" className={input} value={form.name}
          onChange={(e) => setForm({ ...form, name: e.target.value })} />
        <div className="grid grid-cols-2 gap-3">
          <input placeholder="SKU" className={input} value={form.sku}
            onChange={(e) => setForm({ ...form, sku: e.target.value })} />
          <input placeholder="Category" className={input} value={form.category}
            onChange={(e) => setForm({ ...form, category: e.target.value })} />
        </div>
        <div className="grid grid-cols-3 gap-3">
          <label className="text-xs text-gray-600">Price ₹
            <input required type="number" min="0" step="0.01" className={input} value={form.price}
              onChange={(e) => setForm({ ...form, price: Number(e.target.value) })} />
          </label>
          <label className="text-xs text-gray-600">Stock
            <input type="number" className={input} value={form.stockQty}
              onChange={(e) => setForm({ ...form, stockQty: Number(e.target.value) })} />
          </label>
          <label className="text-xs text-gray-600">Low-stock at
            <input type="number" min="0" className={input} value={form.lowStockThreshold}
              onChange={(e) => setForm({ ...form, lowStockThreshold: Number(e.target.value) })} />
          </label>
        </div>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" className="h-4 w-4 accent-rose-600" checked={form.active ?? true}
            onChange={(e) => setForm({ ...form, active: e.target.checked })} />
          Active
        </label>
        <button disabled={saving}
          className="w-full rounded-lg bg-rose-600 py-2 text-sm font-medium text-white hover:bg-rose-700 disabled:opacity-50">
          {saving ? 'Saving…' : 'Save product'}
        </button>
      </form>
    </Modal>
  )
}

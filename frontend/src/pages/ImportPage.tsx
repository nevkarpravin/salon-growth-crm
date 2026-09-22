import { useRef, useState } from 'react'
import { Download, FileUp, Upload } from 'lucide-react'
import { api, type ImportResult } from '../api/client'

export default function ImportPage() {
  const [dragging, setDragging] = useState(false)
  const [file, setFile] = useState<File | null>(null)
  const [result, setResult] = useState<ImportResult | null>(null)
  const [error, setError] = useState('')
  const [uploading, setUploading] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  const upload = async (f: File) => {
    setUploading(true)
    setError('')
    setResult(null)
    try {
      setResult(await api.importCsv(f))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Upload failed')
    } finally {
      setUploading(false)
    }
  }

  const pick = (f?: File | null) => {
    if (!f) return
    setFile(f)
    upload(f)
  }

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-2 text-2xl font-bold text-gray-900">Import Clients</h1>
      <p className="mb-5 text-sm text-gray-500">
        Upload a CSV with columns{' '}
        <code className="rounded bg-gray-100 px-1 text-xs">
          firstName,lastName,phone,email,dateOfBirth,gender,tags,notes
        </code>
        . Rows with an existing phone are skipped; tags are pipe-separated.
      </p>

      <a
        href={api.templateUrl}
        download
        className="mb-5 inline-flex items-center gap-1.5 rounded-lg border border-gray-300 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
      >
        <Download className="h-4 w-4" /> Download template
      </a>

      <div
        onDragOver={(e) => {
          e.preventDefault()
          setDragging(true)
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => {
          e.preventDefault()
          setDragging(false)
          pick(e.dataTransfer.files?.[0])
        }}
        onClick={() => inputRef.current?.click()}
        className={`flex cursor-pointer flex-col items-center justify-center rounded-2xl border-2 border-dashed p-10 text-center transition-colors ${
          dragging ? 'border-rose-500 bg-rose-50' : 'border-gray-300 bg-white hover:border-gray-400'
        }`}
      >
        <FileUp className="mb-3 h-10 w-10 text-gray-400" />
        <p className="text-sm font-medium text-gray-700">
          {file ? file.name : 'Drop your CSV here or click to browse'}
        </p>
        <p className="mt-1 text-xs text-gray-400">CSV up to 10 MB</p>
        <input
          ref={inputRef}
          type="file"
          accept=".csv,text/csv"
          className="hidden"
          onChange={(e) => pick(e.target.files?.[0])}
        />
      </div>

      {uploading && (
        <div className="mt-4 flex items-center gap-2 text-sm text-gray-500">
          <Upload className="h-4 w-4 animate-pulse" /> Importing…
        </div>
      )}
      {error && <div className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</div>}

      {result && (
        <div className="mt-6 space-y-3">
          <div className="grid grid-cols-3 gap-3">
            <Stat label="Imported" value={result.imported} tone="green" />
            <Stat label="Skipped" value={result.skipped} tone="gray" />
            <Stat label="Errors" value={result.errors.length} tone="red" />
          </div>
          {result.errors.length > 0 && (
            <div className="overflow-hidden rounded-xl border border-gray-200 bg-white">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-gray-200 bg-gray-50 text-xs uppercase text-gray-500">
                  <tr>
                    <th className="px-4 py-2">Row</th>
                    <th className="px-4 py-2">Error</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {result.errors.map((e) => (
                    <tr key={e.row}>
                      <td className="px-4 py-2 text-gray-600">{e.row}</td>
                      <td className="px-4 py-2 text-red-600">{e.message}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

function Stat({ label, value, tone }: { label: string; value: number; tone: 'green' | 'gray' | 'red' }) {
  const colors = {
    green: 'text-green-600',
    gray: 'text-gray-600',
    red: 'text-red-600',
  }
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-3 text-center">
      <div className={`text-2xl font-bold ${colors[tone]}`}>{value}</div>
      <div className="text-xs text-gray-500">{label}</div>
    </div>
  )
}

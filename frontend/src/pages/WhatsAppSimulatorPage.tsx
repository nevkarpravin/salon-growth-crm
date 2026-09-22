import { useCallback, useEffect, useRef, useState } from 'react'
import { Send } from 'lucide-react'
import { api, type OutboundMessageDto } from '../api/client'

interface ChatItem {
  id: string;
  direction: 'in' | 'out';
  body: string;
  createdAt?: string;
}

const QUICK = ['Hi', '1', '2', '3', '4', '5', '0']

export default function WhatsAppSimulatorPage() {
  const [phone, setPhone] = useState('9876500001')
  const [profileName, setProfileName] = useState('Test User')
  const [chat, setChat] = useState<ChatItem[]>([])
  const [input, setInput] = useState('')
  const [error, setError] = useState('')
  const [sending, setSending] = useState(false)
  const sentTexts = useRef<{ body: string; id: number }[]>([])
  const bottomRef = useRef<HTMLDivElement>(null)

  // Merge server outbox (bot messages) with locally-sent user texts.
  const load = useCallback(() => {
    if (!phone) return
    api
      .whatsappMessages(phone)
      .then((msgs: OutboundMessageDto[]) => {
        const outMsgs: ChatItem[] = msgs.map((m) => ({
          id: m.id,
          direction: 'out' as const,
          body: m.body,
          createdAt: m.createdAt,
        }))
        setChat(mergeChronological(outMsgs, sentTexts.current))
      })
      .catch((e) => setError(e.message))
  }, [phone])

  useEffect(() => {
    load()
    const t = setInterval(load, 5000)
    return () => clearInterval(t)
  }, [load])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [chat])

  const send = async (text: string) => {
    if (!text.trim() || !phone.trim()) return
    setSending(true)
    setError('')
    sentTexts.current.push({ body: text, id: Date.now() })
    try {
      await api.whatsappSimulate(phone.trim(), text, profileName || undefined)
      // the reply is persisted to the outbox; refresh shows it (plus our sent text)
      load()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Send failed')
    } finally {
      setSending(false)
      setInput('')
    }
  }

  return (
    <div>
      <h1 className="mb-1 text-2xl font-bold text-gray-900">WhatsApp simulator</h1>
      <p className="mb-4 text-sm text-gray-500">
        Simulates the WhatsApp Business webhook while the provider is set to LOG — messages
        the salon sends appear here as bot bubbles.
      </p>
      {error && (
        <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</div>
      )}

      <div className="grid gap-4 md:grid-cols-[260px_1fr]">
        <div className="space-y-3">
          <label className="block text-xs font-medium text-gray-600">
            Phone number
            <input
              className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-rose-500 focus:outline-none focus:ring-1 focus:ring-rose-500"
              value={phone}
              onChange={(e) => {
                setPhone(e.target.value)
                sentTexts.current = []
                setChat([])
              }}
            />
          </label>
          <label className="block text-xs font-medium text-gray-600">
            Profile name
            <input
              className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-rose-500 focus:outline-none focus:ring-1 focus:ring-rose-500"
              value={profileName}
              onChange={(e) => setProfileName(e.target.value)}
            />
          </label>
          <div className="flex flex-wrap gap-1.5">
            {QUICK.map((q) => (
              <button
                key={q}
                onClick={() => send(q)}
                className="rounded-full border border-gray-200 px-3 py-1 text-xs text-gray-700 hover:bg-gray-100"
              >
                {q}
              </button>
            ))}
          </div>
        </div>

        <div className="flex h-[560px] flex-col rounded-xl border border-gray-200 bg-white">
          <div className="flex-1 space-y-2 overflow-y-auto p-4">
            {chat.length === 0 && (
              <div className="py-10 text-center text-xs text-gray-400">
                Send "Hi" to start the conversation.
              </div>
            )}
            {chat.map((m) => (
              <div
                key={m.id}
                className={`flex ${m.direction === 'in' ? 'justify-end' : 'justify-start'}`}
              >
                <div
                  className={`max-w-[75%] whitespace-pre-wrap rounded-2xl px-3 py-2 text-sm ${
                    m.direction === 'in'
                      ? 'bg-gray-200 text-gray-800'
                      : 'bg-green-100 text-gray-900'
                  }`}
                >
                  {m.body}
                </div>
              </div>
            ))}
            <div ref={bottomRef} />
          </div>
          <form
            className="flex gap-2 border-t border-gray-200 p-3"
            onSubmit={(e) => {
              e.preventDefault()
              send(input)
            }}
          >
            <input
              className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-rose-500 focus:outline-none focus:ring-1 focus:ring-rose-500"
              placeholder="Type a WhatsApp message…"
              value={input}
              onChange={(e) => setInput(e.target.value)}
            />
            <button
              disabled={sending}
              className="inline-flex items-center gap-1 rounded-lg bg-rose-600 px-4 py-2 text-sm font-medium text-white hover:bg-rose-700 disabled:opacity-50"
            >
              <Send className="h-4 w-4" />
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}

// Interleave user texts and bot messages preserving send order as best we can:
// local texts get pseudo-timestamps from insertion order.
function mergeChronological(
  outMsgs: ChatItem[],
  sent: { body: string; id: number }[],
): ChatItem[] {
  const items: ChatItem[] = [...outMsgs]
  sent.forEach((s, i) => {
    items.push({
      id: `sent-${i}`,
      direction: 'in',
      body: s.body,
      createdAt: '',
    })
  })
  return items
}

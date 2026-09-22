import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Sparkles, Star } from 'lucide-react'
import { api, type QueueTicketDto } from '../api/client'

export default function PublicTicketPage() {
  const { id } = useParams<{ id: string }>()
  const [ticket, setTicket] = useState<QueueTicketDto | null>(null)
  const [error, setError] = useState('')
  const [leaving, setLeaving] = useState(false)
  const [rating, setRating] = useState(0)
  const [comment, setComment] = useState('')
  const [rated, setRated] = useState(false)

  const load = useCallback(() => {
    if (!id) return
    api.publicTicket(id).then(setTicket).catch((e) => setError(e.message))
  }, [id])

  useEffect(() => {
    load()
    const t = setInterval(load, 10000)
    return () => clearInterval(t)
  }, [load])

  const leave = async () => {
    if (!id || !window.confirm('Leave the queue?')) return
    setLeaving(true)
    try {
      await api.publicLeaveTicket(id)
      load()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not leave queue')
    } finally {
      setLeaving(false)
    }
  }

  const submitReview = async () => {
    if (!id || rating < 1) return
    try {
      await api.publicSubmitReview(id, rating, comment || undefined)
      setRated(true)
      load()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not submit review')
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 p-4">
      <div className="w-full max-w-md rounded-2xl border border-gray-200 bg-white p-6 text-center shadow-sm">
        <div className="mb-4 flex items-center justify-center gap-2">
          <Sparkles className="h-5 w-5 text-rose-600" />
          <span className="font-bold text-gray-900">Glow Salon</span>
        </div>

        {error && (
          <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</div>
        )}
        {!ticket && !error && <div className="py-10 text-sm text-gray-400">Loading…</div>}

        {ticket && (
          <>
            <div className="mb-1 text-xs uppercase text-gray-400">Your token</div>
            <div className="mb-2 text-6xl font-bold text-rose-600">
              #{ticket.tokenNumber}
            </div>
            <div className="mb-4 text-sm text-gray-500">
              {ticket.services.map((s) => s.name).join(', ')}
            </div>

            {(ticket.status === 'WAITING' || ticket.status === 'CALLED') && (
              <div className="space-y-3">
                <div className="rounded-xl bg-rose-50 p-4">
                  <div className="text-sm font-medium text-rose-700">
                    {ticket.status === 'CALLED'
                      ? "It's your turn — please come to the counter!"
                      : `Position ${ticket.position}`}
                  </div>
                  {ticket.status === 'WAITING' && (
                    <div className="mt-1 text-xs text-rose-600">
                      {ticket.peopleAhead} ahead · ~{ticket.etaMinutes} min wait
                    </div>
                  )}
                </div>
                <button
                  onClick={leave}
                  disabled={leaving}
                  className="w-full rounded-lg border border-gray-300 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
                >
                  Leave queue
                </button>
              </div>
            )}

            {ticket.status === 'IN_SERVICE' && (
              <div className="rounded-xl bg-green-50 p-4 text-sm font-medium text-green-700">
                In progress with {ticket.staffName ?? 'the team'} — enjoy!
              </div>
            )}

            {ticket.status === 'COMPLETED' && (
              <div className="space-y-3 text-left">
                <div className="rounded-xl border border-gray-200 p-4">
                  {ticket.services.map((s) => (
                    <div key={s.id} className="flex justify-between text-sm">
                      <span>{s.name}</span>
                      <span>₹{Number(s.price).toLocaleString('en-IN')}</span>
                    </div>
                  ))}
                  <div className="mt-2 flex justify-between border-t border-gray-100 pt-2 text-sm font-semibold">
                    <span>Total</span>
                    <span>₹{Number(ticket.amount ?? 0).toLocaleString('en-IN')}</span>
                  </div>
                </div>
                {ticket.paymentStatus === 'PAID' ? (
                  <div className="rounded-xl bg-green-50 p-3 text-center text-sm font-medium text-green-700">
                    Paid — thank you!
                  </div>
                ) : (
                  <>
                    {ticket.paymentLink && (
                      <a
                        href={ticket.paymentLink}
                        className="block w-full rounded-lg bg-rose-600 py-3 text-center text-sm font-semibold text-white hover:bg-rose-700"
                      >
                        Pay ₹{Number(ticket.amount ?? 0).toLocaleString('en-IN')} via UPI
                      </a>
                    )}
                    <div className="text-center text-xs text-gray-400">
                      or pay at the counter
                    </div>
                  </>
                )}

                {ticket.rating == null && !rated ? (
                  <div className="rounded-xl border border-gray-200 p-4">
                    <div className="mb-2 text-center text-sm font-medium text-gray-700">
                      How was your visit?
                    </div>
                    <div className="mb-2 flex justify-center gap-1">
                      {[1, 2, 3, 4, 5].map((n) => (
                        <button key={n} onClick={() => setRating(n)} aria-label={`${n} star`}>
                          <Star
                            className={`h-7 w-7 ${
                              n <= rating
                                ? 'fill-amber-400 text-amber-400'
                                : 'text-gray-300'
                            }`}
                          />
                        </button>
                      ))}
                    </div>
                    <textarea
                      className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-rose-500 focus:outline-none"
                      rows={2}
                      placeholder="Comment (optional)"
                      value={comment}
                      onChange={(e) => setComment(e.target.value)}
                    />
                    <button
                      onClick={submitReview}
                      disabled={rating < 1}
                      className="mt-2 w-full rounded-lg bg-rose-600 py-2 text-sm font-medium text-white hover:bg-rose-700 disabled:opacity-50"
                    >
                      Submit rating
                    </button>
                  </div>
                ) : (
                  <div className="text-center text-sm text-gray-500">
                    Thanks for your rating{ticket.rating ? ` (${ticket.rating}★)` : ''}!
                  </div>
                )}
              </div>
            )}

            {(ticket.status === 'CANCELLED' ||
              ticket.status === 'EXPIRED' ||
              ticket.status === 'SKIPPED') && (
              <div className="space-y-2">
                <div className="rounded-xl bg-gray-100 p-4 text-sm text-gray-600">
                  This queue spot is {ticket.status.toLowerCase()}.
                </div>
                <div className="text-xs text-gray-400">
                  Message us on WhatsApp to rejoin.
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}

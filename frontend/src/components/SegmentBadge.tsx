const STYLES: Record<string, string> = {
  NEW: 'bg-green-100 text-green-700',
  VIP: 'bg-amber-100 text-amber-700',
  AT_RISK: 'bg-orange-100 text-orange-700',
  LAPSED: 'bg-gray-200 text-gray-600',
  BIRTHDAY_THIS_MONTH: 'bg-pink-100 text-pink-700',
}

const LABELS: Record<string, string> = {
  NEW: 'New',
  VIP: 'VIP',
  AT_RISK: 'At Risk',
  LAPSED: 'Lapsed',
  BIRTHDAY_THIS_MONTH: 'Birthday',
}

export function segmentLabel(segment: string) {
  return LABELS[segment] ?? segment.replaceAll('_', ' ')
}

export default function SegmentBadge({ segment }: { segment: string }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
        STYLES[segment] ?? 'bg-blue-100 text-blue-700'
      }`}
    >
      {segmentLabel(segment)}
    </span>
  )
}

import { Link } from 'react-router-dom'
import { Package, Scissors, Upload, UserCog } from 'lucide-react'

const links = [
  { to: '/services', label: 'Services', icon: Scissors, desc: 'Manage service catalog' },
  { to: '/staff', label: 'Staff', icon: UserCog, desc: 'Team & working hours' },
  { to: '/products', label: 'Products', icon: Package, desc: 'Retail inventory' },
  { to: '/import', label: 'Import', icon: Upload, desc: 'Bulk CSV client import' },
]

export default function MorePage() {
  return (
    <div>
      <h1 className="mb-4 text-2xl font-bold text-gray-900">More</h1>
      <div className="space-y-2">
        {links.map(({ to, label, icon: Icon, desc }) => (
          <Link
            key={to}
            to={to}
            className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white p-4"
          >
            <Icon className="h-5 w-5 text-rose-600" />
            <div>
              <div className="font-medium text-gray-900">{label}</div>
              <div className="text-xs text-gray-500">{desc}</div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  )
}

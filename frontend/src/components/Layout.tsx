import { NavLink, Outlet } from 'react-router-dom'
import { Upload, Users, Sparkles } from 'lucide-react'

const nav = [
  { to: '/clients', label: 'Clients', icon: Users },
  { to: '/import', label: 'Import', icon: Upload },
]

function navClass({ isActive }: { isActive: boolean }) {
  return `flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
    isActive ? 'bg-rose-100 text-rose-700' : 'text-gray-600 hover:bg-gray-100'
  }`
}

function tabClass({ isActive }: { isActive: boolean }) {
  return `flex flex-1 flex-col items-center gap-1 py-2 text-xs ${
    isActive ? 'text-rose-600' : 'text-gray-500'
  }`
}

export default function Layout() {
  return (
    <div className="min-h-screen bg-gray-50">
      {/* Sidebar (lg+) */}
      <aside className="fixed inset-y-0 left-0 hidden w-64 flex-col border-r border-gray-200 bg-white p-4 lg:flex">
        <div className="mb-8 flex items-center gap-2 px-2">
          <Sparkles className="h-6 w-6 text-rose-600" />
          <span className="text-lg font-bold text-gray-900">Salon CRM</span>
        </div>
        <nav className="flex flex-col gap-1">
          {nav.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} className={navClass}>
              <Icon className="h-4 w-4" />
              {label}
            </NavLink>
          ))}
        </nav>
      </aside>

      {/* Top bar (<lg) */}
      <header className="sticky top-0 z-20 flex items-center gap-2 border-b border-gray-200 bg-white px-4 py-3 lg:hidden">
        <Sparkles className="h-5 w-5 text-rose-600" />
        <span className="font-bold text-gray-900">Salon CRM</span>
      </header>

      <main className="mx-auto w-full max-w-7xl px-4 pb-24 pt-6 sm:px-6 lg:ml-64 lg:max-w-none lg:px-8 lg:pb-8">
        <Outlet />
      </main>

      {/* Bottom tab nav (<lg) */}
      <nav className="fixed inset-x-0 bottom-0 z-20 flex border-t border-gray-200 bg-white lg:hidden">
        {nav.map(({ to, label, icon: Icon }) => (
          <NavLink key={to} to={to} className={tabClass}>
            <Icon className="h-5 w-5" />
            {label}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}

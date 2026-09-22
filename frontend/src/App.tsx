import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import CalendarPage from './pages/CalendarPage'
import ClientsPage from './pages/ClientsPage'
import ClientFormPage from './pages/ClientFormPage'
import ClientDetailPage from './pages/ClientDetailPage'
import ImportPage from './pages/ImportPage'
import QueuePage from './pages/QueuePage'
import ServicesPage from './pages/ServicesPage'
import StaffPage from './pages/StaffPage'
import WhatsAppSimulatorPage from './pages/WhatsAppSimulatorPage'
import PublicTicketPage from './pages/PublicTicketPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/q/:id" element={<PublicTicketPage />} />
        <Route element={<Layout />}>
          <Route path="/" element={<Navigate to="/queue" replace />} />
          <Route path="/queue" element={<QueuePage />} />
          <Route path="/whatsapp" element={<WhatsAppSimulatorPage />} />
          <Route path="/calendar" element={<CalendarPage />} />
          <Route path="/services" element={<ServicesPage />} />
          <Route path="/staff" element={<StaffPage />} />
          <Route path="/clients" element={<ClientsPage />} />
          <Route path="/clients/new" element={<ClientFormPage />} />
          <Route path="/clients/:id" element={<ClientDetailPage />} />
          <Route path="/clients/:id/edit" element={<ClientFormPage />} />
          <Route path="/import" element={<ImportPage />} />
          <Route path="*" element={<Navigate to="/clients" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

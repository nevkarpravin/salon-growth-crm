import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import CalendarPage from './pages/CalendarPage'
import ClientsPage from './pages/ClientsPage'
import ClientFormPage from './pages/ClientFormPage'
import ClientDetailPage from './pages/ClientDetailPage'
import ImportPage from './pages/ImportPage'
import ServicesPage from './pages/ServicesPage'
import StaffPage from './pages/StaffPage'
import CheckoutPage from './pages/CheckoutPage'
import SalesPage from './pages/SalesPage'
import SaleDetailPage from './pages/SaleDetailPage'
import ProductsPage from './pages/ProductsPage'
import MorePage from './pages/MorePage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<Navigate to="/calendar" replace />} />
          <Route path="/calendar" element={<CalendarPage />} />
          <Route path="/services" element={<ServicesPage />} />
          <Route path="/staff" element={<StaffPage />} />
          <Route path="/clients" element={<ClientsPage />} />
          <Route path="/clients/new" element={<ClientFormPage />} />
          <Route path="/clients/:id" element={<ClientDetailPage />} />
          <Route path="/clients/:id/edit" element={<ClientFormPage />} />
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/checkout/:saleId" element={<CheckoutPage />} />
          <Route path="/sales" element={<SalesPage />} />
          <Route path="/sales/:id" element={<SaleDetailPage />} />
          <Route path="/products" element={<ProductsPage />} />
          <Route path="/more" element={<MorePage />} />
          <Route path="/import" element={<ImportPage />} />
          <Route path="*" element={<Navigate to="/calendar" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

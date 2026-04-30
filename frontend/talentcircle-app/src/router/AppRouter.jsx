import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAppStore } from '../store/useAppStore'
import Layout from '../components/Layout'
import Login from '../pages/Login/Login'
import Dashboard from '../pages/Dashboard/Dashboard'
import Drafts from '../pages/Drafts/Drafts'
import Executions from '../pages/Executions/Executions'
import Admin from '../pages/Admin/Admin'

function ProtectedRoute({ children }) {
  const isAuthenticated = useAppStore((s) => s.isAuthenticated)
  return isAuthenticated ? children : <Navigate to="/login" replace />
}

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard"  element={<Dashboard />} />
          <Route path="drafts"     element={<Drafts />} />
          <Route path="executions" element={<Executions />} />
          <Route path="admin"      element={<Admin />} />
        </Route>
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

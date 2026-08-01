import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { Toaster } from "sonner";
import { AuthProvider } from "./auth/AuthProvider";
import { PrivateRoute } from "./auth/PrivateRoute";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import Financas from "./pages/Financas";
import CarteiraDetalhe from "./pages/CarteiraDetalhe";
import Contas from "./pages/Contas";
import Tarefas from "./pages/Tarefas";
import Materias from "./pages/Materias";
import Metas from "./pages/Metas";
import Estudos from "./pages/Estudos";
import ComoFunciona from "./pages/ComoFunciona";
import Admin from "./pages/Admin";

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Toaster position="top-right" richColors closeButton duration={3500} />
        <Routes>
          <Route path="/" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
          <Route path="/auth/login" element={<Login />} />
          <Route path="/auth/register" element={<Register />} />
          <Route path="/dashboard" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
          <Route path="/financas" element={<PrivateRoute><Financas /></PrivateRoute>} />
          <Route path="/financas/carteiras/:id" element={<PrivateRoute><CarteiraDetalhe /></PrivateRoute>} />
          <Route path="/contas" element={<PrivateRoute><Contas /></PrivateRoute>} />
          <Route path="/tarefas" element={<PrivateRoute><Tarefas /></PrivateRoute>} />
          <Route path="/materias" element={<PrivateRoute><Materias /></PrivateRoute>} />
          <Route path="/metas" element={<PrivateRoute><Metas /></PrivateRoute>} />
          <Route path="/estudos" element={<PrivateRoute><Estudos /></PrivateRoute>} />
          <Route path="/como-funciona" element={<PrivateRoute><ComoFunciona /></PrivateRoute>} />
          <Route path="/admin" element={<PrivateRoute requireAdmin><Admin /></PrivateRoute>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

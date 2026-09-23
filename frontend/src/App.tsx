import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { Toaster } from "sonner";
import { AuthProvider } from "./auth/AuthProvider";
import { PrivateRoute } from "./auth/PrivateRoute";
import Login from "./pages/Login";
import Register from "./pages/Register";
import ForgotPassword from "./pages/ForgotPassword";
import ResetPassword from "./pages/ResetPassword";
import ActivateAccount from "./pages/ActivateAccount";
import Dashboard from "./pages/Dashboard";
import Financas from "./pages/Financas";
import CarteiraDetalhe from "./pages/CarteiraDetalhe";
import Contas from "./pages/Contas";
import Planejamento from "./pages/Planejamento";
import CarteiraIdeal from "./pages/CarteiraIdeal";
import AvaliacaoAtivos from "./pages/AvaliacaoAtivos";
import AvaliacaoAtivoDetalhe from "./pages/AvaliacaoAtivoDetalhe";
import ChecklistModelos from "./pages/ChecklistModelos";
import Pontuacao from "./pages/Pontuacao";
import ProximosAportes from "./pages/ProximosAportes";
import Tarefas from "./pages/Tarefas";
import Materias from "./pages/Materias";
import Treinos from "./pages/Treinos";
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
          <Route path="/auth/forgot-password" element={<ForgotPassword />} />
          <Route path="/auth/reset-password" element={<ResetPassword />} />
          <Route path="/auth/activate" element={<ActivateAccount />} />
          <Route path="/dashboard" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
          <Route path="/financas" element={<PrivateRoute><Financas /></PrivateRoute>} />
          <Route path="/financas/carteiras/:tipo/:id" element={<PrivateRoute><CarteiraDetalhe /></PrivateRoute>} />
          <Route path="/contas" element={<PrivateRoute><Contas /></PrivateRoute>} />
          <Route path="/planejamento" element={<PrivateRoute><Planejamento /></PrivateRoute>} />
          <Route path="/planejamento/carteira-ideal" element={<PrivateRoute><CarteiraIdeal /></PrivateRoute>} />
          <Route path="/planejamento/carteira-ideal/:carteiraId" element={<PrivateRoute><CarteiraIdeal /></PrivateRoute>} />
          <Route path="/avaliacao" element={<PrivateRoute><AvaliacaoAtivos /></PrivateRoute>} />
          <Route path="/avaliacao/modelos" element={<PrivateRoute><ChecklistModelos /></PrivateRoute>} />
          <Route path="/avaliacao/:ref" element={<PrivateRoute><AvaliacaoAtivoDetalhe /></PrivateRoute>} />
          <Route path="/planejamento/pontuacao" element={<PrivateRoute><Pontuacao /></PrivateRoute>} />
          <Route path="/planejamento/aportes" element={<PrivateRoute><ProximosAportes /></PrivateRoute>} />
          <Route path="/tarefas" element={<PrivateRoute><Tarefas /></PrivateRoute>} />
          <Route path="/materias" element={<PrivateRoute><Materias /></PrivateRoute>} />
          <Route path="/treinos" element={<PrivateRoute><Treinos /></PrivateRoute>} />
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

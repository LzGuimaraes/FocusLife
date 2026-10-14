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
import FinancasCarteiras from "./pages/FinancasCarteiras";
import ComprasVendas from "./pages/ComprasVendas";
import CarteiraDetalhe from "./pages/CarteiraDetalhe";
import Contas from "./pages/Contas";
import Planejamento from "./pages/Planejamento";
import CarteiraIdeal from "./pages/CarteiraIdeal";
import AvaliacaoAtivos from "./pages/AvaliacaoAtivos";
import AvaliacaoAtivoDetalhe from "./pages/AvaliacaoAtivoDetalhe";
import ChecklistModelos from "./pages/ChecklistModelos";
import NotasSubclasse from "./pages/NotasSubclasse";
import ProximosAportes from "./pages/ProximosAportes";
import Tarefas from "./pages/Tarefas";
import Materias from "./pages/Materias";
import Treinos from "./pages/Treinos";
import Metas from "./pages/Metas";
import Estudos from "./pages/Estudos";
import ComoFunciona from "./pages/ComoFunciona";
import Suporte from "./pages/Suporte";
import Admin from "./pages/Admin";
import AdminMusicas from "./pages/AdminMusicas";
import AdminUsuarioDetalhe from "./pages/AdminUsuarioDetalhe";
import AdminUsuarioRegistros from "./pages/AdminUsuarioRegistros";
import { useTema } from "./theme/ThemeProvider";

export default function App() {
  const { tema } = useTema();
  return (
    <AuthProvider>
      <BrowserRouter>
        <Toaster position="top-right" richColors closeButton duration={3500} theme={tema} />
        <Routes>
          <Route path="/" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
          <Route path="/auth/login" element={<Login />} />
          <Route path="/auth/register" element={<Register />} />
          <Route path="/auth/forgot-password" element={<ForgotPassword />} />
          <Route path="/auth/reset-password" element={<ResetPassword />} />
          <Route path="/auth/activate" element={<ActivateAccount />} />
          <Route path="/dashboard" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
          <Route path="/financas" element={<PrivateRoute><Financas /></PrivateRoute>} />
          <Route path="/financas/carteiras" element={<PrivateRoute><FinancasCarteiras /></PrivateRoute>} />
          <Route path="/financas/compras-vendas" element={<PrivateRoute><ComprasVendas /></PrivateRoute>} />
          <Route path="/financas/carteiras/:tipo/:id" element={<PrivateRoute><CarteiraDetalhe /></PrivateRoute>} />
          <Route path="/contas" element={<PrivateRoute><Contas /></PrivateRoute>} />
          <Route path="/planejamento" element={<PrivateRoute><Planejamento /></PrivateRoute>} />
          <Route path="/planejamento/carteira-ideal" element={<PrivateRoute><CarteiraIdeal /></PrivateRoute>} />
          <Route path="/planejamento/carteira-ideal/:carteiraId" element={<PrivateRoute><CarteiraIdeal /></PrivateRoute>} />
          <Route path="/avaliacao" element={<PrivateRoute><AvaliacaoAtivos /></PrivateRoute>} />
          <Route path="/avaliacao/modelos" element={<PrivateRoute><ChecklistModelos /></PrivateRoute>} />
          <Route path="/avaliacao/notas" element={<PrivateRoute><NotasSubclasse /></PrivateRoute>} />
          <Route path="/avaliacao/:ref" element={<PrivateRoute><AvaliacaoAtivoDetalhe /></PrivateRoute>} />
          <Route path="/planejamento/aportes" element={<PrivateRoute><ProximosAportes /></PrivateRoute>} />
          <Route path="/tarefas" element={<PrivateRoute><Tarefas /></PrivateRoute>} />
          <Route path="/materias" element={<PrivateRoute><Materias /></PrivateRoute>} />
          <Route path="/treinos" element={<PrivateRoute><Treinos /></PrivateRoute>} />
          <Route path="/metas" element={<PrivateRoute><Metas /></PrivateRoute>} />
          <Route path="/estudos" element={<PrivateRoute><Estudos /></PrivateRoute>} />
          <Route path="/como-funciona" element={<PrivateRoute><ComoFunciona /></PrivateRoute>} />
          <Route path="/suporte" element={<PrivateRoute><Suporte /></PrivateRoute>} />
          <Route path="/admin" element={<PrivateRoute requireAdmin><Admin /></PrivateRoute>} />
          <Route path="/admin/musicas" element={<PrivateRoute requireAdmin><AdminMusicas /></PrivateRoute>} />
          <Route path="/admin/usuarios/:id" element={<PrivateRoute requireAdmin><AdminUsuarioDetalhe /></PrivateRoute>} />
          <Route path="/admin/usuarios/:id/tarefas" element={<PrivateRoute requireAdmin><AdminUsuarioRegistros key="tarefas" modulo="tarefas" /></PrivateRoute>} />
          <Route path="/admin/usuarios/:id/metas" element={<PrivateRoute requireAdmin><AdminUsuarioRegistros key="metas" modulo="metas" /></PrivateRoute>} />
          <Route path="/admin/usuarios/:id/estudos" element={<PrivateRoute requireAdmin><AdminUsuarioRegistros key="estudos" modulo="estudos" /></PrivateRoute>} />
          <Route path="/admin/usuarios/:id/investimentos" element={<PrivateRoute requireAdmin><AdminUsuarioRegistros key="investimentos" modulo="investimentos" /></PrivateRoute>} />
          <Route path="/admin/usuarios/:id/contas" element={<PrivateRoute requireAdmin><AdminUsuarioRegistros key="contas" modulo="contas" /></PrivateRoute>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

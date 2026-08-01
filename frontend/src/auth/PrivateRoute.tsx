import { Navigate } from "react-router-dom";
import { useAuth } from "./AuthProvider";
import type { JSX } from "react";

export function PrivateRoute({ children, requireAdmin }: { children: JSX.Element; requireAdmin?: boolean }) {
  const { user, loading } = useAuth();

  if (loading) return <div>Carregando...</div>;
  if (!user) return <Navigate to="/auth/login" />;
  if (requireAdmin && user.role !== "ADMIN") return <Navigate to="/" replace />;
  return children;
}

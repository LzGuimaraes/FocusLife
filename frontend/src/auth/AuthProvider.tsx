import { createContext, useContext, useState, useEffect } from "react";
import type { ReactNode } from "react";
import api from "../api/api";

interface AuthContextType {
  user: any | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType>({} as AuthContextType);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<any | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchUser = async () => {
    try {
      const res = await api.get("/users/me");
      setUser(res.data);
      return true;
    } catch (error) {
      setUser(null);
      return false;
    }
  };

  useEffect(() => {
    const checkAuth = async () => {
      setLoading(true);
      await fetchUser();
      setLoading(false);
    };
    checkAuth();
  }, []);

  const login = async (email: string, password: string) => {
    try {
      // Faz o login
      await api.post("/auth/login", { email, password });
      
      // Busca os dados do usuário após o login
      await fetchUser();
    } catch (error) {
      console.error("Erro no login:", error);
      throw error; // Repassa o erro para ser tratado no componente
    }
  };

  const logout = async () => {
    try {
      // Faz o logout no backend
      await api.post("/auth/logout");
    } catch (error) {
      console.error("Erro ao fazer logout:", error);
    } finally {
      // Limpa o estado do usuário independentemente do resultado
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
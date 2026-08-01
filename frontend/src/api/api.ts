import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  withCredentials: true,
});

/* ── Observabilidade: rastreia cada requisição ──
   Se um POST/PUT aparecer como "ERR_CANCELED", o navegador ABORTOU a
   requisição antes de chegar ao backend (ex.: submit nativo de <form>). */
api.interceptors.request.use((config) => {
  console.log(`[API →] ${config.method?.toUpperCase()} ${config.url}`, config.data ?? "");
  return config;
});

api.interceptors.response.use(
  (res) => {
    console.log(`[API ←] ${res.config.method?.toUpperCase()} ${res.config.url} ${res.status}`);
    return res;
  },
  (err) => {
    console.warn(
      `[API ✗] ${err.config?.method?.toUpperCase() ?? "?"} ${err.config?.url ?? "?"} → ${err.code ?? err.message}`,
      err.response ? `HTTP ${err.response.status}` : "sem resposta do backend"
    );
    return Promise.reject(err);
  }
);

export default api;

# FocusLifeHubFront

Frontend do projeto FocusLifeHub, desenvolvido como uma SPA (Single Page Application) usando React + TypeScript e empacotado com Vite. O roteamento é feito com React Router e o fluxo de autenticação utiliza contexto e JWT.

## Tecnologias
- React 19 (react, react-dom)
- Vite 7
- TypeScript 5.9
- React Router DOM 7
- Axios
- jwt-decode
- ESLint e plugins (hooks/refresh)

## Estrutura de Pastas
- `src/main.tsx`: ponto de entrada da aplicação
- `src/App.tsx`: layout, composição e organização de rotas
- `src/pages/`: páginas da aplicação
- `src/auth/AuthProvider.tsx`: provedor de autenticação via contexto
- `src/auth/PrivateRoute.tsx`: componente para proteger rotas
- `src/api/api.ts`: cliente HTTP centralizado (Axios)
- `assets/`, `index.css`: recursos e estilos
- `vite.config.ts`, `tsconfig.*`: configuração de build e TypeScript

## Scripts do Projeto
- `npm run dev`: inicia o servidor de desenvolvimento (HMR)
- `npm run build`: compila TypeScript e gera a pasta `dist`
- `npm run preview`: serve o build gerado para validação
- `npm run lint`: verifica padrões de código (ESLint)

## Como Executar
Pré-requisitos: Node.js 18+ e npm (ou PNPM/Yarn, se preferir).
1. Instalação: `npm install`
2. Desenvolvimento: `npm run dev` (abra a URL exibida no terminal)
3. Build: `npm run build` (gera `dist/`)
4. Preview do build: `npm run preview`

## Autenticação e Rotas
- `AuthProvider` expõe estado do usuário e token via contexto.
- `PrivateRoute` restringe acesso a rotas que exigem login.
- `jwt-decode` auxilia na leitura de claims do token.
- Defina suas rotas no `App.tsx` e utilize `PrivateRoute` onde necessário.

## Consumo de API
- `src/api/api.ts` centraliza a configuração do Axios (baseURL, headers, interceptors).
- Importe funções de API nas páginas para buscar/enviar dados.
- Configure variáveis de ambiente (ex.: `VITE_API_URL`) em arquivos `.env` (Vite usa prefixo `VITE_`).

## Qualidade e Configuração
- ESLint e plugins garantem boas práticas de hooks e atualização.
- TypeScript configurado em `tsconfig.app.json` e `tsconfig.node.json`.
- Priorize tipos fortes, componentes funcionais e hooks.

## Deploy
- O conteúdo de `dist/` pode ser servido em qualquer host estático (Nginx, Apache, Netlify, Vercel, etc.).
- Use `npm run preview` para validar o build localmente.

Nome do pacote: "foslifehub" (package.json).

## Contribuição
Crie branches, abra PRs e mantenha o padrão de código e organização.

## Licença
Defina a licença do projeto (ex.: MIT) conforme necessidade.

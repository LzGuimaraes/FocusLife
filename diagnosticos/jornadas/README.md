# Verificação no navegador

`browser-smoke.cjs` testa fluxos reais de Jornadas em serviços **locais de teste**: login, cadastro de livro, página acumulada, correção, materiais, sessão manual, cronômetro após recarregamento, Escape no modal e rotas antigas. Também captura desktop claro e celular escuro e verifica transbordamento e erros JavaScript.

Requer Playwright instalado para testes (não é dependência da aplicação), Chromium/Chrome e backend/frontend ligados a um banco descartável. Crie uma conta de teste ativa antes de executar; o script não envia e-mails nem prepara a conta. Ele cria uma atividade com nome único por execução e mantém os registros no banco de teste para inspeção.

```bash
PLAYWRIGHT_PATH=/caminho/para/playwright \
CHROME_PATH=/caminho/para/chrome \
JORNADAS_APP_URL=http://localhost:5179 \
JORNADAS_API_URL=http://localhost:18089 \
JORNADAS_TEST_EMAIL=conta-de-teste@example.test \
JORNADAS_TEST_PASSWORD='senha-da-conta-de-teste' \
node diagnosticos/jornadas/browser-smoke.cjs
```

Se Playwright estiver disponível no `node_modules`, omita `PLAYWRIGHT_PATH`. Se usar o Chromium instalado pelo Playwright, omita `CHROME_PATH`. Capturas ficam em `/tmp`; altere com `JORNADAS_ARTIFACTS`.

Resultados e regras de negócio: [JORNADAS.md](../../JORNADAS.md). O teste de navegador usa dados de outubro/novembro de 2026; as previsões exatas são verificadas no backend com relógio fixo.

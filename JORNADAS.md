# Jornadas

Jornadas substitui Tarefas, Matérias e Metas pessoais. Uma atividade representa qualquer quantidade a realizar: aulas, exercícios, páginas, horas, unidades ou outra medida. Atividades simples usam total 1. Agrupamentos são texto livre; não exigem outro cadastro. Metas financeiras de Planejamento não participam dessa unificação.

## Modelo e histórico

- `jornada_atividade`: usuário proprietário, título, descrição, início/prazo opcionais, unidade, possibilidade de frações, total, saldo inicial e data de referência, status, prioridade, horário, agrupamento e versão.
- `jornada_dia`: dias da semana planejados; coleção vazia significa todos os dias.
- `jornada_progresso`: data, modo `INCREMENTO` ou `ACUMULADO`, quantidade, observação, motivação opcional de 1 a 5 e UUID idempotente.
- `jornada_material`: título, URL HTTP/HTTPS, descrição e atividade opcional, sempre com proprietário. Não há requisição para buscar conteúdo da URL.
- `sessao_estudo` e `tb-estudos`: preservam cronômetro, duração manual e anotações, agora vinculados por `atividade_id`.

O total realizado **não é uma coluna editável nem um cache**: é obtido reproduzindo o saldo inicial e os lançamentos, ordenados por **data e ID crescente**. Dentro do mesmo dia, a ordem de criação resolve a sequência. Um incremento soma ao saldo anterior; um acumulado substitui o saldo naquele ponto. Exemplo: 100 páginas acumuladas, +5 páginas e depois 120 acumuladas resultam em **120**, não 225. Um incremento lançado retroativamente antes do acumulado pode ser absorvido por esse ponto de referência; isso é intencional.

Correções e exclusões reproduzem toda a sequência. Acumulados menores que o saldo anterior, progresso acima do total ou frações em unidades indivisíveis são recusados, com rollback. Para reduzir o acumulado real, corrija os lançamentos anteriores responsáveis pelo excesso. Quantidades têm até quatro casas decimais. O total, quando informado, deve ser maior que zero.

O saldo inicial é uma fotografia **ao fim da data de referência**, sem distribuir produção por dias desconhecidos. Novos lançamentos devem ser posteriores a essa data. Para saldo zero e início conhecido, a referência padrão é a véspera do início. Para saldo já realizado, informe a referência real; para registrar produção de hoje separadamente, use o saldo conhecido ao fim de um dia anterior. O saldo inicial pode ser corrigido no cadastro, desde que o histórico continue válido. Alterar a unidade não converte automaticamente quantidades existentes: informe os valores reais na nova medida.

Ao atingir o total pelo histórico, a atividade é concluída automaticamente; corrigir ou excluir a produção pode reabri-la. Marcar como concluída sem atingir o total é recusado. A data de conclusão vem do primeiro ponto do histórico que atinge o total; para atividades cujo saldo inicial já está completo, a data real permanece desconhecida. Uma atividade cancelada precisa ser reativada para receber novos avanços. O realizado de uma atividade não iniciada deve ser zero.

## Convenção de datas e previsões

Todos os indicadores são calculados no backend por `CalculadoraJornada`. O serviço usa um `Clock` injetável, sempre em **America/Cuiaba**. Não recebe do navegador uma data que altere os cálculos; os testes controlam o relógio.

A referência é o **fim do dia atual**:

1. Início e prazo contam como oportunidades, inclusive quando são o mesmo dia.
2. O período transcorrido inclui o dia atual: `[início, min(hoje, prazo)]`.
3. O período futuro começa amanhã: `[max(amanhã, início), prazo]`.
4. O período observado começa no dia seguinte ao saldo inicial e inclui hoje. Dias sem produção também contam.
5. Com dias da semana selecionados, contam somente as oportunidades daqueles dias. Produção informada fora desses dias é preservada; o denominador continua sendo as oportunidades planejadas. Sem dias selecionados, usam-se dias corridos.

Indicadores:

| Indicador | Regra |
|---|---|
| Realizado | Replay do saldo inicial e do histórico até hoje |
| Percentual | `realizado / total × 100` |
| Restante | `total − realizado` |
| Esperado | `total × oportunidades transcorridas / oportunidades do período` |
| Diferença | `realizado − esperado` |
| Ritmo observado | `(realizado − saldo inicial) / oportunidades observadas` |
| Projeção no prazo | `realizado + ritmo observado × oportunidades futuras até o prazo` |
| Conclusão estimada | Última oportunidade necessária após hoje para executar `ceil(restante / ritmo observado)` |
| Necessário por dia | `restante / oportunidades futuras até o prazo` |
| Necessário por semana | Necessário diário × 7, ou × quantidade de dias planejados por semana |

**Posição** (`ADIANTADA`, `NO_PLANEJADO`, `ATRASADA`) compara realizado e esperado hoje. **Tendência** compara a projeção com o total no prazo. É possível estar adiantado e ter um ritmo observado insuficiente. O esperado usa o período completo do cadastro; não é recalibrado automaticamente a cada lançamento ou saldo inicial.

Um saldo inicial isolado, zero produção ou ausência de oportunidades observadas não gera ritmo positivo nem previsão. A interface mostra **“Sem dados suficientes para estimar”** e apresenta o esforço necessário quando calculável. Para unidades indivisíveis, a recomendação prática é arredondada para cima; médias diárias e semanais decimais permanecem como referência. Projeções quantitativas não são limitadas artificialmente ao total.

No prazo hoje, a fotografia já inclui hoje: não existe outra oportunidade futura até o prazo. A interface informa o saldo a registrar hoje, sem divisão por zero. Prazo vencido não gera projeção no prazo nem esforço impossível; pode haver uma estimativa de conclusão futura quando há ritmo suficiente. Antes do início, o esperado é zero e a tendência informa início futuro. Sem oportunidades planejadas no período, o esperado é indisponível. Datas/quantidades ausentes mantêm a atividade acessível, com indicadores indisponíveis onde faltam dados.

Pausar ou cancelar suspende projeções e recomendações futuras, mantendo o saldo e a posição visíveis. **O prazo não é estendido.** Os dias de pausa continuam na janela de observação ao retomar: a média representa o ritmo efetivo do período, inclusive inatividade. Não há dedução de motivação com base em produtividade. A motivação mostrada é somente aquela informada em cada lançamento.

Períodos novos são limitados a 100 anos. Estimativas que exigiriam mais de 36.525 oportunidades ficam indisponíveis, evitando datas desproporcionais. O gráfico é SVG, sem nova biblioteca, com até cerca de 200 pontos de referência; não desenha produção desconhecida anterior a um saldo inicial positivo.

### Cenário de aceitação

Início **15/10/2026**, prazo **15/11/2026**, referência ao fim de **05/11/2026**, total **20 unidades**, saldo inicial zero na véspera do início, +2 em 18/10 e +2 em 05/11:

| Medida | Resultado |
|---|---:|
| Oportunidades no período | 32 dias |
| Observadas até 05/11 | 22 dias |
| Futuras até o prazo | 10 dias |
| Realizado | 4 (20%) |
| Esperado | 13,75 |
| Diferença | −9,75 |
| Ritmo observado | 4/22 ≈ 0,1818 por dia |
| Projeção no prazo | ≈ 5,8182 |
| Necessário diário | 1,6; recomendação prática: 2 |
| Necessário semanal | 11,2; referência prática: 12 |
| Conclusão estimada | 01/02/2027 |

O total 20 é exclusivo desse cenário de teste.

## Migração V40

Nenhuma migration anterior foi editada. A V40 cria o domínio unificado e copia registros que possuem um usuário existente. `origem` + `origem_id` preservam a identificação anterior, distinguindo IDs iguais dos três módulos.

- Tarefa: total 1, unidade `unidades`, saldo 1 se concluída e 0 nos demais casos; prioridade, prazo e horário preservados. Não é inventado um início.
- Meta pessoal: progresso válido de 0–100 preservado como saldo em unidade `%`, total 100. Não representa páginas, aulas ou outra quantidade física. O valor original fica também em `percentual_legado`. Percentuais ausentes ou inválidos ficam sem total calculável e preservam o valor original para correção, sem truncamento silencioso.
- Matéria: título, descrição e dias planejados preservados. Início, prazo, total e unidade ficam desconhecidos até o usuário completar o cadastro.
- Sessões e anotações: os mesmos registros recebem `atividade_id`; IDs, datas, conteúdo, duração e sessões abertas são mantidos. O cronômetro recupera sessões abertas após navegação/recarregamento. Duração manual e cronômetro são mostrados separadamente, evitando somá-los quando poderiam descrever o mesmo estudo.
- Logs de tarefas já entregues são associados ao novo ID de atividade como `ATIVIDADE_DIA`, evitando repetir o aviso no dia da migração.

O saldo migrado é conhecido na data local da migração; isso não é uma data de produção nem de conclusão. Não são criados lançamentos fictícios.

As tabelas `tb_tarefas`, `tb_metas`, `tb_materia` e `materia_dias_semana` continuam como arquivo para conferência. Os modelos antigos dão suporte a esse arquivo e a contratos administrativos antigos de leitura. Não recebem novas escritas pelas APIs pessoais: `/tarefas/**`, `/metas/**`, `/materias/**` e `/estudos/**` retornam **410** após autenticação. A SPA redireciona seus caminhos antigos a Jornadas; referências administrativas antigas da SPA preservam o usuário-alvo e vão à consulta unificada. Não são mantidos três domínios ativos por trás da nova tela.

Registros antigos sem proprietário válido e estudos órfãos permanecem no arquivo original e não são atribuídos a outra conta. As verificações da migração garantem os vínculos de estudos com matérias migradas. Antes de qualquer remoção futura do arquivo, confira as contagens por origem/usuário e trate esses órfãos. A entrega não remove o arquivo nem limpa outros módulos.

Excluir uma atividade remove seus lançamentos, sessões e anotações após confirmação explícita; seus materiais tornam-se recursos gerais. Para exclusão de usuário, as novas FKs usam cascata para atividades, progresso e materiais. As restrições preexistentes de outros módulos continuam podendo impedir a exclusão da conta; não há limpeza financeira implícita.

## API e concorrência

Todas as rotas `/jornadas` usam o usuário do JWT validado, nunca `userId` informado no corpo. Atividades, progresso, materiais e sessões de terceiros retornam 404.

| Método e caminho | Função |
|---|---|
| `GET /jornadas` | Atividades com indicadores do proprietário |
| `POST /jornadas` | Cadastro |
| `GET /jornadas/{id}` | Detalhe com histórico, gráfico, materiais e estudos |
| `PUT /jornadas/{id}` | Edição completa do cadastro, com `version` |
| `DELETE /jornadas/{id}?version=N` | Exclusão explícita; exige cronômetro pausado |
| `POST /jornadas/{id}/progresso` | Lançamento com `version` e `requestId` UUID |
| `PUT /jornadas/{id}/progresso/{progressoId}` | Correção, com `version` |
| `DELETE /jornadas/{id}/progresso/{progressoId}?version=N` | Exclusão e recálculo |
| `GET/POST /jornadas/materiais` | Listagem/cadastro de materiais |
| `PUT/DELETE /jornadas/materiais/{id}` | Edição com `version` no corpo; exclusão com versão na query |
| `POST /jornadas/{id}/sessoes` | Inicia ou recupera cronômetro aberto |
| `PUT /jornadas/{id}/sessoes/{sessaoId}/pausar` | Encerra sessão; repetição é idempotente |
| `POST /jornadas/{id}/estudos` | Sessão manual com nome, data, duração em minutos e notas |
| `PUT/DELETE /jornadas/{id}/estudos/{notaId}` | Edita/exclui sessão manual |
| `GET /admin/users/{usuarioId}/jornadas[/{id}]` | ADMIN somente leitura, sempre limitado ao usuário-alvo |

Cadastro de exemplo (datas são dados do usuário):

```json
{
  "titulo": "Ler um livro",
  "inicio": "2026-10-15",
  "prazo": "2026-11-15",
  "unidade": "páginas",
  "fracionavel": false,
  "total": 350,
  "saldoInicial": 0,
  "status": "NAO_INICIADA",
  "diasSemana": ["MONDAY", "WEDNESDAY", "FRIDAY"],
  "agrupamento": "Leitura"
}
```

Lançamento: `{ "version": 0, "requestId": "UUID", "data": "2026-11-05", "modo": "ACUMULADO", "quantidade": 120, "motivacao": 4 }`. `motivacao` e `observacao` são opcionais. Link: `{ "titulo": "Material", "url": "https://example.com", "descricao": "Opcional", "atividadeId": null }`.

Operações de cadastro e histórico são transacionais. Escritas de progresso e cronômetro bloqueiam a atividade proprietária; uma versão antiga é recusada com **409**, sem perder atualizações. UUID repetido no cadastro de um lançamento devolve o resultado já persistido. Materiais usam versão otimista. A interface bloqueia submits simultâneos e orienta a recarregar em conflitos.

A listagem atual retorna todas as atividades da conta e aplica busca/filtros no cliente; em contas muito grandes, paginação e consultas agregadas são uma evolução de desempenho. As regras quantitativas permanecem no backend.

## Lembretes e configuração

Não é necessária nova dependência ou serviço. Aplique o backend e frontend juntos; o Flyway aplica V40 na inicialização. As configurações existentes de PostgreSQL, autenticação, SMTP e `APP_FRONTEND_URL` continuam necessárias.

`JornadasNotificationJob` verifica a cada 15 minutos em America/Cuiaba. Sem horário: a partir das 08h no dia do prazo; com horário: primeira verificação após ele, nos dias planejados e dentro do período. Atividades pausadas/canceladas/concluídas e usuários inativos não recebem aviso. Um log por atividade/data evita novos envios após sucesso. Falhas SMTP não apagam dados nem impedem outros destinatários. Uma falha após envio SMTP e antes da gravação do log ainda pode repetir um aviso, como no fluxo de contas existente.

A propriedade opcional `app.jornadas.notifications.cron` altera a frequência. O job financeiro continua separado; a ação administrativa de executar notificações chama ambos. Não houve envio de e-mail real nos testes.

## Verificação

- `cd frontend && npm run build`.
- `cd backend && ./mvnw -Dtest='CalculadoraJornadaTest,JornadasPersistenciaTest,JornadasControllerTest' test`: regras, ORM real em H2, segurança MVC e concorrência de versões, sem banco da aplicação.
- Os testes `JornadasMigracaoTest` exigem PostgreSQL dedicado. Defina `JORNADAS_TEST_DB_URL`, `JORNADAS_TEST_DB_USER` e, se necessário, `JORNADAS_TEST_DB_PASSWORD`, e execute `./mvnw -Dtest=JornadasMigracaoTest test`. Cada teste usa um schema aleatório próprio, removido ao terminar. Sem a URL, esses testes ficam explicitamente ignorados.
- `./mvnw verify` também carrega o contexto completo e precisa de um PostgreSQL de teste configurado para Spring. Ao executar localmente, sobrescreva `spring.config.import` e a conexão para evitar usar a base do `.env`. Configure SMTP de teste; não use destinatários reais.

Os testes cobrem o cenário de datas acima, páginas acumuladas, exercícios incrementais, atividade simples, saldo inicial sem produção conhecida, correção/exclusão, retroativos, frações, dias planejados, pausa/cancelamento, prazo vencido/hoje, conclusão antes/depois do prazo, links e isolamento, idempotência, 409, migração com sessões abertas e notificações.

A verificação de navegador reutilizável está em [diagnosticos/jornadas](diagnosticos/jornadas/README.md) e usa serviços reais locais, uma conta de teste e um banco descartável, sem fixtures de API na interface.

### Resultado da implementação (05/11/2026)

- Backend: `clean verify` e verificação final aprovados, **237 testes**, sem falhas, erros ou testes ignorados. Inclui 32 testes novos de Jornadas e PostgreSQL temporário para V40 e notificações.
- Frontend: TypeScript e build Vite aprovados; permanece o aviso de bundle acima de 500 kB.
- Navegador com backend real: cadastro, acumulado, correção, links, anotações, recuperação/pausa de cronômetro, desktop claro, celular escuro sem transbordamento, Escape e quatro rotas antigas aprovados.
- ESLint dos arquivos alterados: aprovado. O lint global ainda encontra **61 erros preexistentes**, confirmados na versão anterior; não foi realizada limpeza geral de outros módulos.
- Migração aplicada e verificada somente em bancos descartáveis. Nenhuma conexão foi feita ao banco configurado no `.env` para aplicar alterações, e nenhum e-mail real foi enviado.

### Controles da interface

A listagem também permite excluir uma atividade diretamente no card, com confirmação explícita. Os campos de data usam calendário estilizado com navegação por teclado e limites de início/prazo; seletores e dias planejados acompanham os temas claro/escuro. Os componentes estão em `frontend/src/components/PickerFields.tsx`, sem nova dependência. O roteiro `diagnosticos/jornadas/controls-smoke.cjs` verifica esses controles e a exclusão real de uma atividade de teste local.

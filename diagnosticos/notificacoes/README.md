# Diagnóstico das notificações agendadas — 13/10/2026

## Relato e causa reproduzida

O usuário informou que, em produção, os e-mails de cadastro e recuperação chegam normalmente, enquanto os agendados não chegam. O scheduler está habilitado por `@EnableScheduling` em `SecurityConfig`.

O job usava `findByPagoFalseAndDataVencimento` e `findByPrazoAndStatusNot`, sem carregar os destinatários. `DespesaModel.carteiraDividas`, `CarteiraModel.user` e `TarefasModel.user` são relações LAZY. Fora da requisição HTTP, o job não mantém uma sessão para acessar os vínculos depois da consulta.

O agrupamento por usuário das contas tenta acessar a carteira/usuário antes do envio, e a montagem das mensagens das tarefas acessa o usuário. Uma `LazyInitializationException` interrompe esses caminhos. O job captura o erro e apenas registra a falha; isso não impede os e-mails de autenticação, que usam outro fluxo.

Um teste com Hibernate, H2 e os repositórios reais reproduziu a exceção para contas e tarefas depois de fechar o EntityManager. As novas consultas foram verificadas no mesmo cenário, e os destinatários continuaram acessíveis sem sessão aberta.

Esse é um defeito confirmado no código. Não houve acesso aos logs da VPS nem comprovação de entrega de um e-mail em produção nesta investigação.

## Correções locais

- Consultas próprias de notificações carregam conta → carteira → usuário e tarefa → usuário na mesma consulta, com `join fetch`.
- A seleção exclui contas pagas, saldos, usuários desativados, tarefas canceladas/concluídas e avisos já registrados para o mesmo usuário, tipo, referência e data.
- Despesas antigas com `pago` nulo são tratadas como não pagas. Na correção inicial, a seleção era limitada à data do vencimento; a ampliação para contas atrasadas foi solicitada e implementada na etapa descrita ao final.
- O envio começa às 08:00 e é repetido a cada hora até 23:00, no fuso America/Cuiaba. A repetição busca apenas referências ainda não notificadas.
- Ao iniciar o backend depois das 08:00, há uma tentativa de recuperar os avisos do dia. Uma inicialização antes das 08:00 aguarda o agendamento.
- O relógio do job usa explicitamente America/Cuiaba para a data e os registros, inclusive se o relógio UTC já estiver no dia seguinte.
- Chamadas simultâneas são ignoradas enquanto o job está executando na mesma instância.
- `email_log` é gravado em lote depois que o SMTP aceita o envio; uma falha de envio deixa as referências disponíveis para nova tentativa.
- Timeouts de conexão, leitura e escrita do SMTP são de 10 segundos. O scheduler tem dois workers para que um envio lento não ocupe a única thread de todos os agendamentos.

## Validação

**14 testes passaram:** oito do job e seis dos repositórios. Incluem a reprodução da falha original, consultas sem sessão aberta, filtros de seleção, deduplicação, falha de um destinatário sem bloquear os outros, nova tentativa após falha SMTP, retomada na inicialização, horário local e chamadas simultâneas.

H2 é uma dependência apenas de testes. O teste de repositórios cria um banco em memória com URL fixa; não usa o datasource da aplicação, não consulta o banco de produção e não envia e-mails reais.

```sh
cd backend
./mvnw -o -Dtest='DailyNotificationJobTest,NotificationRepositoryTest' test
```

No sandbox deste ambiente, o Mockito precisou ser iniciado como agente da JVM para evitar autoanexação bloqueada. A execução validada acrescentou:

```text
-DargLine=-javaagent:/home/luiz-fernando/.m2/repository/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar
```

## Conferência após atualizar o backend

O deploy precisa incluir a nova versão do backend. O endpoint `POST /admin/jobs/daily/run` executa o envio real e continua restrito a ADMIN; seu retorno apenas confirma a execução do job, não a entrega pelo provedor.

Nos logs, verificar:

- `Job diário executado` com a data correta de Cuiabá;
- `Notificações de contas` e `Notificações de tarefas` com as quantidades selecionadas;
- mensagens de envio aceito pelo SMTP ou os erros específicos de consulta/envio;
- ausência de `LazyInitializationException` nesse fluxo.

No banco, `email_log` registra quais referências foram notificadas. Não é necessário apagar esses registros para que uma falha SMTP seja tentada novamente, porque a falha não grava sucesso.

A deduplicação cobre os envios registrados e as chamadas concorrentes na mesma instância. Se o SMTP aceitar a mensagem e o banco falhar ao gravar o registro, uma tentativa posterior ainda pode repetir o aviso. A confirmação de aceitação pelo SMTP também não comprova a chegada à caixa de entrada.

As correções permanecem locais nesta etapa; não houve publicação nem execução de envios reais para usuários.

## Estilização e inclusão de contas atrasadas

Após a correção inicial, o usuário pediu o e-mail com as cores do sistema e escolheu incluir contas já vencidas nos avisos.

- A seleção de contas usa `dataVencimento <= data do aviso`. Futuras, pagas, saldos e usuários desativados continuam excluídos.
- O log usa a data do **aviso**, não a data de vencimento. Isso permite lembrar uma conta atrasada uma vez por dia até o pagamento, sem repetir a mensagem a cada execução horária.
- O assunto diferencia **Contas atrasadas**, **Contas vencendo hoje** e **Contas pendentes** quando há ambos os casos.
- O HTML usa indigo `#6366f1`, fundo `#f1f5f9`, cartão branco, alertas de atraso em vermelho e contas que vencem hoje em âmbar, seguindo os tokens de `frontend/src/index.css`.
- O e-mail mostra totais por moeda, nome da conta, carteira, vencimento em `dd/MM/yyyy`, dias de atraso e botão **Ver minhas contas**. O link usa `app.frontend.url` e abre `/contas`.
- O formato usa tabelas e estilos inline. Nomes de contas e carteiras são escapados para não serem interpretados como HTML.

Prévias com dados fictícios:

- [Contas atrasadas](previa-contas-atrasadas.html)
- [Atrasadas e contas vencendo hoje](previa-contas-pendentes.html)

Para regenerar as prévias a partir do código, execute na raiz do repositório após compilar o backend:

```sh
javac -cp backend/target/classes -d /tmp/previa-email-contas diagnosticos/notificacoes/PreviaEmailContas.java
java -cp backend/target/classes:/tmp/previa-email-contas dev.LzGuimaraes.FocusLifeHub.Email.PreviaEmailContas
```

Validação desta etapa: **20 testes passaram** (8 do job, 7 dos repositórios e 5 do template). Os testes verificam a seleção de atrasadas, exclusão de futuras/pagas, um aviso por dia até o pagamento, assuntos, datas, valores, escape de HTML e totais separados por moeda. As prévias foram renderizadas no Chrome em largura de 900px e 390px; isso não substitui a conferência em clientes de e-mail específicos.

Nenhum e-mail real foi enviado durante os testes. A estilização e a ampliação para contas atrasadas estão locais nesta etapa.

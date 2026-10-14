# Diagnóstico do aporte — 13/10/2026

Os defeitos abaixo foram identificados na investigação inicial. A seção final registra as correções implementadas posteriormente no código local.

## Caso informado pelo usuário

Aporte: **R$ 1.500,00**. Alocado: **R$ 1.074,32**. Não alocado: **R$ 425,68**.

| Ativo elegível | Capacidade antes de distribuir | Sugestão | Restrição restante |
| --- | ---: | ---: | --- |
| BTC | R$ 96,54 | R$ 96,54 | Capacidade esgotada |
| Caixa PICPAY | R$ 977,78 | R$ 977,78 | Capacidade herdada da classe/subclasse esgotada |
| VALE3 | R$ 10,45 | R$ 0,00 | Uma ação custa R$ 67,16 e ultrapassaria a capacidade |

Os outros 15 ativos informados foram descartados por limite atingido. Com esses candidatos e capacidades, **R$ 1.074,32 é o máximo executável nas regras atuais**.

Antes do arredondamento, a soma das capacidades é R$ 1.084,77. Portanto, R$ 415,23 já não encontram capacidade. Os R$ 10,45 da VALE3 também ficam sem aplicação porque não permitem comprar uma ação inteira. A soma é R$ 425,68.

Um cenário isolado com as capacidades e o preço da VALE3 fornecidos reproduziu exatamente o resultado:

```text
aporte=1500 alocado=1074.32 naoAlocado=425.68 troco=10.45
```

Essa reprodução usa os valores finais de capacidade como entrada do alocador. Não reconstrói a carteira, o patrimônio, as metas ou a elegibilidade a partir do banco. A conclusão depende de a tabela fornecida listar todos os candidatos elegíveis.

## Erro confirmado na explicação

Em `backend/src/main/java/dev/LzGuimaraes/FocusLifeHub/Planejamento/Aporte/AporteService.java:640`, `explicacaoNaoAlocado` identifica um ativo elegível com espaço residual e preço conhecido e atribui **toda a sobra** a troco de arredondamento.

O método não verifica se o saldo disponível permite comprar uma unidade, se a capacidade residual permite a compra, se o ativo exige unidades inteiras ou se os limites da classe/subclasse impedem a aplicação. Também seleciona o primeiro preço encontrado, apesar de chamá-lo de preço da cota mais barata.

No caso informado, R$ 425,68 permitem comprar seis ações de R$ 67,16. Porém a VALE3 só comporta R$ 10,45 adicionais. O impedimento é o limite de concentração, não o saldo.

Uma explicação correta seria:

> R$ 425,68 não foram alocados: BTC e PICPAY receberam toda a capacidade disponível, e a VALE3 só comporta R$ 10,45 adicionais, menos que o preço de uma ação (R$ 67,16). Os demais ativos atingiram seus limites.

## Regras que explicam a sobra

`ReferenciaAporte.limite` usa:

```text
R = patrimônio atual + aporte informado
limite percentual = min(meta × (1 + margem / 100), limite cadastrado, quando existente)
capacidade = max(0, limite percentual / 100 × R − valor atual)
```

Uma margem de 5% é relativa à meta. Uma meta de 2,8% permite aproximadamente 2,94%, não 7,8%. O motor mantém essa restrição mesmo quando há saldo não alocado. Nota 100 dá prioridade entre candidatos, mas não aumenta capacidade.

Posições sem meta própria, como PICPAY, herdam capacidade da classe/subclasse em `AporteService.heranca`. A ausência de meta exibida na linha não significa capacidade ilimitada.

Redistribuir o saldo entre esses mesmos três candidatos não resolverá o caso informado. Para executar os R$ 1.500 integralmente, as regras/configuração precisariam oferecer pelo menos R$ 425,68 de capacidade executável adicional. O diagnóstico não altera as metas nem pressupõe autorização para exceder limites.

## Outra ambiguidade da tela

`frontend/src/components/RankingAportesTable.tsx:109` chama a coluna de **Atual (após)**, mas mostra `percentual_atual`. O backend calcula esse campo como `valor atual / (patrimônio atual + aporte)`, sem adicionar a sugestão recebida pelo ativo ao numerador.

Por isso BTC ainda aparece com 16,48%, embora os R$ 96,54 sugeridos levem sua posição até o limite de aproximadamente 16,97%. Essa coluna não mostra a participação final depois de executar a compra. Para isso, seria necessário calcular `(valor atual + sugestão) / R` e identificar claramente o tratamento do saldo não alocado.

## Outros defeitos reproduzidos no alocador

São cenários sintéticos para isolar falhas do algoritmo; não são causas demonstradas da sobra específica do usuário.

| Cenário | Resultado observado antes da correção | Local principal no código original |
| --- | --- | --- |
| Capacidade de ativo em subclasse bloqueada continua pesando na divisão entre classes | De R$ 1.000, só R$ 57,14 são alocados em outra classe capaz de receber R$ 1.000 | `AlocacaoService.java:121`, `:109`, `:380` |
| Todas as subclasses estão bloqueadas, mas há ativo sem subclasse com capacidade na mesma classe | R$ 0 alocado de R$ 1.000 | `AlocacaoService.java:393` retorna antes do fallback em `:407` |
| Redistribuição do troco respeita apenas capacidade individual | Renda fixa recebe R$ 60 com teto explícito de classe de R$ 10 | `AlocacaoService.java:288` e `:319` |
| Aporte menor que a tolerância proporcional ao patrimônio | Em patrimônio de R$ 1 milhão, aporte de R$ 50 recebe alocação zero, apesar da capacidade | `AlocacaoService.java:60`, `:109`, `:428` |

O cálculo de `troco` em `AlocacaoService.java:176` considera só o arredondamento de cotas. Não representa todo o não alocado. No caso do usuário, troco = R$ 10,45 e não alocado = R$ 425,68. O endpoint calcula corretamente o total não alocado como `aporte − alocado`, mas sua explicação confunde as duas grandezas.

As correções do alocador devem calcular capacidade efetivamente executável em toda a hierarquia, manter o fallback para candidatos sem subclasse, aplicar todos os limites também na redistribuição e distinguir tolerância de percentuais de saldo monetário executável. Apenas aumentar o número de rodadas não resolve a capacidade bloqueada que permanece no denominador.

## Validação e reprodução

Os **47 testes existentes do motor** passaram. Os cenários acima foram executados separadamente com as classes compiladas e os construtores de cenário já existentes no projeto.

Na raiz do repositório:

```sh
cd backend
./mvnw -o -Dtest='*Aporte*Test,*Aportes*Test,RateioEntreClassesTest' test
cd ..
mkdir -p /tmp/focuslife-diagnostico-aportes
javac -cp backend/target/classes:backend/target/test-classes -d /tmp/focuslife-diagnostico-aportes diagnosticos/aportes/DiagnosticoAporte.java
java -cp backend/target/classes:backend/target/test-classes:/tmp/focuslife-diagnostico-aportes dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.DiagnosticoAporte
```

O comando Maven usa o cache local de dependências. O reprodutor imprime os resultados observados; não é uma suíte automatizada de asserções.

Na investigação inicial, nenhuma lógica de produção foi alterada.

## Correções implementadas após a escolha do usuário

Regra escolhida: **priorizar ativos abaixo da meta e dividir pela nota entre eles**.

- A primeira etapa distribui até os déficits dos ativos. Posições sem meta própria herdam uma fatia do déficit da classe/subclasse, assim como já herdavam a capacidade.
- A segunda etapa usa a capacidade restante até o limite operacional. A nota pondera a divisão dentro de cada grupo. Sem nota positiva restante, o espaço disponível vira o peso e permite continuar distribuindo.
- A capacidade de cada classe considera o que suas subclasses efetivamente podem receber. Ativos sem subclasse também formam um grupo e não dependem da existência de uma subclasse livre.
- Arredondamento e redistribuição respeitam os limites de ativo, classe e subclasse. O saldo total é conservado em centavos, usando BigDecimal.
- Preços de ações, FIIs e ETFs são normalizados em centavos antes da contagem de cotas, eliminando ruído originado em Float.
- A tolerância de percentual deixa de descartar dinheiro aplicável. A elegibilidade por limite usa valores monetários, evitando descartar capacidade real porque um percentual foi arredondado na tela.
- A explicação da sobra distingue falta de capacidade para uma cota de saldo insuficiente para uma compra permitida.
- A tela mostra **Antes da compra** e **Após o plano**, ambos sobre o patrimônio previsto. A projeção inclui todo o aporte e mantém o não alocado como saldo disponível; isso não registra uma posição de caixa no banco.
- Checklists desativados deixam de influenciar a nota consolidada e seus bloqueios. Avaliações parciais são identificadas na tabela. A fórmula de avaliação mantém as escalas e os pesos definidos pelo usuário.

Os testes antigos que exigiam preencher a margem de um ativo de nota alta antes de atender outro déficit foram atualizados para a regra escolhida. Os demais testes originais foram preservados.

Cobertura adicional: o caso informado, os defeitos reproduzidos, os dois estágios de aporte, ausência de nota, preço vindo de Float, integração do ranking com herança de déficit e consolidação de notas. Um teste com 200 cenários determinísticos verifica conservação do saldo, limites compartilhados, unidades inteiras e ausência de dinheiro parado quando ainda cabe uma compra.

Validação final: **67 testes de aporte e notas passaram**, e o build TypeScript/Vite do frontend foi concluído. No sandbox, o Mockito foi iniciado como agente Java para testar os serviços sem depender de autoanexação à JVM.

```sh
cd backend
./mvnw -o -Dtest='*Aporte*Test,*Aportes*Test,RateioEntreClassesTest,AlocacaoRegressaoTest,ChecklistNotasTest' test
cd ../frontend
npm run build
```

**Resultado da primeira correção:** com a margem fixa e as mesmas capacidades fornecidas pelo usuário, o plano permitia apenas R$ 1.074,32 de compras e deixava R$ 425,68 sem destino. A regra aprovada abaixo passa a ampliar a capacidade operacional do plano para reduzir essa sobra.

## Nova regra aprovada: no máximo 5% de sobra do aporte

O usuário escolheu ampliar somente a margem operacional do plano, mantendo os limites máximos cadastrados e os bloqueios do checklist.

```text
sobra máxima = aporte × 5%, arredondada para baixo em centavos
mínimo a aplicar = aporte − sobra máxima
Para R$ 1.500: sobra máxima R$ 75,00; mínimo a aplicar R$ 1.425,00.
```

O plano inicial continua usando déficit → nota → capacidade e cotas inteiras. Se já aplica ao menos 95%, ele é mantido. Caso contrário, `UtilizacaoAporteService` calcula uma margem ampliada e procura reduzi-la, em passos de 0,01 ponto percentual de margem, até encontrar um plano que atenda à meta de utilização.

Cada avaliação **completa o mesmo plano inicial**. As compras já sugeridas são preservadas; o complemento segue a prioridade de déficits e notas e mantém os limites explícitos de ativo, classe e subclasse. A margem aplicada não é salva na configuração nem altera os percentuais ideais cadastrados.

Se limites explícitos, bloqueios ou cotas impedirem atingir 95%, o motor busca a utilização executável que encontrou com essas restrições, aplica apenas a margem necessária para esse resultado e informa que a meta de utilização não foi atingida. Não inventa alocação para exibir uma sobra menor. Quando ampliar a margem não melhora a execução, a margem inicial é mantida.

Em cenário equivalente às três capacidades iniciais informadas (BTC R$ 96,54, PICPAY R$ 977,78 e VALE3 R$ 10,45), usando patrimônio projetado de R$ 19.800 e metas de 16,16%, 10% e 1,02%:

| Grandeza | Antes | Com ajuste do plano |
| --- | ---: | ---: |
| Aporte | R$ 1.500,00 | R$ 1.500,00 |
| Margem operacional | 5,00% | 11,78% |
| Alocado | R$ 1.074,32 | R$ 1.425,49 |
| Sobra | R$ 425,68 | R$ 74,51 |
| Percentual de sobra do aporte | 28,38% | 4,97% |

Esse resultado é uma reprodução controlada, não uma consulta à carteira real. O ranking completo foi testado também com PICPAY sem meta própria, herança de capacidade/deficit e um ativo bloqueado por critério eliminatório. O resultado preservou a configuração inicial de 5% e não aportou no bloqueado.

A API acrescenta `margem_configurada_percentual`, `sobra_maxima_percentual`, `valor_sobra_maxima`, `valor_minimo_alocar` e `meta_utilizacao_atingida`. `margem_percentual` identifica a margem efetivamente aplicada ao plano. A tela mostra os valores iniciais e aplicados, o objetivo de utilização e sua situação.

O reprodutor `DiagnosticoAporte.java` imprime tanto o rateio inicial quanto a nova regra de utilização. Essas mudanças continuam locais, sem publicar o projeto nem alterar a carteira cadastrada.

Validação da nova regra: **79 testes de aporte e notas passaram**, o build do frontend foi concluído e o reprodutor confirmou R$ 1.425,49 aplicados com R$ 74,51 de sobra no cenário equivalente.

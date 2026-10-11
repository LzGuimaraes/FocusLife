package dev.LzGuimaraes.FocusLifeHub.Admin.dto;

/**
 * Detalhe administrativo de um usuário: dados cadastrais PERMITIDOS + um resumo
 * (contagens) dos dados que ele possui no sistema.
 *
 * PRINCÍPIO DE SEGURANÇA
 * Este DTO é um {@code record} com campos FIXOS — nunca o {@link dev.LzGuimaraes.FocusLifeHub.User.UserModel}
 * cru. É assim que credenciais e material sensível ficam de fora: {@code password},
 * {@code activationCode}, {@code resetPasswordToken}, {@code tokenVersion} e as
 * expirações desses segredos NÃO têm componente aqui, então nunca são serializados
 * (a garantia é estrutural, não uma decisão de serialização).
 *
 * SOBRE O RESUMO
 * Cada contagem respeita o relacionamento REAL da tabela com o usuário — que NÃO é
 * uniforme neste projeto:
 * <ul>
 *   <li><b>FK direta</b> {@code user_id -> tb_user}: carteiras, matérias, metas,
 *       estratégias, checklists, config de aporte, histórico de score, tarefas, treinos;</li>
 *   <li><b>coluna escalar</b> {@code user_id} (sem FK de banco): compras/vendas,
 *       preços-alvo, aportes registrados, sessões de treino;</li>
 *   <li><b>aninhada</b> (a posse vem do pai): ativos e metas de ativo pela carteira
 *       de investimento, despesas pela carteira de dívidas, estudos e sessões de
 *       estudo pela matéria.</li>
 * </ul>
 * O resumo é montado com COUNT no banco (uma query por tabela), sem carregar
 * entidades nem varrer coleções — evita N+1 e volumes grandes de dados.
 */
public record AdminUserDetailDTO(
        Long id,
        String nome,
        String email,
        String role,
        boolean ativo,
        ResumoDTO resumo
) {

    /** Contagens dos dados do usuário, agrupadas pelos módulos do sistema. */
    public record ResumoDTO(
            FinancasDTO financas,
            PlanejamentoDTO planejamento,
            EstudosDTO estudos,
            long tarefas,
            TreinoDTO treino
    ) {}

    public record FinancasDTO(
            long carteirasInvestimento,
            long carteirasDividas,
            long ativos,
            long despesas,
            long operacoesCompraVenda,
            long precosAlvo
    ) {}

    public record PlanejamentoDTO(
            long estrategias,
            long metasDeAtivos,
            long checklistsModelo,
            long checklistsAtivo,
            boolean aporteConfigurado,
            long aportesRegistrados,
            long registrosHistorico
    ) {}

    public record EstudosDTO(
            long materias,
            long estudos,
            long sessoesEstudo
    ) {}

    public record TreinoDTO(
            long treinos,
            long sessoes
    ) {}
}

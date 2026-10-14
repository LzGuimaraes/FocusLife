package dev.LzGuimaraes.FocusLifeHub.Suporte;

public enum TipoChamado {
    NOVA_MUSICA("Nova música"), NOVO_ATIVO("Novo ativo"), MELHORIA("Melhoria do sistema"), DUVIDA_PROBLEMA("Dúvida ou problema");
    private final String descricao;
    TipoChamado(String descricao) { this.descricao = descricao; }
    public String descricao() { return descricao; }
}

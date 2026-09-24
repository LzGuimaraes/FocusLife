package dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.dto;

/**
 * Linha do sync do catálogo de ativos (`POST /ativos/admin/sync`).
 *
 * `setor` é o nome do SETOR DE MERCADO do ticker (V32): é por aqui que a
 * classificação manual (script/banco) entra, sem passar pela tela. O setor é
 * resolvido ou criado no catálogo global pelo nome normalizado.
 */
public class AtivoCadastroSyncDTO {
    private String nome;
    private String tipo;
    private Float precoAtual;
    private String setor;

    public AtivoCadastroSyncDTO() {}

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Float getPrecoAtual() { return precoAtual; }
    public void setPrecoAtual(Float precoAtual) { this.precoAtual = precoAtual; }

    public String getSetor() { return setor; }
    public void setSetor(String setor) { this.setor = setor; }
}

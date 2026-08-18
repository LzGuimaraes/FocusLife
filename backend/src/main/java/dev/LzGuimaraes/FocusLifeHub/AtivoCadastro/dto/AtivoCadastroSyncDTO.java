package dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.dto;

public class AtivoCadastroSyncDTO {
    private String nome;
    private String tipo;
    private Float precoAtual;

    public AtivoCadastroSyncDTO() {}

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Float getPrecoAtual() { return precoAtual; }
    public void setPrecoAtual(Float precoAtual) { this.precoAtual = precoAtual; }
}

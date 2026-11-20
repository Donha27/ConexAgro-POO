package Model;

public class Especificacao {
    private long idEspecificacao;
    private final String nomeEspecificacao;
    private final String valorEspecificacao;
    private final String unidade;

    public Especificacao(String nome, String valor, String unidade) {
        this(0, nome, valor, unidade);
    }

    public Especificacao(long id, String nome, String valor, String unidade) {
        idEspecificacao = Validacao.id(id);
        nomeEspecificacao = Validacao.obrigatorio(nome, "Caracteristica", 80);
        valorEspecificacao = Validacao.obrigatorio(valor, "Valor da especificacao", 255);
        this.unidade = Validacao.opcional(unidade, "Unidade", 30);
    }

    public long getIdEspecificacao() {
        return idEspecificacao;
    }

    public void setIdEspecificacao(long id) {
        idEspecificacao = Validacao.id(id);
    }

    public String getNomeEspecificacao() {
        return nomeEspecificacao;
    }

    public String getValorEspecificacao() {
        return valorEspecificacao;
    }

    public String getUnidade() {
        return unidade;
    }
}

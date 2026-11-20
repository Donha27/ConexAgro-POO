package Model;

public class OrigemCodigo {
    private long idOrigem;
    private final String nomeOrigem;
    private final String tipoOrigem;

    public OrigemCodigo(String nome, String tipo) {
        this(0, nome, tipo);
    }

    public OrigemCodigo(long id, String nome, String tipo) {
        this.idOrigem = Validacao.id(id);
        this.nomeOrigem = Validacao.obrigatorio(nome, "Origem", 120);
        if (!"fabricante".equals(tipo) && !"fornecedor".equals(tipo)) {
            throw new IllegalArgumentException("Origem deve ser fabricante ou fornecedor.");
        }
        this.tipoOrigem = tipo;
    }

    public long getIdOrigem() {
        return idOrigem;
    }

    public void setIdOrigem(long id) {
        idOrigem = Validacao.id(id);
    }

    public String getNomeOrigem() {
        return nomeOrigem;
    }

    public String getTipoOrigem() {
        return tipoOrigem;
    }

    @Override
    public String toString() {
        return nomeOrigem;
    }
}

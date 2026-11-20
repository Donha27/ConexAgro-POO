package Model;

public class Categoria {
    private long idCategoria;
    private String nomeCategoria;

    public Categoria(String nomeCategoria) {
        this(0, nomeCategoria);
    }

    public Categoria(long idCategoria, String nomeCategoria) {
        setIdCategoria(idCategoria);
        setNomeCategoria(nomeCategoria);
    }

    public long getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(long idCategoria) {
        this.idCategoria = Validacao.id(idCategoria);
    }

    public String getNomeCategoria() {
        return nomeCategoria;
    }

    public void setNomeCategoria(String nomeCategoria) {
        this.nomeCategoria = Validacao.obrigatorio(nomeCategoria, "Nome da categoria", 80);
    }

    @Override
    public String toString() {
        return nomeCategoria;
    }
}

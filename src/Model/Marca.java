package Model;

public class Marca {
    private long idMarca;
    private String nomeMarca;

    public Marca(String nomeMarca) {
        this(0, nomeMarca);
    }

    public Marca(long idMarca, String nomeMarca) {
        setIdMarca(idMarca);
        setNomeMarca(nomeMarca);
    }

    public long getIdMarca() {
        return idMarca;
    }

    public void setIdMarca(long idMarca) {
        this.idMarca = Validacao.id(idMarca);
    }

    public String getNomeMarca() {
        return nomeMarca;
    }

    public void setNomeMarca(String nomeMarca) {
        this.nomeMarca = Validacao.obrigatorio(nomeMarca, "Nome da marca", 120);
    }

    @Override
    public String toString() {
        return nomeMarca;
    }
}

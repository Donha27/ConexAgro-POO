package Model;

public class CodigoPeca {
    private long idCodigo;
    private final Marca marca;
    private final OrigemCodigo origem;
    private final String codigo;
    private final String tipoCodigo;

    public CodigoPeca(Marca marca, OrigemCodigo origem, String codigo, String tipo) {
        this(0, marca, origem, codigo, tipo);
    }

    public CodigoPeca(long id, Marca marca, OrigemCodigo origem, String codigo, String tipo) {
        idCodigo = Validacao.id(id);
        this.marca = Validacao.referencia(marca, "Marca do codigo");
        this.origem = Validacao.referencia(origem, "Origem do codigo");
        this.codigo = Validacao.obrigatorio(codigo, "Codigo", 80);
        if (!"original".equals(tipo) && !"paralelo".equals(tipo) && !"fornecedor".equals(tipo)) {
            throw new IllegalArgumentException("Tipo de codigo invalido.");
        }
        tipoCodigo = tipo;
    }

    public long getIdCodigo() {
        return idCodigo;
    }

    public void setIdCodigo(long id) {
        idCodigo = Validacao.id(id);
    }

    public Marca getMarca() {
        return marca;
    }

    public OrigemCodigo getOrigem() {
        return origem;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTipoCodigo() {
        return tipoCodigo;
    }
}

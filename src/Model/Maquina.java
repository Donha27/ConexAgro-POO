package Model;

public class Maquina extends ItemCatalogo {
    private String modelo;
    private String tipoMaquina;
    private Integer ano;
    private String serie;
    private String motor;
    private String versao;

    public Maquina(String nome, String modelo, String tipoMaquina, Marca marca) {
        this(0, nome, null, EstadoItem.RASCUNHO, marca, null,
                modelo, tipoMaquina, null, null, null, null);
    }

    public Maquina(long idItem, String nome, String descricao, EstadoItem estado,
                   Marca marca, Usuario usuarioCadastro, String modelo, String tipoMaquina,
                   Integer ano, String serie, String motor, String versao) {
        super(idItem, nome, descricao, estado,
                Validacao.referencia(marca, "Marca da maquina"), null, usuarioCadastro);
        setModelo(modelo);
        setTipoMaquina(tipoMaquina);
        setAno(ano);
        setSerie(serie);
        setMotor(motor);
        setVersao(versao);
    }

    public long getIdMaquina() {
        return getIdItem();
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = Validacao.obrigatorio(modelo, "Modelo da maquina", 80);
    }

    public String getTipoMaquina() {
        return tipoMaquina;
    }

    public void setTipoMaquina(String tipoMaquina) {
        this.tipoMaquina = Validacao.obrigatorio(tipoMaquina, "Tipo da maquina", 80);
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        if (ano != null && (ano < 1900 || ano > 2200)) {
            throw new IllegalArgumentException("Ano deve estar entre 1900 e 2200.");
        }
        this.ano = ano;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = Validacao.opcional(serie, "Serie", 100);
    }

    public String getMotor() {
        return motor;
    }

    public void setMotor(String motor) {
        this.motor = Validacao.opcional(motor, "Motor", 100);
    }

    public String getVersao() {
        return versao;
    }

    public void setVersao(String versao) {
        this.versao = Validacao.opcional(versao, "Versao", 100);
    }

    @Override
    public void setMarca(Marca marca) {
        super.setMarca(Validacao.referencia(marca, "Marca da maquina"));
    }

    @Override
    public String getTipoItem() {
        return "maquina";
    }

    @Override
    public String getResumo() {
        return "Maquina: " + getNome() + " | Modelo: " + modelo + " | Tipo: " + tipoMaquina;
    }
}

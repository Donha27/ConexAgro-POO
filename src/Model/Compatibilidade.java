package Model;

import java.time.LocalDateTime;

/**
 * Para editar, crie uma nova instancia com os dados alterados.
 */
public class Compatibilidade {
    private final Maquina maquina;
    private final String estado;
    private final String fonte;
    private final Long idCatalogo;
    private final Integer anoInicio;
    private final Integer anoFim;
    private final String restricaoSerie;
    private final String restricaoMotor;
    private final String restricaoVersao;
    private final String observacoes;
    private final LocalDateTime dataConfirmacao;

    public Compatibilidade(Maquina maquina) {
        this(maquina, "pendente", null, null, null, null, null, null, null, null, null);
    }

    public Compatibilidade(Maquina maquina, String estado, String fonte, Long idCatalogo,
                           Integer inicio, Integer fim, String serie, String motor, String versao,
                           String observacoes, LocalDateTime confirmacao) {
        this.maquina = Validacao.referencia(maquina, "Maquina");
        if (!"pendente".equals(estado) && !"confirmada".equals(estado)) {
            throw new IllegalArgumentException("Estado de compatibilidade invalido.");
        }
        this.estado = estado;
        this.fonte = Validacao.opcional(fonte, "Fonte", 500);
        if ("confirmada".equals(estado) && (this.fonte == null || confirmacao == null)) {
            throw new IllegalArgumentException("Confirmacao exige fonte e data.");
        }
        if (idCatalogo != null && (idCatalogo < 1 || idCatalogo > 4294967295L)) {
            throw new IllegalArgumentException("ID de catalogo invalido.");
        }
        if ((inicio != null && (inicio < 1900 || inicio > 2200))
                || (fim != null && (fim < 1900 || fim > 2200))
                || (inicio != null && fim != null && inicio > fim)) {
            throw new IllegalArgumentException("Intervalo de anos invalido.");
        }
        this.idCatalogo = idCatalogo;
        anoInicio = inicio;
        anoFim = fim;
        restricaoSerie = Validacao.opcional(serie, "Restricao de serie", 150);
        restricaoMotor = Validacao.opcional(motor, "Restricao de motor", 150);
        restricaoVersao = Validacao.opcional(versao, "Restricao de versao", 150);
        this.observacoes = observacoes;
        dataConfirmacao = confirmacao;
    }

    public Maquina getMaquina() {
        return maquina;
    }

    public String getEstado() {
        return estado;
    }

    public String getFonte() {
        return fonte;
    }

    public Long getIdCatalogo() {
        return idCatalogo;
    }

    public Integer getAnoInicio() {
        return anoInicio;
    }

    public Integer getAnoFim() {
        return anoFim;
    }

    public String getRestricaoSerie() {
        return restricaoSerie;
    }

    public String getRestricaoMotor() {
        return restricaoMotor;
    }

    public String getRestricaoVersao() {
        return restricaoVersao;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public LocalDateTime getDataConfirmacao() {
        return dataConfirmacao;
    }
}

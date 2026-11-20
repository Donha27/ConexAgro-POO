package DAO;

import Model.*;
import Util.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Detalhes pertencentes a uma peca. Toda alteracao bloqueia primeiro o item pai.
 */
public class DetalhesPecaDAO {
    private final ConexaoProvider provider;

    public DetalhesPecaDAO() {
        this(ConnectionFactory::getConnection);
    }

    public DetalhesPecaDAO(ConexaoProvider provider) {
        this.provider = provider;
    }

    private <T> T alterar(long peca, Jdbc.Trabalho<T> trabalho) throws SQLException {
        return Jdbc.transacao(provider, c -> {
            PecaDAO.bloquear(c, peca);
            T resposta = trabalho.executar(c);
            PecaDAO.validarSePublicada(c, peca);
            return resposta;
        });
    }

    private static void novo(long id) {
        if (id != 0) {
            throw new IllegalArgumentException("Detalhe novo deve ter ID zero.");
        }
    }

    public Imagem inserirImagem(long peca, Imagem imagem) throws SQLException {
        novo(imagem.getIdImagem());
        long id = alterar(peca, c -> Jdbc.inserir(c, "INSERT INTO imagem(id_item,url_imagem,descricao_imagem) VALUES(?,?,?)",
                peca, imagem.getUrlImagem(), imagem.getDescricaoImagem()));
        imagem.setIdImagem(id);
        return imagem;
    }

    public void atualizarImagem(long peca, Imagem imagem) throws SQLException {
        alterar(peca, c -> {
            Jdbc.alterarUma(c, "UPDATE imagem SET url_imagem=?,descricao_imagem=? WHERE id_item=? AND id_imagem=?",
                    imagem.getUrlImagem(), imagem.getDescricaoImagem(), peca, Jdbc.id(imagem.getIdImagem()));
            return null;
        });
    }

    public void excluirImagem(long peca, long imagem) throws SQLException {
        alterar(peca, c -> {
            Jdbc.alterarUma(c, "DELETE FROM imagem WHERE id_item=? AND id_imagem=?", peca, Jdbc.id(imagem));
            return null;
        });
    }

    public List<Imagem> listarImagens(long peca) throws SQLException {
        try (Connection c = provider.abrir()) {
            return imagens(c, Jdbc.id(peca));
        }
    }

    static List<Imagem> imagens(Connection c, long peca) throws SQLException {
        return Jdbc.listar(c, "SELECT * FROM imagem WHERE id_item=? ORDER BY id_imagem",
                r -> new Imagem(r.getLong("id_imagem"), r.getString("url_imagem"), r.getString("descricao_imagem")), peca);
    }

    public Especificacao inserirEspecificacao(long peca, Especificacao e) throws SQLException {
        novo(e.getIdEspecificacao());
        long id = alterar(peca, c -> Jdbc.inserir(c, "INSERT INTO especificacao(id_item,nome_especificacao,valor_especificacao,unidade) VALUES(?,?,?,?)",
                peca, e.getNomeEspecificacao(), e.getValorEspecificacao(), e.getUnidade()));
        e.setIdEspecificacao(id);
        return e;
    }

    public void atualizarEspecificacao(long peca, Especificacao e) throws SQLException {
        alterar(peca, c -> {
            Jdbc.alterarUma(c, "UPDATE especificacao SET nome_especificacao=?,valor_especificacao=?,unidade=? "
                            + "WHERE id_item=? AND id_especificacao=?", e.getNomeEspecificacao(), e.getValorEspecificacao(),
                    e.getUnidade(), peca, Jdbc.id(e.getIdEspecificacao()));
            return null;
        });
    }

    public void excluirEspecificacao(long peca, long especificacao) throws SQLException {
        alterar(peca, c -> {
            Jdbc.alterarUma(c, "DELETE FROM especificacao WHERE id_item=? AND id_especificacao=?", peca, Jdbc.id(especificacao));
            return null;
        });
    }

    public List<Especificacao> listarEspecificacoes(long peca) throws SQLException {
        try (Connection c = provider.abrir()) {
            return especificacoes(c, Jdbc.id(peca));
        }
    }

    static List<Especificacao> especificacoes(Connection c, long peca) throws SQLException {
        return Jdbc.listar(c, "SELECT * FROM especificacao WHERE id_item=? ORDER BY id_especificacao",
                r -> new Especificacao(r.getLong("id_especificacao"), r.getString("nome_especificacao"),
                        r.getString("valor_especificacao"), r.getString("unidade")), peca);
    }

    public CodigoPeca inserirCodigo(long peca, CodigoPeca codigo) throws SQLException {
        novo(codigo.getIdCodigo());
        long id = alterar(peca, c -> Jdbc.inserir(c, "INSERT INTO codigo_peca(id_peca,id_marca,id_origem,codigo,tipo_codigo) VALUES(?,?,?,?,?)",
                peca, Jdbc.id(codigo.getMarca().getIdMarca()), Jdbc.id(codigo.getOrigem().getIdOrigem()),
                codigo.getCodigo(), codigo.getTipoCodigo()));
        codigo.setIdCodigo(id);
        return codigo;
    }

    public void atualizarCodigo(long peca, CodigoPeca codigo) throws SQLException {
        alterar(peca, c -> {
            Jdbc.alterarUma(c, "UPDATE codigo_peca SET id_marca=?,id_origem=?,codigo=?,tipo_codigo=? "
                            + "WHERE id_peca=? AND id_codigo=?", Jdbc.id(codigo.getMarca().getIdMarca()),
                    Jdbc.id(codigo.getOrigem().getIdOrigem()), codigo.getCodigo(), codigo.getTipoCodigo(),
                    peca, Jdbc.id(codigo.getIdCodigo()));
            return null;
        });
    }

    public void excluirCodigo(long peca, long codigo) throws SQLException {
        alterar(peca, c -> {
            Jdbc.alterarUma(c, "DELETE FROM codigo_peca WHERE id_peca=? AND id_codigo=?", peca, Jdbc.id(codigo));
            return null;
        });
    }

    public List<CodigoPeca> listarCodigos(long peca) throws SQLException {
        try (Connection c = provider.abrir()) {
            return codigos(c, Jdbc.id(peca));
        }
    }

    static List<CodigoPeca> codigos(Connection c, long peca) throws SQLException {
        return Jdbc.listar(c, "SELECT cp.*,m.nome_marca,o.nome_origem,o.tipo_origem FROM codigo_peca cp "
                + "JOIN marca m ON m.id_marca=cp.id_marca JOIN origem_codigo o ON o.id_origem=cp.id_origem "
                + "WHERE cp.id_peca=? ORDER BY cp.id_codigo", r -> new CodigoPeca(r.getLong("id_codigo"),
                new Marca(r.getLong("id_marca"), r.getString("nome_marca")),
                new OrigemCodigo(r.getLong("id_origem"), r.getString("nome_origem"), r.getString("tipo_origem")),
                r.getString("codigo"), r.getString("tipo_codigo")), peca);
    }

    /**
     * Insere ou atualiza o vinculo da peca com essa maquina, sem duplicar o par.
     */
    public void salvarCompatibilidade(long peca, Compatibilidade a) throws SQLException {
        alterar(peca, c -> {
            long maquina = Jdbc.id(a.getMaquina().getIdMaquina());
            Object[] valores = {a.getEstado(), a.getFonte(), a.getIdCatalogo(), a.getAnoInicio(), a.getAnoFim(),
                    a.getRestricaoSerie(), a.getRestricaoMotor(), a.getRestricaoVersao(),
                    a.getObservacoes(), a.getDataConfirmacao(), peca, maquina};
            if (Jdbc.numero(c, "SELECT COUNT(*) FROM compatibilidade WHERE id_peca=? AND id_maquina=?", peca, maquina) > 0) {
                Jdbc.alterarUma(c, "UPDATE compatibilidade SET estado=?,fonte=?,id_catalogo=?,ano_inicio=?,ano_fim=?,"
                        + "restricao_serie=?,restricao_motor=?,restricao_versao=?,observacoes=?,data_confirmacao=? "
                        + "WHERE id_peca=? AND id_maquina=?", valores);
            } else {
                Jdbc.executar(c, "INSERT INTO compatibilidade(estado,fonte,id_catalogo,ano_inicio,ano_fim,restricao_serie,"
                        + "restricao_motor,restricao_versao,observacoes,data_confirmacao,id_peca,id_maquina) "
                        + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", valores);
            }
            return null;
        });
    }

    public void excluirCompatibilidade(long peca, long maquina) throws SQLException {
        alterar(peca, c -> {
            Jdbc.alterarUma(c, "DELETE FROM compatibilidade WHERE id_peca=? AND id_maquina=?", peca, Jdbc.id(maquina));
            return null;
        });
    }

    public List<Compatibilidade> listarCompatibilidades(long peca) throws SQLException {
        try (Connection c = provider.abrir()) {
            return compatibilidades(c, Jdbc.id(peca), false);
        }
    }

    static List<Compatibilidade> compatibilidades(Connection c, long peca, boolean confirmadas) throws SQLException {
        String sql = "SELECT " + Mapeamento.COLUNAS + ",q.modelo,q.tipo_maquina,q.ano,q.serie,q.motor,q.versao,"
                + "co.estado AS compat_estado,co.fonte,co.id_catalogo,co.ano_inicio,co.ano_fim,"
                + "co.restricao_serie,co.restricao_motor,co.restricao_versao,co.observacoes,co.data_confirmacao "
                + Mapeamento.ORIGEM + "JOIN maquina q ON q.id_maquina=i.id_item "
                + "JOIN compatibilidade co ON co.id_maquina=q.id_maquina WHERE co.id_peca=? "
                + (confirmadas ? "AND co.estado='confirmada' " : "") + "ORDER BY q.modelo,q.id_maquina";
        return Jdbc.listar(c, sql, r -> new Compatibilidade(Mapeamento.maquina(r), r.getString("compat_estado"),
                r.getString("fonte"), Jdbc.longNulo(r, "id_catalogo"), Jdbc.inteiroNulo(r, "ano_inicio"),
                Jdbc.inteiroNulo(r, "ano_fim"), r.getString("restricao_serie"), r.getString("restricao_motor"),
                r.getString("restricao_versao"), r.getString("observacoes"), Jdbc.data(r, "data_confirmacao")), peca);
    }
}

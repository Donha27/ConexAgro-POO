package DAO;

import Model.*;

import java.sql.ResultSet;
import java.sql.SQLException;

final class Mapeamento {
    private Mapeamento() {
    }

    static final String COLUNAS = "i.*, m.nome_marca, g.nome_categoria, "
            + "u.id_usuario AS autor_id, u.nome_completo AS autor_nome, u.email AS autor_email, "
            + "u.senha_hash AS autor_hash, u.ocupacao AS autor_ocupacao, "
            + "u.perfil_acesso AS autor_perfil, u.ativo AS autor_ativo, u.criado_em AS autor_criado";
    static final String ORIGEM = " FROM item_catalogo i "
            + "LEFT JOIN marca m ON m.id_marca=i.id_marca "
            + "LEFT JOIN categoria g ON g.id_categoria=i.id_categoria "
            + "LEFT JOIN usuario u ON u.id_usuario=i.id_usuario_cadastro ";
    static final String SELECT_PECA = "SELECT " + COLUNAS + ORIGEM
            + "JOIN peca p ON p.id_peca=i.id_item ";
    static final String SELECT_MAQUINA = "SELECT " + COLUNAS
            + ",q.modelo,q.tipo_maquina,q.ano,q.serie,q.motor,q.versao "
            + ORIGEM + "JOIN maquina q ON q.id_maquina=i.id_item ";

    static Usuario usuario(ResultSet r) throws SQLException {
        Usuario u = new Usuario(r.getLong("id_usuario"), r.getString("nome_completo"),
                r.getString("email"), r.getString("senha_hash"), Ocupacao.doBanco(r.getString("ocupacao")),
                PerfilAcesso.doBanco(r.getString("perfil_acesso")), r.getBoolean("ativo"));
        u.setCriadoEm(Jdbc.data(r, "criado_em"));
        return u;
    }

    private static Usuario autor(ResultSet r) throws SQLException {
        if (r.getLong("autor_id") == 0) {
            return null;
        }
        Usuario u = new Usuario(r.getLong("autor_id"), r.getString("autor_nome"),
                r.getString("autor_email"), r.getString("autor_hash"),
                Ocupacao.doBanco(r.getString("autor_ocupacao")),
                PerfilAcesso.doBanco(r.getString("autor_perfil")), r.getBoolean("autor_ativo"));
        u.setCriadoEm(Jdbc.data(r, "autor_criado"));
        return u;
    }

    private static Marca marca(ResultSet r) throws SQLException {
        return r.getLong("id_marca") == 0 ? null : new Marca(r.getLong("id_marca"), r.getString("nome_marca"));
    }

    private static Categoria categoria(ResultSet r) throws SQLException {
        return r.getLong("id_categoria") == 0 ? null
                : new Categoria(r.getLong("id_categoria"), r.getString("nome_categoria"));
    }

    private static void datas(ItemCatalogo item, ResultSet r) throws SQLException {
        item.setCriadoEm(Jdbc.data(r, "criado_em"));
        item.setAtualizadoEm(Jdbc.data(r, "atualizado_em"));
    }

    static Peca peca(ResultSet r) throws SQLException {
        Peca p = new Peca(r.getLong("id_item"), r.getString("nome"), r.getString("descricao"),
                EstadoItem.doBanco(r.getString("estado")), marca(r), categoria(r), autor(r));
        datas(p, r);
        return p;
    }

    static Maquina maquina(ResultSet r) throws SQLException {
        Maquina m = new Maquina(r.getLong("id_item"), r.getString("nome"), r.getString("descricao"),
                EstadoItem.doBanco(r.getString("estado")), marca(r), autor(r), r.getString("modelo"),
                r.getString("tipo_maquina"), Jdbc.inteiroNulo(r, "ano"), r.getString("serie"),
                r.getString("motor"), r.getString("versao"));
        m.setCategoria(categoria(r));
        datas(m, r);
        return m;
    }

    static Object marcaId(ItemCatalogo item) {
        return item.getMarca() == null ? null : Jdbc.id(item.getMarca().getIdMarca());
    }

    static Object categoriaId(ItemCatalogo item) {
        return item.getCategoria() == null ? null : Jdbc.id(item.getCategoria().getIdCategoria());
    }

    static Object autorId(ItemCatalogo item) {
        return item.getUsuarioCadastro() == null ? null : Jdbc.id(item.getUsuarioCadastro().getIdUsuario());
    }
}

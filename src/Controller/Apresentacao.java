package Controller;

import Model.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Respostas para as Views, compostas apenas de mapas, listas e valores simples.
 */
final class Apresentacao {
    private Apresentacao() {
    }

    static Map<String, Object> mapa() {
        return new LinkedHashMap<>();
    }

    static <T> List<Map<String, Object>> lista(List<T> valores, Function<T, Map<String, Object>> converter) {
        List<Map<String, Object>> resposta = new ArrayList<>();
        for (T valor : valores) {
            resposta.add(converter.apply(valor));
        }
        return resposta;
    }

    static Map<String, Object> usuario(Usuario u) {
        Map<String, Object> r = mapa();
        r.put("id", u.getIdUsuario());
        r.put("nome", u.getNomeCompleto());
        r.put("email", u.getEmail());
        r.put("ocupacao", u.getOcupacao() == null ? null : u.getOcupacao().getValorBanco());
        r.put("perfil", u.getPerfilAcesso().getValorBanco());
        r.put("ativo", u.isAtivo());
        r.put("criadoEm", u.getCriadoEm() == null ? null : u.getCriadoEm().toString());
        return r;
    }

    static Map<String, Object> marca(Marca m) {
        if (m == null) {
            return null;
        }
        Map<String, Object> r = mapa();
        r.put("id", m.getIdMarca());
        r.put("nome", m.getNomeMarca());
        return r;
    }

    static Map<String, Object> categoria(Categoria c) {
        if (c == null) {
            return null;
        }
        Map<String, Object> r = mapa();
        r.put("id", c.getIdCategoria());
        r.put("nome", c.getNomeCategoria());
        return r;
    }

    static Map<String, Object> origem(OrigemCodigo o) {
        Map<String, Object> r = mapa();
        r.put("id", o.getIdOrigem());
        r.put("nome", o.getNomeOrigem());
        r.put("tipo", o.getTipoOrigem());
        return r;
    }

    private static Map<String, Object> item(ItemCatalogo i) {
        Map<String, Object> r = mapa();
        r.put("id", i.getIdItem());
        r.put("tipo", i.getTipoItem());
        r.put("nome", i.getNome());
        r.put("descricao", i.getDescricao());
        r.put("estado", i.getEstado().getValorBanco());
        r.put("marca", marca(i.getMarca()));
        r.put("categoria", categoria(i.getCategoria()));
        if (i.getUsuarioCadastro() != null) {
            Map<String, Object> autor = mapa();
            autor.put("id", i.getUsuarioCadastro().getIdUsuario());
            autor.put("nome", i.getUsuarioCadastro().getNomeCompleto());
            r.put("autor", autor);
        } else {
            r.put("autor", null);
        }
        r.put("criadoEm", i.getCriadoEm() == null ? null : i.getCriadoEm().toString());
        r.put("atualizadoEm", i.getAtualizadoEm() == null ? null : i.getAtualizadoEm().toString());
        return r;
    }

    static Map<String, Object> peca(Peca p) {
        return item(p);
    }

    static Map<String, Object> maquina(Maquina m) {
        Map<String, Object> r = item(m);
        r.put("modelo", m.getModelo());
        r.put("tipoMaquina", m.getTipoMaquina());
        r.put("ano", m.getAno());
        r.put("serie", m.getSerie());
        r.put("motor", m.getMotor());
        r.put("versao", m.getVersao());
        return r;
    }

    static Map<String, Object> imagem(Imagem i) {
        Map<String, Object> r = mapa();
        r.put("id", i.getIdImagem());
        r.put("url", i.getUrlImagem());
        r.put("descricao", i.getDescricaoImagem());
        return r;
    }

    static Map<String, Object> especificacao(Especificacao e) {
        Map<String, Object> r = mapa();
        r.put("id", e.getIdEspecificacao());
        r.put("nome", e.getNomeEspecificacao());
        r.put("valor", e.getValorEspecificacao());
        r.put("unidade", e.getUnidade());
        return r;
    }

    static Map<String, Object> codigo(CodigoPeca c) {
        Map<String, Object> r = mapa();
        r.put("id", c.getIdCodigo());
        r.put("codigo", c.getCodigo());
        r.put("tipo", c.getTipoCodigo());
        r.put("marca", marca(c.getMarca()));
        r.put("origem", origem(c.getOrigem()));
        return r;
    }

    static Map<String, Object> compatibilidade(Compatibilidade c) {
        Map<String, Object> r = mapa();
        r.put("maquina", maquina(c.getMaquina()));
        r.put("estado", c.getEstado());
        r.put("fonte", c.getFonte());
        r.put("idCatalogo", c.getIdCatalogo());
        r.put("anoInicio", c.getAnoInicio());
        r.put("anoFim", c.getAnoFim());
        r.put("restricaoSerie", c.getRestricaoSerie());
        r.put("restricaoMotor", c.getRestricaoMotor());
        r.put("restricaoVersao", c.getRestricaoVersao());
        r.put("observacoes", c.getObservacoes());
        r.put("dataConfirmacao", c.getDataConfirmacao() == null ? null : c.getDataConfirmacao().toString());
        return r;
    }

    static Map<String, Object> ficha(FichaPeca f) {
        Map<String, Object> r = peca(f.getPeca());
        r.put("imagens", lista(f.getImagens(), Apresentacao::imagem));
        r.put("especificacoes", lista(f.getEspecificacoes(), Apresentacao::especificacao));
        r.put("codigos", lista(f.getCodigos(), Apresentacao::codigo));
        r.put("compatibilidades", lista(f.getCompatibilidades(), Apresentacao::compatibilidade));
        return r;
    }
}

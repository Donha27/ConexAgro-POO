package Controller;

import DAO.*;
import Model.*;
import Util.ConnectionFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class PecaController extends ControllerBase {
    private final AutenticacaoController acesso;
    private final PecaDAO pecas;
    private final DetalhesPecaDAO detalhes;
    private final MarcaDAO marcas;
    private final CategoriaDAO categorias;
    private final MaquinaDAO maquinas;
    private final OrigemCodigoDAO origens;
    private final ConsultaPecaDAO consultas;

    public PecaController(AutenticacaoController acesso) {
        this(acesso, ConnectionFactory::getConnection);
    }

    public PecaController(AutenticacaoController acesso, ConexaoProvider provider) {
        this.acesso = acesso;
        pecas = new PecaDAO(provider);
        detalhes = new DetalhesPecaDAO(provider);
        marcas = new MarcaDAO(provider);
        categorias = new CategoriaDAO(provider);
        maquinas = new MaquinaDAO(provider);
        origens = new OrigemCodigoDAO(provider);
        consultas = new ConsultaPecaDAO(provider);
    }

    public Map<String, Object> pagina(String token, boolean administracao, String termo, Long marca, Long categoria,
                                      Long maquina, Integer ano, String estado, int pagina, int tamanho) {
        return tratar(() -> {
            if (administracao) acesso.exigirAdministrador(token);
            else acesso.exigirUsuario(token);
            List<Peca> resultado = administracao ? consultas.administracao(termo, marca, categoria, estado) : pecas.pesquisar(termo, marca, categoria, maquina, ano);
            int limite = Math.max(1, Math.min(48, tamanho));
            int paginas = Math.max(1, (resultado.size() + limite - 1) / limite);
            int atual = Math.max(1, Math.min(paginas, pagina)), inicio = (atual - 1) * limite;
            List<Peca> selecionadas = resultado.subList(inicio, Math.min(inicio + limite, resultado.size()));
            java.util.List<Long> ids = new java.util.ArrayList<>();
            for (Peca p : selecionadas) ids.add(p.getIdPeca());
            java.util.Map<Long, ResumoPeca> extras = consultas.resumos(ids);
            java.util.List<Map<String, Object>> itens = new java.util.ArrayList<>();
            for (Peca p : selecionadas) {
                Map<String, Object> item = Apresentacao.peca(p);
                ResumoPeca extra = extras.get(p.getIdPeca());
                item.put("imagem", extra == null ? null : extra.getImagem());
                item.put("codigo", extra == null ? null : extra.getCodigo());
                itens.add(item);
            }
            Map<String, Object> r = Apresentacao.mapa();
            r.put("itens", itens);
            r.put("total", resultado.size());
            r.put("pagina", atual);
            r.put("paginas", paginas);
            r.put("tamanho", limite);
            return r;
        });
    }

    public List<Map<String, Object>> pesquisar(String token, String termo, Long marca, Long categoria, Long maquina, Integer ano) {
        return tratar(() -> {
            acesso.exigirUsuario(token);
            return Apresentacao.lista(pecas.pesquisar(termo, marca, categoria, maquina, ano), Apresentacao::peca);
        });
    }

    public List<Map<String, Object>> listarAdministracao(String token) {
        return tratar(() -> {
            acesso.exigirAdministrador(token);
            return Apresentacao.lista(pecas.listar(), Apresentacao::peca);
        });
    }

    public Map<String, Object> consultar(String token, long id) {
        return tratar(() -> {
            Usuario u = acesso.exigirUsuario(token);
            FichaPeca f = u.isAdministrador() ? pecas.buscarFichaPorId(id) : pecas.buscarFichaPublica(id);
            return Apresentacao.ficha(encontrado(f, "Peca"));
        });
    }

    public Map<String, Object> cadastrarRascunho(String token, String nome, long categoria, Long marca, String descricao) {
        return tratar(() -> {
            Usuario ator = acesso.exigirAdministrador(token);
            Peca p = new Peca(nome, encontrado(categorias.buscarPorId(categoria), "Categoria"));
            if (marca != null) {
                p.setMarca(encontrado(marcas.buscarPorId(marca), "Marca"));
            }
            p.setDescricao(descricao);
            p.setUsuarioCadastro(ator);
            return Apresentacao.peca(pecas.inserir(p));
        });
    }

    public Map<String, Object> editar(String token, long id, String nome, long categoria, Long marca, String descricao) {
        return tratar(() -> {
            acesso.exigirAdministrador(token);
            Peca p = encontrado(pecas.buscarPorId(id), "Peca");
            p.setNome(nome);
            p.setCategoria(encontrado(categorias.buscarPorId(categoria), "Categoria"));
            p.setMarca(marca == null ? null : encontrado(marcas.buscarPorId(marca), "Marca"));
            p.setDescricao(descricao);
            // Preserva o autor original e o estado; publicar e inativar sao operacoes separadas.
            pecas.atualizar(p);
            return Apresentacao.peca(pecas.buscarPorId(id));
        });
    }

    public void publicar(String token, long id) {
        tratar(() -> {
            acesso.exigirAdministrador(token);
            pecas.publicar(id);
            return null;
        });
    }

    public void inativar(String token, long id) {
        tratar(() -> {
            acesso.exigirAdministrador(token);
            pecas.inativar(id);
            return null;
        });
    }

    public void excluir(String token, long id) {
        tratar(() -> {
            acesso.exigirAdministrador(token);
            pecas.excluir(id);
            return null;
        });
    }

    public Map<String, Object> salvarImagem(String token, long peca, long id, String url, String descricao) {
        return tratar(() -> {
            acesso.exigirAdministrador(token);
            Imagem i = new Imagem(id, url, descricao);
            if (id == 0) {
                detalhes.inserirImagem(peca, i);
            } else {
                detalhes.atualizarImagem(peca, i);
            }
            return Apresentacao.imagem(i);
        });
    }

    public void excluirImagem(String token, long peca, long id) {
        tratar(() -> {
            acesso.exigirAdministrador(token);
            detalhes.excluirImagem(peca, id);
            return null;
        });
    }

    public Map<String, Object> salvarEspecificacao(String token, long peca, long id, String nome, String valor, String unidade) {
        return tratar(() -> {
            acesso.exigirAdministrador(token);
            Especificacao e = new Especificacao(id, nome, valor, unidade);
            if (id == 0) {
                detalhes.inserirEspecificacao(peca, e);
            } else {
                detalhes.atualizarEspecificacao(peca, e);
            }
            return Apresentacao.especificacao(e);
        });
    }

    public void excluirEspecificacao(String token, long peca, long id) {
        tratar(() -> {
            acesso.exigirAdministrador(token);
            detalhes.excluirEspecificacao(peca, id);
            return null;
        });
    }

    public Map<String, Object> salvarCodigo(String token, long peca, long id, long marca, long origem, String codigo, String tipo) {
        return tratar(() -> {
            acesso.exigirAdministrador(token);
            CodigoPeca c = new CodigoPeca(id, encontrado(marcas.buscarPorId(marca), "Marca"),
                    encontrado(origens.buscarPorId(origem), "Origem"), codigo, tipo);
            if (id == 0) {
                detalhes.inserirCodigo(peca, c);
            } else {
                detalhes.atualizarCodigo(peca, c);
            }
            return Apresentacao.codigo(c);
        });
    }

    public void excluirCodigo(String token, long peca, long id) {
        tratar(() -> {
            acesso.exigirAdministrador(token);
            detalhes.excluirCodigo(peca, id);
            return null;
        });
    }

    public void salvarCompatibilidade(String token, long peca, long maquina, String estado, String fonte,
                                      Integer inicio, Integer fim, String serie, String motor, String versao, String observacoes) {
        tratar(() -> {
            acesso.exigirAdministrador(token);
            Maquina m = encontrado(maquinas.buscarPorId(maquina), "Maquina");
            Compatibilidade c = new Compatibilidade(m, estado, fonte, null, inicio, fim, serie, motor, versao, observacoes,
                    "confirmada".equals(estado) ? LocalDateTime.now() : null);
            detalhes.salvarCompatibilidade(peca, c);
            return null;
        });
    }

    public void excluirCompatibilidade(String token, long peca, long maquina) {
        tratar(() -> {
            acesso.exigirAdministrador(token);
            detalhes.excluirCompatibilidade(peca, maquina);
            return null;
        });
    }
}

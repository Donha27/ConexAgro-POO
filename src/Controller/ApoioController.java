package Controller;

import DAO.*;
import Model.*;
import Util.ConnectionFactory;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class ApoioController extends ControllerBase {
    private final AutenticacaoController acesso;
    private final MarcaDAO marcas;
    private final CategoriaDAO categorias;
    private final OrigemCodigoDAO origens;
    private final MaquinaDAO maquinas;

    public ApoioController(AutenticacaoController acesso) {
        this(acesso, ConnectionFactory::getConnection);
    }

    public ApoioController(AutenticacaoController acesso, ConexaoProvider provider) {
        this.acesso = acesso;
        marcas = new MarcaDAO(provider);
        categorias = new CategoriaDAO(provider);
        origens = new OrigemCodigoDAO(provider);
        maquinas = new MaquinaDAO(provider);
    }

    public Map<String, Object> listar(String token) {
        return tratar(() -> {
            Usuario ator = acesso.exigirUsuario(token);
            Map<String, Object> r = Apresentacao.mapa();
            r.put("marcas", Apresentacao.lista(marcas.listar(), Apresentacao::marca));
            r.put("categorias", Apresentacao.lista(categorias.listar(), Apresentacao::categoria));
            r.put("origens", Apresentacao.lista(origens.listar(), Apresentacao::origem));
            List<Maquina> lista = new ArrayList<>();
            for (Maquina m : maquinas.listar()) {
                if (ator.isAdministrador() || m.getEstado() != EstadoItem.INATIVO) {
                    lista.add(m);
                }
            }
            r.put("maquinas", Apresentacao.lista(lista, Apresentacao::maquina));
            return r;
        });
    }

    public Map<String, Object> salvarMarca(String token, long id, String nome) {
        return tratar(() -> {
            acesso.exigirAdministrador(token);
            Marca m = new Marca(id, nome);
            if (id == 0) {
                marcas.inserir(m);
            } else {
                marcas.atualizar(m);
            }
            return Apresentacao.marca(m);
        });
    }

    public void excluirMarca(String token, long id) {
        tratar(() -> {
            acesso.exigirAdministrador(token);
            marcas.excluir(id);
            return null;
        });
    }

    public Map<String, Object> salvarCategoria(String token, long id, String nome) {
        return tratar(() -> {
            acesso.exigirAdministrador(token);
            Categoria c = new Categoria(id, nome);
            if (id == 0) {
                categorias.inserir(c);
            } else {
                categorias.atualizar(c);
            }
            return Apresentacao.categoria(c);
        });
    }

    public void excluirCategoria(String token, long id) {
        tratar(() -> {
            acesso.exigirAdministrador(token);
            categorias.excluir(id);
            return null;
        });
    }

    public Map<String, Object> salvarOrigem(String token, long id, String nome, String tipo) {
        return tratar(() -> {
            acesso.exigirAdministrador(token);
            OrigemCodigo o = new OrigemCodigo(id, nome, tipo);
            if (id == 0) {
                origens.inserir(o);
            } else {
                origens.atualizar(o);
            }
            return Apresentacao.origem(o);
        });
    }

    public void excluirOrigem(String token, long id) {
        tratar(() -> {
            acesso.exigirAdministrador(token);
            origens.excluir(id);
            return null;
        });
    }

    public Map<String, Object> salvarMaquina(String token, long id, String nome, String modelo, String tipo, long marca,
                                             String descricao, Integer ano, String serie, String motor, String versao, EstadoItem estado) {
        return tratar(() -> {
            Usuario ator = acesso.exigirAdministrador(token);
            Maquina anterior = id == 0 ? null : encontrado(maquinas.buscarPorId(id), "Maquina");
            Maquina m = new Maquina(id, nome, descricao, estado, encontrado(marcas.buscarPorId(marca), "Marca"),
                    anterior == null ? ator : anterior.getUsuarioCadastro(), modelo, tipo, ano, serie, motor, versao);
            if (anterior != null) {
                m.setCategoria(anterior.getCategoria());
            }
            if (id == 0) {
                maquinas.inserir(m);
            } else {
                maquinas.atualizar(m);
            }
            return Apresentacao.maquina(encontrado(maquinas.buscarPorId(m.getIdMaquina()), "Maquina"));
        });
    }

    public void excluirMaquina(String token, long id) {
        tratar(() -> {
            acesso.exigirAdministrador(token);
            maquinas.excluir(id);
            return null;
        });
    }
}

package Controller;

import DAO.*;
import Model.*;
import Util.SenhaUtil;

import java.util.List;
import java.util.Map;

public class UsuarioController extends ControllerBase {
    private final AutenticacaoController acesso;
    private final UsuarioDAO usuarios;
    private final UsuarioSegurancaDAO seguranca;

    public UsuarioController(AutenticacaoController acesso) {
        this(acesso, new UsuarioDAO(), new UsuarioSegurancaDAO());
    }

    public UsuarioController(AutenticacaoController acesso, UsuarioDAO usuarios, UsuarioSegurancaDAO seguranca) {
        this.acesso = acesso;
        this.usuarios = usuarios;
        this.seguranca = seguranca;
    }

    /**
     * Cadastro publico: nao recebe perfil nem estado de atividade.
     */
    public Map<String, Object> registrar(String nome, String email, String senha, Ocupacao ocupacao) {
        return tratar(() -> Apresentacao.usuario(usuarios.inserir(
                new Usuario(nome, email, SenhaUtil.gerarHash(senha), ocupacao))));
    }

    public Map<String, Object> meuPerfil(String token) {
        return Apresentacao.usuario(acesso.exigirUsuario(token));
    }

    public Map<String, Object> editarMeuPerfil(String token, String nome, String email, Ocupacao ocupacao) {
        return tratar(() -> {
            Usuario u = acesso.exigirUsuario(token);
            u.setNomeCompleto(nome);
            u.setEmail(email);
            u.setOcupacao(ocupacao);
            return Apresentacao.usuario(seguranca.atualizarPerfil(u));
        });
    }

    public void alterarMinhaSenha(String token, String atual, String nova) {
        tratar(() -> {
            Usuario u = acesso.exigirUsuario(token);
            if (!SenhaUtil.verificar(atual, u.getSenhaHash())) {
                throw new ErroAplicacao(401, "Senha atual incorreta.");
            }
            seguranca.alterarSenha(u.getIdUsuario(), u.getSenhaHash(), SenhaUtil.gerarHash(nova));
            acesso.invalidarUsuario(u.getIdUsuario());
            return null;
        });
    }

    public List<Map<String, Object>> listar(String token, String termo) {
        return tratar(() -> {
            acesso.exigirAdministrador(token);
            return Apresentacao.lista(usuarios.pesquisar(termo), Apresentacao::usuario);
        });
    }

    public Map<String, Object> consultar(String token, long id) {
        return tratar(() -> {
            acesso.exigirAdministrador(token);
            return Apresentacao.usuario(encontrado(usuarios.buscarPorId(id), "Usuario"));
        });
    }

    public Map<String, Object> cadastrar(String token, String nome, String email, String senha, Ocupacao ocupacao, PerfilAcesso perfil) {
        return tratar(() -> {
            acesso.exigirAdministrador(token);
            Usuario u = new Usuario(0, nome, email, SenhaUtil.gerarHash(senha), ocupacao, perfil, true);
            return Apresentacao.usuario(usuarios.inserir(u));
        });
    }

    public Map<String, Object> editar(String token, long id, String nome, String email, Ocupacao ocupacao,
                                      PerfilAcesso perfil, boolean ativo, String novaSenha) {
        return tratar(() -> {
            Usuario ator = acesso.exigirAdministrador(token);
            Usuario atual = encontrado(usuarios.buscarPorId(id), "Usuario");
            Usuario dados = new Usuario(id, nome, email, atual.getSenhaHash(), ocupacao, perfil, ativo);
            String hash = novaSenha == null || novaSenha.isEmpty() ? null : SenhaUtil.gerarHash(novaSenha);
            Usuario alterado = seguranca.atualizarComoAdministrador(ator.getIdUsuario(), dados, hash);
            acesso.invalidarUsuario(id);
            return Apresentacao.usuario(alterado);
        });
    }

    public void excluir(String token, long id) {
        tratar(() -> {
            Usuario ator = acesso.exigirAdministrador(token);
            seguranca.excluirComoAdministrador(ator.getIdUsuario(), id);
            acesso.invalidarUsuario(id);
            return null;
        });
    }
}

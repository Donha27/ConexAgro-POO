package Controller;

import DAO.ConexaoProvider;
import DAO.UsuarioDAO;
import DAO.UsuarioSegurancaDAO;
import Util.ConnectionFactory;

/**
 * Composicao compartilhada pelos futuros handlers do site.
 */
public class AplicacaoController {
    private final AutenticacaoController autenticacao;
    private final UsuarioController usuarios;
    private final PecaController pecas;
    private final ApoioController apoio;

    public AplicacaoController() {
        this(ConnectionFactory::getConnection);
    }

    public AplicacaoController(ConexaoProvider provider) {
        UsuarioDAO dao = new UsuarioDAO(provider);
        autenticacao = new AutenticacaoController(dao);
        usuarios = new UsuarioController(autenticacao, dao, new UsuarioSegurancaDAO(provider));
        pecas = new PecaController(autenticacao, provider);
        apoio = new ApoioController(autenticacao, provider);
    }

    public AutenticacaoController getAutenticacao() {
        return autenticacao;
    }

    public UsuarioController getUsuarios() {
        return usuarios;
    }

    public PecaController getPecas() {
        return pecas;
    }

    public ApoioController getApoio() {
        return apoio;
    }
}

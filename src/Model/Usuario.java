package Model;

import java.time.LocalDateTime;
import java.util.Locale;

public class Usuario {
    private long idUsuario;
    private String nomeCompleto;
    private String email;
    private String senhaHash;
    private Ocupacao ocupacao;
    private PerfilAcesso perfilAcesso;
    private boolean ativo;
    private LocalDateTime criadoEm;

    public Usuario(String nomeCompleto, String email, String senhaHash, Ocupacao ocupacao) {
        this(0, nomeCompleto, email, senhaHash, ocupacao, PerfilAcesso.USUARIO, true);
    }

    public Usuario(long idUsuario, String nomeCompleto, String email, String senhaHash,
                   Ocupacao ocupacao, PerfilAcesso perfilAcesso, boolean ativo) {
        setIdUsuario(idUsuario);
        setNomeCompleto(nomeCompleto);
        setEmail(email);
        setSenhaHash(senhaHash);
        validarPerfilEOcupacao(perfilAcesso, ocupacao);
        this.ocupacao = ocupacao;
        this.perfilAcesso = perfilAcesso;
        this.ativo = ativo;
    }

    private static void validarPerfilEOcupacao(PerfilAcesso perfil, Ocupacao ocupacao) {
        Validacao.referencia(perfil, "Perfil de acesso");
        if (perfil == PerfilAcesso.USUARIO && ocupacao == null) {
            throw new IllegalArgumentException("Informe a ocupacao do usuario comum.");
        }
    }

    public long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(long idUsuario) {
        this.idUsuario = Validacao.id(idUsuario);
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = Validacao.obrigatorio(nomeCompleto, "Nome completo", 120);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        String normalizado = Validacao.obrigatorio(email, "E-mail", 254).toLowerCase(Locale.ROOT);
        // Validacao pratica de formulario, sem tentar implementar todo o RFC de e-mail.
        if (!normalizado.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Informe um e-mail valido.");
        }
        this.email = normalizado;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        // Recebe um hash produzido pelo futuro componente de autenticacao.
        // Esta entidade nao recebe uma senha em texto simples nem calcula o hash.
        this.senhaHash = Validacao.obrigatorio(senhaHash, "Hash da senha", 255);
    }

    public Ocupacao getOcupacao() {
        return ocupacao;
    }

    public void setOcupacao(Ocupacao ocupacao) {
        validarPerfilEOcupacao(perfilAcesso, ocupacao);
        this.ocupacao = ocupacao;
    }

    public PerfilAcesso getPerfilAcesso() {
        return perfilAcesso;
    }

    public void setPerfilAcesso(PerfilAcesso perfilAcesso) {
        validarPerfilEOcupacao(perfilAcesso, ocupacao);
        this.perfilAcesso = perfilAcesso;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public boolean isAdministrador() {
        return perfilAcesso == PerfilAcesso.ADMINISTRADOR;
    }

    public boolean podeGerenciarCatalogo() {
        // O Controller devera consultar o usuario autenticado antes de cada operacao.
        return ativo && isAdministrador();
    }

    @Override
    public String toString() {
        // Nunca incluir senhaHash na representacao de exibicao.
        return "Usuario{id=" + idUsuario + ", nome='" + nomeCompleto
                + "', perfil=" + perfilAcesso + ", ativo=" + ativo + "}";
    }
}

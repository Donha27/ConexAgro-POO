package Model;

import java.time.LocalDateTime;

/**
 * Base abstrata das entidades tecnicas. Nao representa um item generico instanciavel.
 */
public abstract class ItemCatalogo {
    private long idItem;
    private String nome;
    private String descricao;
    private EstadoItem estado;
    private Marca marca;
    private Categoria categoria;
    private Usuario usuarioCadastro;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    protected ItemCatalogo(long idItem, String nome, String descricao, EstadoItem estado,
                           Marca marca, Categoria categoria, Usuario usuarioCadastro) {
        // Atribuicoes diretas evitam chamar metodos sobrescritos durante a construcao.
        this.idItem = Validacao.id(idItem);
        this.nome = Validacao.obrigatorio(nome, "Nome do item", 150);
        this.descricao = descricaoOpcional(descricao);
        this.estado = Validacao.referencia(estado, "Estado do item");
        validarCamposPublicados(this.estado, marca, this.descricao);
        this.marca = marca;
        this.categoria = categoria;
        this.usuarioCadastro = usuarioCadastro;
    }

    private static String descricaoOpcional(String descricao) {
        // TEXT nao possui o mesmo limite em caracteres e bytes. O DAO tambem
        // tratara os limites de armazenamento do MySQL na persistencia.
        return descricao == null || descricao.trim().isEmpty() ? null : descricao.trim();
    }

    private static void validarCamposPublicados(EstadoItem estado, Marca marca, String descricao) {
        if (estado == EstadoItem.PUBLICADO && (marca == null || descricao == null)) {
            throw new IllegalArgumentException("Item publicado exige marca e descricao.");
        }
    }

    public long getIdItem() {
        return idItem;
    }

    public void setIdItem(long idItem) {
        this.idItem = Validacao.id(idItem);
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = Validacao.obrigatorio(nome, "Nome do item", 150);
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        String novaDescricao = descricaoOpcional(descricao);
        validarCamposPublicados(estado, marca, novaDescricao);
        this.descricao = novaDescricao;
    }

    public EstadoItem getEstado() {
        return estado;
    }

    public void setEstado(EstadoItem estado) {
        EstadoItem novoEstado = Validacao.referencia(estado, "Estado do item");
        validarCamposPublicados(novoEstado, marca, descricao);
        // Valida somente os campos locais. Publicacao completa (RN02) exigira
        // a operacao transacional do DAO, com codigos, imagens e compatibilidades.
        this.estado = novoEstado;
    }

    public Marca getMarca() {
        return marca;
    }

    public void setMarca(Marca marca) {
        validarCamposPublicados(estado, marca, descricao);
        this.marca = marca;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Usuario getUsuarioCadastro() {
        return usuarioCadastro;
    }

    public void setUsuarioCadastro(Usuario usuarioCadastro) {
        // Pode ser nulo: o banco preserva o item quando a conta e excluida.
        this.usuarioCadastro = usuarioCadastro;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public abstract String getTipoItem();

    public abstract String getResumo();

    @Override
    public String toString() {
        return getResumo();
    }
}

package Model;

public class Peca extends ItemCatalogo {
    public Peca(String nome, Categoria categoria) {
        this(0, nome, null, EstadoItem.RASCUNHO, null, categoria, null);
    }

    public Peca(long idItem, String nome, String descricao, EstadoItem estado,
                Marca marca, Categoria categoria, Usuario usuarioCadastro) {
        super(idItem, nome, descricao, estado, marca,
                Validacao.referencia(categoria, "Categoria da peca"), usuarioCadastro);
    }

    // Nao existe um segundo campo de ID: peca.id_peca e item_catalogo.id_item
    // representam o mesmo objeto e compartilham o mesmo valor no banco.
    public long getIdPeca() {
        return getIdItem();
    }

    @Override
    public void setCategoria(Categoria categoria) {
        super.setCategoria(Validacao.referencia(categoria, "Categoria da peca"));
    }

    @Override
    public String getTipoItem() {
        return "peca";
    }

    @Override
    public String getResumo() {
        return "Peca: " + getNome() + " | Categoria: " + getCategoria().getNomeCategoria();
    }
}

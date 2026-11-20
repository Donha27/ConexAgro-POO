package Model;

public class Imagem {
    private long idImagem;
    private final String urlImagem;
    private final String descricaoImagem;

    public Imagem(String url, String descricao) {
        this(0, url, descricao);
    }

    public Imagem(long id, String url, String descricao) {
        idImagem = Validacao.id(id);
        urlImagem = Validacao.obrigatorio(url, "Endereco da imagem", 2048);
        descricaoImagem = Validacao.opcional(descricao, "Descricao da imagem", 255);
    }

    public long getIdImagem() {
        return idImagem;
    }

    public void setIdImagem(long id) {
        idImagem = Validacao.id(id);
    }

    public String getUrlImagem() {
        return urlImagem;
    }

    public String getDescricaoImagem() {
        return descricaoImagem;
    }
}

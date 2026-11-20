package Model;

public class ResumoPeca {
    private final String imagem;
    private final String codigo;

    public ResumoPeca(String imagem, String codigo) {
        this.imagem = imagem;
        this.codigo = codigo;
    }

    public String getImagem() {
        return imagem;
    }

    public String getCodigo() {
        return codigo;
    }
}

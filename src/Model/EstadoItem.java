package Model;

/**
 * Valores correspondentes ao ENUM estado de item_catalogo.
 */
public enum EstadoItem {
    RASCUNHO("rascunho"), PUBLICADO("publicado"), INATIVO("inativo");

    private final String valorBanco;

    EstadoItem(String valorBanco) {
        this.valorBanco = valorBanco;
    }

    public String getValorBanco() {
        return valorBanco;
    }

    public static EstadoItem doBanco(String valor) {
        for (EstadoItem estado : values()) {
            if (estado.valorBanco.equals(valor)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de item desconhecido: " + valor);
    }
}

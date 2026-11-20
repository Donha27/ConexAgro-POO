package Model;

/**
 * Ocupacao da pessoa; nao concede permissoes administrativas.
 */
public enum Ocupacao {
    VENDEDOR("vendedor"), AGRICULTOR("agricultor"), MECANICO("mecanico");

    private final String valorBanco;

    Ocupacao(String valorBanco) {
        this.valorBanco = valorBanco;
    }

    public String getValorBanco() {
        return valorBanco;
    }

    public static Ocupacao doBanco(String valor) {
        // O banco permite ocupacao nula para administrador.
        if (valor == null) {
            return null;
        }
        for (Ocupacao ocupacao : values()) {
            if (ocupacao.valorBanco.equals(valor)) {
                return ocupacao;
            }
        }
        throw new IllegalArgumentException("Ocupacao desconhecida: " + valor);
    }
}

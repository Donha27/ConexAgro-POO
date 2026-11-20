package Model;

/**
 * Regras simples compartilhadas pelas entidades. Visivel apenas no pacote Model.
 */
final class Validacao {
    private Validacao() {
    }

    static long id(long valor) {
        // Zero identifica um objeto novo. INT UNSIGNED do MySQL cabe em long.
        if (valor < 0 || valor > 4294967295L) {
            throw new IllegalArgumentException("ID fora do intervalo do banco.");
        }
        return valor;
    }

    static String obrigatorio(String valor, String campo, int limite) {
        String texto = opcional(valor, campo, limite);
        if (texto == null) {
            throw new IllegalArgumentException(campo + " e obrigatorio.");
        }
        return texto;
    }

    static String opcional(String valor, String campo, int limite) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        String texto = valor.trim();
        if (texto.codePointCount(0, texto.length()) > limite) {
            throw new IllegalArgumentException(campo + " excede " + limite + " caracteres.");
        }
        return texto;
    }

    static <T> T referencia(T valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException(campo + " e obrigatorio.");
        }
        return valor;
    }
}

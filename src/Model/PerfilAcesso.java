package Model;

public enum PerfilAcesso {
    USUARIO("usuario"), ADMINISTRADOR("administrador");

    private final String valorBanco;

    PerfilAcesso(String valorBanco) {
        this.valorBanco = valorBanco;
    }

    public String getValorBanco() {
        return valorBanco;
    }

    public static PerfilAcesso doBanco(String valor) {
        for (PerfilAcesso perfil : values()) {
            if (perfil.valorBanco.equals(valor)) {
                return perfil;
            }
        }
        throw new IllegalArgumentException("Perfil de acesso desconhecido: " + valor);
    }
}

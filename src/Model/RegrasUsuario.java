package Model;

public final class RegrasUsuario {
    private RegrasUsuario() {
    }

    public static void validarAdministracao(Usuario ator) {
        if (ator == null || !ator.podeGerenciarCatalogo()) {
            throw new AcessoNegadoException("Operacao permitida somente ao administrador ativo.");
        }
    }

    public static void validarAlteracao(Usuario ator, Usuario atual, Usuario novo, int administradoresAtivos) {
        validarAdministracao(ator);
        boolean removePrivilegio = atual.podeGerenciarCatalogo() && !novo.podeGerenciarCatalogo();
        protegerUltimo(removePrivilegio, administradoresAtivos);
    }

    public static void validarExclusao(Usuario ator, Usuario alvo, int administradoresAtivos) {
        validarAdministracao(ator);
        protegerUltimo(alvo.podeGerenciarCatalogo(), administradoresAtivos);
    }

    private static void protegerUltimo(boolean removeAdministrador, int quantidade) {
        if (removeAdministrador && quantidade <= 1) {
            throw new OperacaoBloqueadaException("Mantenha pelo menos um administrador ativo no sistema.");
        }
    }
}

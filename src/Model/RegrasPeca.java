package Model;

/**
 * Regras de negocio puras; nao dependem de SQL ou de uma interface.
 */
public final class RegrasPeca {
    private RegrasPeca() {
    }

    public static void exigirNovoRascunho(Peca peca) {
        if (peca.getIdItem() != 0 || peca.getEstado() != EstadoItem.RASCUNHO) {
            throw new IllegalArgumentException("Cadastre a nova peca como rascunho, sem ID.");
        }
    }

    public static void validarPublicacao(Peca peca, long codigos, long imagens,
                                         long especificacoes, long confirmacoes) {
        if (peca.getMarca() == null || peca.getDescricao() == null
                || codigos < 1 || imagens < 1 || especificacoes < 1 || confirmacoes < 1) {
            throw new IllegalArgumentException("Publicacao exige marca, descricao, codigo, imagem, "
                    + "especificacao e compatibilidade confirmada.");
        }
    }

    public static void validarExclusao(EstadoItem estado, long referenciasExternas) {
        if (estado != EstadoItem.RASCUNHO || referenciasExternas != 0) {
            throw new IllegalArgumentException("Somente rascunho sem referencias externas pode ser excluido. "
                    + "Para preservar o cadastro, use a inativacao.");
        }
    }
}

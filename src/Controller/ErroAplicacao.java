package Controller;

/**
 * Erro que a futura camada HTTP apresentara sem detalhes internos do banco.
 */
public class ErroAplicacao extends RuntimeException {
    private final int status;

    public ErroAplicacao(int status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public ErroAplicacao(int status, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}

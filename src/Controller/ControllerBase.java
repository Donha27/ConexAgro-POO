package Controller;

import Model.AcessoNegadoException;
import Model.OperacaoBloqueadaException;

import java.sql.SQLException;

abstract class ControllerBase {
    interface Acao<T> {
        T executar() throws SQLException;
    }

    protected <T> T tratar(Acao<T> acao) {
        try {
            return acao.executar();
        } catch (ErroAplicacao e) {
            throw e;
        } catch (AcessoNegadoException e) {
            throw new ErroAplicacao(403, e.getMessage(), e);
        } catch (OperacaoBloqueadaException e) {
            throw new ErroAplicacao(409, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ErroAplicacao(400, e.getMessage(), e);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new ErroAplicacao(409, "Ja existe um cadastro com esses dados.", e);
            }
            if (e.getErrorCode() == 1451) {
                throw new ErroAplicacao(409, "O cadastro esta em uso e deve ser preservado.", e);
            }
            if (e.getErrorCode() == 1452 || e.getErrorCode() == 3819) {
                throw new ErroAplicacao(400, "Confira os dados e os cadastros relacionados.", e);
            }
            if ("02000".equals(e.getSQLState())) {
                throw new ErroAplicacao(404, "Cadastro nao encontrado.", e);
            }
            throw new ErroAplicacao(500, "Nao foi possivel concluir a operacao no banco de dados.", e);
        }
    }

    protected static <T> T encontrado(T valor, String nome) {
        if (valor == null) {
            throw new ErroAplicacao(404, nome + " nao encontrado.");
        }
        return valor;
    }
}

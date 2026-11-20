package Web;

import Controller.ErroAplicacao;

import java.math.BigDecimal;
import java.util.Map;

final class Entrada {
    private Entrada() {
    }

    static String texto(Map<String, ?> dados, String chave) {
        Object v = dados.get(chave);
        if (v == null) return null;
        if (!(v instanceof String)) throw new ErroAplicacao(400, "Campo invalido: " + chave);
        return (String) v;
    }

    static String texto(Map<String, ?> dados, String chave, String padrao) {
        String v = texto(dados, chave);
        return v == null ? padrao : v;
    }

    static Long numero(Map<String, ?> dados, String chave) {
        Object v = dados.get(chave);
        if (v == null || v instanceof String && ((String) v).trim().isEmpty()) return null;
        try {
            return new BigDecimal(v.toString()).longValueExact();
        } catch (RuntimeException e) {
            throw new ErroAplicacao(400, "Numero invalido: " + chave);
        }
    }

    static long obrigatorio(Map<String, ?> dados, String chave) {
        Long v = numero(dados, chave);
        if (v == null) throw new ErroAplicacao(400, "Informe " + chave + ".");
        return v;
    }

    static Integer inteiro(Map<String, ?> dados, String chave) {
        Long v = numero(dados, chave);
        if (v == null) return null;
        if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) throw new ErroAplicacao(400, "Numero fora do limite.");
        return v.intValue();
    }

    static int inteiro(Map<String, ?> dados, String chave, int padrao) {
        Integer v = inteiro(dados, chave);
        return v == null ? padrao : v;
    }

    static boolean ativo(Map<String, ?> dados) {
        Object v = dados.get("ativo");
        if (v == null) return true;
        if (!(v instanceof Boolean)) throw new ErroAplicacao(400, "Atividade invalida.");
        return (Boolean) v;
    }

    static long id(String valor) {
        try {
            long id = Long.parseLong(valor);
            if (id < 1 || id > 4294967295L) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw new ErroAplicacao(400, "Identificador invalido.");
        }
    }
}

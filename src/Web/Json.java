package Web;

import java.math.BigDecimal;
import java.util.*;

/**
 * JSON restrito aos tipos utilizados pelas respostas dos Controllers.
 */
public final class Json {
    private Json() {
    }

    public static String escrever(Object valor) {
        if (valor == null) return "null";
        if (valor instanceof String) return citar((String) valor);
        if (valor instanceof Boolean) return valor.toString();
        if (valor instanceof Number) {
            String n = valor.toString();
            if (n.equals("NaN") || n.contains("Infinity")) throw new IllegalArgumentException("Numero invalido.");
            return n;
        }
        if (valor instanceof Map) {
            List<String> itens = new ArrayList<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) valor).entrySet()) {
                if (!(e.getKey() instanceof String)) throw new IllegalArgumentException("Chave JSON invalida.");
                itens.add(citar((String) e.getKey()) + ":" + escrever(e.getValue()));
            }
            return "{" + String.join(",", itens) + "}";
        }
        if (valor instanceof Iterable) {
            List<String> itens = new ArrayList<>();
            for (Object v : (Iterable<?>) valor) itens.add(escrever(v));
            return "[" + String.join(",", itens) + "]";
        }
        throw new IllegalArgumentException("Resposta deve conter somente dados de apresentacao.");
    }

    private static String citar(String s) {
        StringBuilder r = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    r.append("\\\"");
                    break;
                case '\\':
                    r.append("\\\\");
                    break;
                case '\n':
                    r.append("\\n");
                    break;
                case '\r':
                    r.append("\\r");
                    break;
                case '\t':
                    r.append("\\t");
                    break;
                default:
                    if (c < 32 || c == '<' || c == '\u2028' || c == '\u2029')
                        r.append(String.format("\\u%04x", (int) c));
                    else r.append(c);
            }
        }
        return r.append('"').toString();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> objeto(String texto) {
        Parser p = new Parser(texto);
        Object v = p.valor(0);
        p.espacos();
        if (p.pos != texto.length() || !(v instanceof Map)) throw p.erro();
        return (Map<String, Object>) v;
    }

    private static final class Parser {
        final String s;
        int pos;

        Parser(String s) {
            this.s = s;
        }

        IllegalArgumentException erro() {
            return new IllegalArgumentException("JSON invalido.");
        }

        void espacos() {
            while (pos < s.length() && " \t\n\r".indexOf(s.charAt(pos)) >= 0) pos++;
        }

        boolean pegar(char c) {
            espacos();
            if (pos < s.length() && s.charAt(pos) == c) {
                pos++;
                return true;
            }
            return false;
        }

        Object valor(int nivel) {
            if (nivel > 32) throw erro();
            espacos();
            if (pos >= s.length()) throw erro();
            char c = s.charAt(pos);
            if (c == '"') return string();
            if (c == '{') {
                pos++;
                Map<String, Object> r = new LinkedHashMap<>();
                if (pegar('}')) return r;
                do {
                    espacos();
                    String chave = string();
                    if (r.containsKey(chave) || !pegar(':')) throw erro();
                    r.put(chave, valor(nivel + 1));
                    if (pegar('}')) return r;
                } while (pegar(','));
                throw erro();
            }
            if (c == '[') {
                pos++;
                List<Object> r = new ArrayList<>();
                if (pegar(']')) return r;
                do {
                    r.add(valor(nivel + 1));
                    if (pegar(']')) return r;
                } while (pegar(','));
                throw erro();
            }
            for (String palavra : Arrays.asList("true", "false", "null")) {
                if (s.startsWith(palavra, pos)) {
                    pos += palavra.length();
                    return palavra.equals("null") ? null : Boolean.valueOf(palavra);
                }
            }
            int inicio = pos;
            while (pos < s.length() && "0123456789.eE+-".indexOf(s.charAt(pos)) >= 0) pos++;
            String n = s.substring(inicio, pos);
            if (!n.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")) throw erro();
            try {
                return new BigDecimal(n);
            } catch (NumberFormatException e) {
                throw erro();
            }
        }

        String string() {
            if (pos >= s.length() || s.charAt(pos++) != '"') throw erro();
            StringBuilder r = new StringBuilder();
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '"') return r.toString();
                if (c < 32) throw erro();
                if (c == '\\') {
                    if (pos >= s.length()) throw erro();
                    char e = s.charAt(pos++);
                    switch (e) {
                        case '"':
                        case '\\':
                        case '/':
                            r.append(e);
                            break;
                        case 'b':
                            r.append('\b');
                            break;
                        case 'f':
                            r.append('\f');
                            break;
                        case 'n':
                            r.append('\n');
                            break;
                        case 'r':
                            r.append('\r');
                            break;
                        case 't':
                            r.append('\t');
                            break;
                        case 'u':
                            if (pos + 4 > s.length()) throw erro();
                            try {
                                r.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                            } catch (NumberFormatException x) {
                                throw erro();
                            }
                            pos += 4;
                            break;
                        default:
                            throw erro();
                    }
                } else r.append(c);
            }
            throw erro();
        }
    }
}

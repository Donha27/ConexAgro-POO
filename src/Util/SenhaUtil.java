package Util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Hash de senha com PBKDF2-HMAC-SHA256 e salt aleatorio por senha.
 */
public final class SenhaUtil {
    private static final int ITERACOES = 600000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SenhaUtil() {
    }

    public static String gerarHash(String senha) {
        if (senha == null || senha.length() < 8 || senha.length() > 1024) {
            throw new IllegalArgumentException("Use uma senha entre 8 e 1024 caracteres.");
        }
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = derivar(senha, salt, ITERACOES);
        return "pbkdf2_sha256$" + ITERACOES + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean formatoValido(String hash) {
        try {
            String[] partes = hash.split("\\$", -1);
            return partes.length == 4 && partes[0].equals("pbkdf2_sha256")
                    && Integer.parseInt(partes[1]) == ITERACOES
                    && Base64.getDecoder().decode(partes[2]).length == 16
                    && Base64.getDecoder().decode(partes[3]).length == 32;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static boolean verificar(String senha, String hash) {
        if (senha == null || senha.length() > 1024 || !formatoValido(hash)) {
            return false;
        }
        String[] partes = hash.split("\\$", -1);
        return MessageDigest.isEqual(Base64.getDecoder().decode(partes[3]),
                derivar(senha, Base64.getDecoder().decode(partes[2]), ITERACOES));
    }

    private static byte[] derivar(String senha, byte[] salt, int iteracoes) {
        char[] caracteres = senha.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(caracteres, salt, iteracoes, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2-HMAC-SHA256 indisponivel neste Java.", e);
        } finally {
            spec.clearPassword();
            Arrays.fill(caracteres, '\0');
        }
    }
}

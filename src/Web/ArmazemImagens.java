package Web;

import Controller.ErroAplicacao;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

final class ArmazemImagens {
    private final Path pasta;

    ArmazemImagens(Path pasta) {
        this.pasta = pasta.toAbsolutePath().normalize();
    }

    String guardar(byte[] bytes) throws IOException {
        String ext = null;
        if (bytes.length >= 8 && (bytes[0] & 255) == 137 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71 && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10)
            ext = "png";
        else if (bytes.length >= 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255)
            ext = "jpg";
        else if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P')
            ext = "webp";
        if (ext == null) throw new ErroAplicacao(400, "Envie uma imagem PNG, JPEG ou WebP.");
        Files.createDirectories(pasta);
        String nome = UUID.randomUUID() + "." + ext;
        Files.write(pasta.resolve(nome), bytes, StandardOpenOption.CREATE_NEW);
        return "/uploads/" + nome;
    }

    Path localizar(String caminho) throws IOException {
        String nome = caminho.substring("/uploads/".length());
        if (!nome.matches("[0-9a-f-]{36}\\.(png|jpg|webp)")) throw new ErroAplicacao(404, "Imagem nao encontrada.");
        Path arquivo = pasta.resolve(nome).normalize();
        if (!Files.isRegularFile(arquivo) || !arquivo.toRealPath().startsWith(pasta.toRealPath()))
            throw new ErroAplicacao(404, "Imagem nao encontrada.");
        return arquivo;
    }

    void desfazer(String url) {
        try {
            Files.deleteIfExists(pasta.resolve(url.substring("/uploads/".length())));
        } catch (IOException ignored) {
        }
    }
}

package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FichaPeca {
    private final Peca peca;
    private final List<Imagem> imagens;
    private final List<Especificacao> especificacoes;
    private final List<CodigoPeca> codigos;
    private final List<Compatibilidade> compatibilidades;

    public FichaPeca(Peca peca, List<Imagem> imagens, List<Especificacao> especificacoes,
                     List<CodigoPeca> codigos, List<Compatibilidade> compatibilidades) {
        this.peca = Validacao.referencia(peca, "Peca");
        this.imagens = Collections.unmodifiableList(new ArrayList<>(imagens));
        this.especificacoes = Collections.unmodifiableList(new ArrayList<>(especificacoes));
        this.codigos = Collections.unmodifiableList(new ArrayList<>(codigos));
        this.compatibilidades = Collections.unmodifiableList(new ArrayList<>(compatibilidades));
    }

    public Peca getPeca() {
        return peca;
    }

    public List<Imagem> getImagens() {
        return imagens;
    }

    public List<Especificacao> getEspecificacoes() {
        return especificacoes;
    }

    public List<CodigoPeca> getCodigos() {
        return codigos;
    }

    public List<Compatibilidade> getCompatibilidades() {
        return compatibilidades;
    }
}

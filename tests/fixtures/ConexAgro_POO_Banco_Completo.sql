-- CONEXAGRO - BANCO COMPLETO PARA A ENTREGA DE PROGRAMACAO ORIENTADA A OBJETOS
-- Autor: Gabriel de Jesus Coelho Donha
-- Versao: 1.1 - 19/11/2026 | Requisito: MySQL 8.0.16 ou superior.
--
-- FINALIDADE
-- Instalar a estrutura e os dados de demonstracao compativeis com o projeto Java
-- ConexAgro (Model, View, Controller, DAO, ConnectionFactory e Principal).
-- Este arquivo utiliza SOMENTE o banco conexagro. Ele nao utiliza conexagro_bd1.
-- As operacoes do CRUD do trabalho devem ser realizadas no programa Java.
--
-- COMO O PROFESSOR PODE TESTAR
-- 1. No MySQL Workbench, abra este arquivo e execute o script INTEIRO.
--    A pasta dados/imagens deve acompanhar o projeto Java, junto com src e web.
--    O usuario do MySQL precisa poder criar banco, tabelas e a rotina temporaria
--    de importacao (CREATE ROUTINE / EXECUTE / ALTER ROUTINE).
-- 2. No projeto Java, copie conexao.properties.example para conexao.properties
--    caso o arquivo de configuracao ainda nao exista. Preencha os dados locais:
--       db.url=jdbc:mysql://localhost:3306/conexagro
--       db.user=SEU_USUARIO_MYSQL
--       db.password=SUA_SENHA_MYSQL
--    Use Java 17 ou superior e adicione o MySQL Connector/J as bibliotecas.
--    O diretorio de trabalho do IntelliJ deve ser a raiz do projeto (src e web).
-- 3. Execute a classe Principal. No PRIMEIRO ACESSO de um banco novo, abra o
--    endereco temporario de configuracao exibido no console e crie o administrador.
--    Nao ha usuario nem senha administrativos fixos neste arquivo.
-- 4. Entre no site local informado no console. Em Cadastros de apoio > Categorias,
--    teste cadastrar, listar/consultar, alterar e excluir categorias.
--    Em Pecas, teste as mesmas operacoes; a exclusao fisica e para rascunhos.
-- 5. Ha uma categoria 'POO - testes CRUD' e a peca 'Peca para teste CRUD [DEMO]',
--    codigo CX-POO-CRUD-001. O professor pode alterar esses cadastros. Tentar excluir
--    a categoria enquanto a peca estiver ligada a ela demonstra a protecao da FK.
--    Exclua o rascunho primeiro e depois a categoria. Tambem pode criar registros novos.
--
-- CONTEUDO EM UMA INSTALACAO NOVA
-- 15 tabelas com PKs, FKs, unicidade, CHECKs e indices.
-- Heranca persistida: item_catalogo e os subtipos peca e maquina compartilham o ID.
-- Relacionamento Categoria -> ItemCatalogo -> Peca, usado no CRUD da apresentacao.
-- 10 pecas publicadas, 1 peca em rascunho para testes, 4 maquinas ficticias,
-- 2 marcas, 7 categorias, 1 origem de codigo, 11 codigos, 20 especificacoes,
-- 10 imagens demonstrativas e 12 compatibilidades ficticias.
-- As imagens de demonstracao usam as dez ilustracoes da pasta dados/imagens,
-- ja fornecida com o pacote de cadastros e presente no projeto do aluno.
-- Inclua essa pasta ao enviar o projeto Java ao professor. O SQL guarda os
-- caminhos /uploads/..., e o servidor Java atende os arquivos de dados/imagens.
-- Esta versao tambem corrige as miniaturas da versao 1.0: garante a ilustracao
-- correta de cada peca DEMO e remove somente sua imagem provisoria /logo.svg.
--
-- REEXECUCAO
-- Este arquivo NAO apaga bancos, tabelas, contas, pecas ou categorias existentes.
-- Remove somente imagens /logo.svg das pecas DEMO apos inserir suas ilustracoes.
-- Em uma base compativel, reutiliza os registros DEMO e preserva campos editados.
-- Registros de teste apagados podem ser recriados ao executar a carga novamente.
-- CREATE TABLE IF NOT EXISTS nao realiza migracoes em estruturas diferentes.
-- A rotina de carga e temporaria: criada, executada e removida neste arquivo.
-- Seus comandos organizam a importacao; o CRUD da aplicacao continua nos DAOs Java.
--
-- Dados, medidas, codigos e compatibilidades sao FICTICIOS, sem validade comercial.
-- Este SQL nao contem senhas reais, contas do computador ou credenciais do aluno.
-- ---------------------------------------------------------------------------

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET @cx_poo_instalacao_ok=0;

-- PARTE 1 - ESTRUTURA DO BANCO E RELACIONAMENTOS

CREATE DATABASE IF NOT EXISTS conexagro
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE conexagro;

-- 1. Conta: ocupacao descreve a pessoa; perfil_acesso determina permissao.
CREATE TABLE IF NOT EXISTS usuario (
    id_usuario INT UNSIGNED AUTO_INCREMENT,
    nome_completo VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    ocupacao ENUM('vendedor', 'agricultor', 'mecanico') NULL,
    perfil_acesso ENUM('usuario', 'administrador') NOT NULL DEFAULT 'usuario',
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_nome CHECK (CHAR_LENGTH(TRIM(nome_completo)) > 0),
    CONSTRAINT ck_usuario_email CHECK (
        CHAR_LENGTH(email) > 0
        AND BINARY email = BINARY LOWER(TRIM(email))
        AND LOCATE(' ', email) = 0
    ),
    CONSTRAINT ck_usuario_hash CHECK (CHAR_LENGTH(TRIM(senha_hash)) > 0),
    CONSTRAINT ck_usuario_ocupacao CHECK (
        perfil_acesso = 'administrador' OR ocupacao IS NOT NULL
    ),
    CONSTRAINT ck_usuario_ativo CHECK (ativo IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. Fabricante/marca de pecas, maquinas e catalogos.
CREATE TABLE IF NOT EXISTS marca (
    id_marca INT UNSIGNED AUTO_INCREMENT,
    nome_marca VARCHAR(120) NOT NULL,
    CONSTRAINT pk_marca PRIMARY KEY (id_marca),
    CONSTRAINT uq_marca_nome UNIQUE (nome_marca),
    CONSTRAINT ck_marca_nome CHECK (CHAR_LENGTH(TRIM(nome_marca)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. Substitui o texto livre da categoria da peca.
CREATE TABLE IF NOT EXISTS categoria (
    id_categoria INT UNSIGNED AUTO_INCREMENT,
    nome_categoria VARCHAR(80) NOT NULL,
    CONSTRAINT pk_categoria PRIMARY KEY (id_categoria),
    CONSTRAINT uq_categoria_nome UNIQUE (nome_categoria),
    CONSTRAINT ck_categoria_nome CHECK (CHAR_LENGTH(TRIM(nome_categoria)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 4. Dados comuns a Peca e Maquina. O ID e compartilhado com a tabela filha.
CREATE TABLE IF NOT EXISTS item_catalogo (
    id_item INT UNSIGNED AUTO_INCREMENT,
    tipo_item ENUM('peca', 'maquina') NOT NULL,
    nome VARCHAR(150) NOT NULL,
    descricao TEXT NULL,
    estado ENUM('rascunho', 'publicado', 'inativo') NOT NULL DEFAULT 'rascunho',
    id_marca INT UNSIGNED NULL,
    id_categoria INT UNSIGNED NULL,
    id_usuario_cadastro INT UNSIGNED NULL,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_item_catalogo PRIMARY KEY (id_item),
    CONSTRAINT uq_item_catalogo_tipo UNIQUE (id_item, tipo_item),
    CONSTRAINT fk_item_marca FOREIGN KEY (id_marca)
        REFERENCES marca (id_marca) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_item_categoria FOREIGN KEY (id_categoria)
        REFERENCES categoria (id_categoria) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_item_usuario FOREIGN KEY (id_usuario_cadastro)
        REFERENCES usuario (id_usuario) ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT ck_item_nome CHECK (CHAR_LENGTH(TRIM(nome)) > 0),
    CONSTRAINT ck_item_categoria CHECK (
        tipo_item <> 'peca' OR id_categoria IS NOT NULL
    ),
    CONSTRAINT ck_item_marca_maquina CHECK (
        tipo_item <> 'maquina' OR id_marca IS NOT NULL
    ),
    CONSTRAINT ck_item_publicado_campos CHECK (
        estado <> 'publicado' OR (
            id_marca IS NOT NULL AND descricao IS NOT NULL
            AND CHAR_LENGTH(TRIM(descricao)) > 0
        )
    ),
    INDEX idx_item_pesquisa (tipo_item, estado, id_marca)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 5. Subtipo Peca. O codigo comercial vai em codigo_peca, nunca na PK.
-- O discriminador fixo + FK composta impede usar um item maquina como peca.
CREATE TABLE IF NOT EXISTS peca (
    id_peca INT UNSIGNED NOT NULL,
    tipo_item ENUM('peca', 'maquina') NOT NULL DEFAULT 'peca',
    CONSTRAINT pk_peca PRIMARY KEY (id_peca),
    CONSTRAINT ck_peca_tipo CHECK (tipo_item = 'peca'),
    CONSTRAINT fk_peca_item FOREIGN KEY (id_peca, tipo_item)
        REFERENCES item_catalogo (id_item, tipo_item)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 6. Subtipo Maquina: uma marca pode ter varios modelos.
CREATE TABLE IF NOT EXISTS maquina (
    id_maquina INT UNSIGNED NOT NULL,
    tipo_item ENUM('peca', 'maquina') NOT NULL DEFAULT 'maquina',
    modelo VARCHAR(80) NOT NULL,
    tipo_maquina VARCHAR(80) NOT NULL,
    ano SMALLINT UNSIGNED NULL,
    serie VARCHAR(100) NULL,
    motor VARCHAR(100) NULL,
    versao VARCHAR(100) NULL,
    CONSTRAINT pk_maquina PRIMARY KEY (id_maquina),
    CONSTRAINT ck_maquina_tipo CHECK (tipo_item = 'maquina'),
    CONSTRAINT ck_maquina_modelo CHECK (CHAR_LENGTH(TRIM(modelo)) > 0),
    CONSTRAINT ck_maquina_tipo_nome CHECK (CHAR_LENGTH(TRIM(tipo_maquina)) > 0),
    CONSTRAINT ck_maquina_ano CHECK (ano IS NULL OR ano BETWEEN 1900 AND 2200),
    CONSTRAINT fk_maquina_item FOREIGN KEY (id_maquina, tipo_item)
        REFERENCES item_catalogo (id_item, tipo_item)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX idx_maquina_modelo (modelo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 7. Unifica Imagem_peca e Imagem_maquina; cada imagem pertence a um item.
CREATE TABLE IF NOT EXISTS imagem (
    id_imagem INT UNSIGNED AUTO_INCREMENT,
    id_item INT UNSIGNED NOT NULL,
    url_imagem VARCHAR(2048) NOT NULL,
    descricao_imagem VARCHAR(255) NULL,
    CONSTRAINT pk_imagem PRIMARY KEY (id_imagem),
    CONSTRAINT fk_imagem_item FOREIGN KEY (id_item)
        REFERENCES item_catalogo (id_item) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_imagem_url CHECK (CHAR_LENGTH(TRIM(url_imagem)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 8. Caracteristica, valor e unidade: ex. diametro externo / 80 / mm.
-- Texto admite tambem caracteristicas como material / aco.
CREATE TABLE IF NOT EXISTS especificacao (
    id_especificacao INT UNSIGNED AUTO_INCREMENT,
    id_item INT UNSIGNED NOT NULL,
    nome_especificacao VARCHAR(80) NOT NULL,
    valor_especificacao VARCHAR(255) NOT NULL,
    unidade VARCHAR(30) NULL,
    CONSTRAINT pk_especificacao PRIMARY KEY (id_especificacao),
    CONSTRAINT fk_especificacao_item FOREIGN KEY (id_item)
        REFERENCES item_catalogo (id_item) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_especificacao_nome CHECK (CHAR_LENGTH(TRIM(nome_especificacao)) > 0),
    CONSTRAINT ck_especificacao_valor CHECK (CHAR_LENGTH(TRIM(valor_especificacao)) > 0),
    CONSTRAINT ck_especificacao_unidade CHECK (
        unidade IS NULL OR CHAR_LENGTH(TRIM(unidade)) > 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 9. Emissor da referencia: fabricante ou fornecedor.
CREATE TABLE IF NOT EXISTS origem_codigo (
    id_origem INT UNSIGNED AUTO_INCREMENT,
    nome_origem VARCHAR(120) NOT NULL,
    tipo_origem ENUM('fabricante', 'fornecedor') NOT NULL,
    CONSTRAINT pk_origem_codigo PRIMARY KEY (id_origem),
    CONSTRAINT uq_origem_codigo UNIQUE (nome_origem, tipo_origem),
    CONSTRAINT ck_origem_nome CHECK (CHAR_LENGTH(TRIM(nome_origem)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 10. Uma peca pode ter varios codigos; zeros e pontuacao sao preservados.
-- O campo gerado remove espacos simples e ignora caixa para a unicidade.
-- A collation ignora acentos. A aplicacao normalizara os demais espacos.
CREATE TABLE IF NOT EXISTS codigo_peca (
    id_codigo INT UNSIGNED AUTO_INCREMENT,
    id_peca INT UNSIGNED NOT NULL,
    id_marca INT UNSIGNED NOT NULL,
    id_origem INT UNSIGNED NOT NULL,
    codigo VARCHAR(80) NOT NULL,
    codigo_normalizado VARCHAR(80)
        GENERATED ALWAYS AS (UPPER(REPLACE(TRIM(codigo), ' ', ''))) STORED,
    tipo_codigo ENUM('original', 'paralelo', 'fornecedor') NOT NULL DEFAULT 'original',
    CONSTRAINT pk_codigo_peca PRIMARY KEY (id_codigo),
    CONSTRAINT uq_codigo_contexto UNIQUE (codigo_normalizado, id_marca, id_origem),
    CONSTRAINT fk_codigo_peca FOREIGN KEY (id_peca)
        REFERENCES peca (id_peca) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_codigo_marca FOREIGN KEY (id_marca)
        REFERENCES marca (id_marca) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_codigo_origem FOREIGN KEY (id_origem)
        REFERENCES origem_codigo (id_origem) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_codigo_conteudo CHECK (
        CHAR_LENGTH(REPLACE(TRIM(codigo), ' ', '')) > 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 11. Cada catalogo de fabricante pertence a uma marca.
CREATE TABLE IF NOT EXISTS catalogo (
    id_catalogo INT UNSIGNED AUTO_INCREMENT,
    nome_catalogo VARCHAR(150) NOT NULL,
    versao VARCHAR(30) NULL,
    ano SMALLINT UNSIGNED NULL,
    arquivo_url VARCHAR(2048) NOT NULL,
    id_marca INT UNSIGNED NOT NULL,
    distribuicao_autorizada BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_catalogo PRIMARY KEY (id_catalogo),
    CONSTRAINT fk_catalogo_marca FOREIGN KEY (id_marca)
        REFERENCES marca (id_marca) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_catalogo_nome CHECK (CHAR_LENGTH(TRIM(nome_catalogo)) > 0),
    CONSTRAINT ck_catalogo_url CHECK (CHAR_LENGTH(TRIM(arquivo_url)) > 0),
    CONSTRAINT ck_catalogo_ano CHECK (ano IS NULL OR ano BETWEEN 1900 AND 2200),
    CONSTRAINT ck_catalogo_ativo CHECK (ativo IN (0, 1)),
    CONSTRAINT ck_catalogo_distribuicao CHECK (distribuicao_autorizada IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 12. Um item pode estar em varios catalogos e cada catalogo tem varios itens.
CREATE TABLE IF NOT EXISTS catalogo_item (
    id_catalogo INT UNSIGNED NOT NULL,
    id_item INT UNSIGNED NOT NULL,
    pagina VARCHAR(30) NULL,
    CONSTRAINT pk_catalogo_item PRIMARY KEY (id_catalogo, id_item),
    CONSTRAINT fk_catalogo_item_catalogo FOREIGN KEY (id_catalogo)
        REFERENCES catalogo (id_catalogo) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_catalogo_item_item FOREIGN KEY (id_item)
        REFERENCES item_catalogo (id_item) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 13. Relacao N:N que substitui "E compativel com" do modelo logico.
-- Nao presumir aplicacao real a partir de dados ficticios ou similares.
CREATE TABLE IF NOT EXISTS compatibilidade (
    id_peca INT UNSIGNED NOT NULL,
    id_maquina INT UNSIGNED NOT NULL,
    estado ENUM('pendente', 'confirmada') NOT NULL DEFAULT 'pendente',
    fonte VARCHAR(500) NULL,
    id_catalogo INT UNSIGNED NULL,
    ano_inicio SMALLINT UNSIGNED NULL,
    ano_fim SMALLINT UNSIGNED NULL,
    restricao_serie VARCHAR(150) NULL,
    restricao_motor VARCHAR(150) NULL,
    restricao_versao VARCHAR(150) NULL,
    observacoes TEXT NULL,
    data_confirmacao DATETIME NULL,
    CONSTRAINT pk_compatibilidade PRIMARY KEY (id_peca, id_maquina),
    CONSTRAINT fk_compatibilidade_peca FOREIGN KEY (id_peca)
        REFERENCES peca (id_peca) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_compatibilidade_maquina FOREIGN KEY (id_maquina)
        REFERENCES maquina (id_maquina) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_compatibilidade_catalogo FOREIGN KEY (id_catalogo)
        REFERENCES catalogo (id_catalogo) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_compatibilidade_fonte CHECK (
        estado <> 'confirmada' OR (
            fonte IS NOT NULL AND CHAR_LENGTH(TRIM(fonte)) > 0
            AND data_confirmacao IS NOT NULL
        )
    ),
    CONSTRAINT ck_compatibilidade_anos CHECK (
        (ano_inicio IS NULL OR ano_inicio BETWEEN 1900 AND 2200)
        AND (ano_fim IS NULL OR ano_fim BETWEEN 1900 AND 2200)
        AND (ano_inicio IS NULL OR ano_fim IS NULL OR ano_inicio <= ano_fim)
    ),
    INDEX idx_compatibilidade_maquina (id_maquina, estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 14. Ligacao usuario-catalogo prevista no PDF. Apagar uma conta remove
-- seus favoritos, mas nunca remove as pecas ou maquinas favoritados.
CREATE TABLE IF NOT EXISTS favorito (
    id_usuario INT UNSIGNED NOT NULL,
    id_item INT UNSIGNED NOT NULL,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_favorito PRIMARY KEY (id_usuario, id_item),
    CONSTRAINT fk_favorito_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_favorito_item FOREIGN KEY (id_item)
        REFERENCES item_catalogo (id_item) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 15. Equivalencia do modelo original, guardada uma unica vez (A menor que B).
-- A consulta deve considerar os dois sentidos; A-B e B-C nao implicam A-C.
CREATE TABLE IF NOT EXISTS equivalencia_peca (
    id_peca_a INT UNSIGNED NOT NULL,
    id_peca_b INT UNSIGNED NOT NULL,
    tipo_equivalencia VARCHAR(80) NOT NULL,
    estado ENUM('pendente', 'confirmada') NOT NULL DEFAULT 'pendente',
    fonte_fabricante VARCHAR(500) NULL,
    data_confirmacao DATETIME NULL,
    CONSTRAINT pk_equivalencia_peca PRIMARY KEY (id_peca_a, id_peca_b),
    CONSTRAINT fk_equivalencia_a FOREIGN KEY (id_peca_a)
        REFERENCES peca (id_peca) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_equivalencia_b FOREIGN KEY (id_peca_b)
        REFERENCES peca (id_peca) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_equivalencia_ordem CHECK (id_peca_a < id_peca_b),
    CONSTRAINT ck_equivalencia_tipo CHECK (CHAR_LENGTH(TRIM(tipo_equivalencia)) > 0),
    CONSTRAINT ck_equivalencia_fonte CHECK (
        estado <> 'confirmada' OR (
            fonte_fabricante IS NOT NULL
            AND CHAR_LENGTH(TRIM(fonte_fabricante)) > 0
            AND data_confirmacao IS NOT NULL
        )
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- PARTE 2 - DADOS FICTICIOS DE DEMONSTRACAO

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE conexagro;

DROP PROCEDURE IF EXISTS cx_instalar_dados_poo_20261119;
DELIMITER $$
CREATE PROCEDURE cx_instalar_dados_poo_20261119()
SQL SECURITY INVOKER
BEGIN
    DECLARE v_fim BOOLEAN DEFAULT FALSE;
    DECLARE v_lock INT DEFAULT 0;
    DECLARE v_quantidade INT DEFAULT 0;
    DECLARE v_id INT UNSIGNED;
    DECLARE v_autor INT UNSIGNED;
    DECLARE v_marca INT UNSIGNED;
    DECLARE v_categoria INT UNSIGNED;
    DECLARE v_origem INT UNSIGNED;
    DECLARE v_maquina INT UNSIGNED;
    DECLARE v_nome VARCHAR(150);
    DECLARE v_modelo VARCHAR(80);
    DECLARE v_tipo VARCHAR(80);
    DECLARE v_nome_marca VARCHAR(120);
    DECLARE v_nome_categoria VARCHAR(80);
    DECLARE v_ano SMALLINT UNSIGNED;
    DECLARE v_codigo VARCHAR(80);
    DECLARE v_spec VARCHAR(80);
    DECLARE v_valor VARCHAR(255);
    DECLARE v_unidade VARCHAR(30);
    DECLARE v_url VARCHAR(2048);

    DECLARE c_maquinas CURSOR FOR
        SELECT nome,modelo,tipo,nome_marca,ano FROM cx_demo_maquinas ORDER BY modelo;
    DECLARE c_pecas CURSOR FOR
        SELECT codigo,nome,nome_categoria,nome_marca,modelo,spec,valor,unidade,url
        FROM cx_demo_pecas ORDER BY codigo;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_fim=TRUE;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        DROP TEMPORARY TABLE IF EXISTS cx_demo_pecas;
        DROP TEMPORARY TABLE IF EXISTS cx_demo_maquinas;
        IF v_lock=1 THEN DO RELEASE_LOCK('conexagro_carga_poo_20261119'); END IF;
        RESIGNAL;
    END;


    -- Confere os campos usados pelos DAOs antes de inserir qualquer registro.
    IF @@FOREIGN_KEY_CHECKS <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Ative FOREIGN_KEY_CHECKS=1 e execute novamente.';
    END IF;
    IF EXISTS (
        SELECT 1 FROM (
        SELECT 'usuario' AS tabela, 'id_usuario' AS coluna
        UNION ALL SELECT 'usuario' AS tabela, 'nome_completo' AS coluna
        UNION ALL SELECT 'usuario' AS tabela, 'email' AS coluna
        UNION ALL SELECT 'usuario' AS tabela, 'senha_hash' AS coluna
        UNION ALL SELECT 'usuario' AS tabela, 'ocupacao' AS coluna
        UNION ALL SELECT 'usuario' AS tabela, 'perfil_acesso' AS coluna
        UNION ALL SELECT 'usuario' AS tabela, 'ativo' AS coluna
        UNION ALL SELECT 'usuario' AS tabela, 'criado_em' AS coluna
        UNION ALL SELECT 'marca' AS tabela, 'id_marca' AS coluna
        UNION ALL SELECT 'marca' AS tabela, 'nome_marca' AS coluna
        UNION ALL SELECT 'categoria' AS tabela, 'id_categoria' AS coluna
        UNION ALL SELECT 'categoria' AS tabela, 'nome_categoria' AS coluna
        UNION ALL SELECT 'item_catalogo' AS tabela, 'id_item' AS coluna
        UNION ALL SELECT 'item_catalogo' AS tabela, 'tipo_item' AS coluna
        UNION ALL SELECT 'item_catalogo' AS tabela, 'nome' AS coluna
        UNION ALL SELECT 'item_catalogo' AS tabela, 'descricao' AS coluna
        UNION ALL SELECT 'item_catalogo' AS tabela, 'estado' AS coluna
        UNION ALL SELECT 'item_catalogo' AS tabela, 'id_marca' AS coluna
        UNION ALL SELECT 'item_catalogo' AS tabela, 'id_categoria' AS coluna
        UNION ALL SELECT 'item_catalogo' AS tabela, 'id_usuario_cadastro' AS coluna
        UNION ALL SELECT 'item_catalogo' AS tabela, 'criado_em' AS coluna
        UNION ALL SELECT 'item_catalogo' AS tabela, 'atualizado_em' AS coluna
        UNION ALL SELECT 'peca' AS tabela, 'id_peca' AS coluna
        UNION ALL SELECT 'peca' AS tabela, 'tipo_item' AS coluna
        UNION ALL SELECT 'maquina' AS tabela, 'id_maquina' AS coluna
        UNION ALL SELECT 'maquina' AS tabela, 'tipo_item' AS coluna
        UNION ALL SELECT 'maquina' AS tabela, 'modelo' AS coluna
        UNION ALL SELECT 'maquina' AS tabela, 'tipo_maquina' AS coluna
        UNION ALL SELECT 'maquina' AS tabela, 'ano' AS coluna
        UNION ALL SELECT 'maquina' AS tabela, 'serie' AS coluna
        UNION ALL SELECT 'maquina' AS tabela, 'motor' AS coluna
        UNION ALL SELECT 'maquina' AS tabela, 'versao' AS coluna
        UNION ALL SELECT 'imagem' AS tabela, 'id_imagem' AS coluna
        UNION ALL SELECT 'imagem' AS tabela, 'id_item' AS coluna
        UNION ALL SELECT 'imagem' AS tabela, 'url_imagem' AS coluna
        UNION ALL SELECT 'imagem' AS tabela, 'descricao_imagem' AS coluna
        UNION ALL SELECT 'especificacao' AS tabela, 'id_especificacao' AS coluna
        UNION ALL SELECT 'especificacao' AS tabela, 'id_item' AS coluna
        UNION ALL SELECT 'especificacao' AS tabela, 'nome_especificacao' AS coluna
        UNION ALL SELECT 'especificacao' AS tabela, 'valor_especificacao' AS coluna
        UNION ALL SELECT 'especificacao' AS tabela, 'unidade' AS coluna
        UNION ALL SELECT 'origem_codigo' AS tabela, 'id_origem' AS coluna
        UNION ALL SELECT 'origem_codigo' AS tabela, 'nome_origem' AS coluna
        UNION ALL SELECT 'origem_codigo' AS tabela, 'tipo_origem' AS coluna
        UNION ALL SELECT 'codigo_peca' AS tabela, 'id_codigo' AS coluna
        UNION ALL SELECT 'codigo_peca' AS tabela, 'id_peca' AS coluna
        UNION ALL SELECT 'codigo_peca' AS tabela, 'id_marca' AS coluna
        UNION ALL SELECT 'codigo_peca' AS tabela, 'id_origem' AS coluna
        UNION ALL SELECT 'codigo_peca' AS tabela, 'codigo' AS coluna
        UNION ALL SELECT 'codigo_peca' AS tabela, 'codigo_normalizado' AS coluna
        UNION ALL SELECT 'codigo_peca' AS tabela, 'tipo_codigo' AS coluna
        UNION ALL SELECT 'catalogo' AS tabela, 'id_catalogo' AS coluna
        UNION ALL SELECT 'catalogo' AS tabela, 'nome_catalogo' AS coluna
        UNION ALL SELECT 'catalogo' AS tabela, 'versao' AS coluna
        UNION ALL SELECT 'catalogo' AS tabela, 'ano' AS coluna
        UNION ALL SELECT 'catalogo' AS tabela, 'arquivo_url' AS coluna
        UNION ALL SELECT 'catalogo' AS tabela, 'id_marca' AS coluna
        UNION ALL SELECT 'catalogo' AS tabela, 'distribuicao_autorizada' AS coluna
        UNION ALL SELECT 'catalogo' AS tabela, 'ativo' AS coluna
        UNION ALL SELECT 'catalogo_item' AS tabela, 'id_catalogo' AS coluna
        UNION ALL SELECT 'catalogo_item' AS tabela, 'id_item' AS coluna
        UNION ALL SELECT 'catalogo_item' AS tabela, 'pagina' AS coluna
        UNION ALL SELECT 'compatibilidade' AS tabela, 'id_peca' AS coluna
        UNION ALL SELECT 'compatibilidade' AS tabela, 'id_maquina' AS coluna
        UNION ALL SELECT 'compatibilidade' AS tabela, 'estado' AS coluna
        UNION ALL SELECT 'compatibilidade' AS tabela, 'fonte' AS coluna
        UNION ALL SELECT 'compatibilidade' AS tabela, 'id_catalogo' AS coluna
        UNION ALL SELECT 'compatibilidade' AS tabela, 'ano_inicio' AS coluna
        UNION ALL SELECT 'compatibilidade' AS tabela, 'ano_fim' AS coluna
        UNION ALL SELECT 'compatibilidade' AS tabela, 'restricao_serie' AS coluna
        UNION ALL SELECT 'compatibilidade' AS tabela, 'restricao_motor' AS coluna
        UNION ALL SELECT 'compatibilidade' AS tabela, 'restricao_versao' AS coluna
        UNION ALL SELECT 'compatibilidade' AS tabela, 'observacoes' AS coluna
        UNION ALL SELECT 'compatibilidade' AS tabela, 'data_confirmacao' AS coluna
        UNION ALL SELECT 'favorito' AS tabela, 'id_usuario' AS coluna
        UNION ALL SELECT 'favorito' AS tabela, 'id_item' AS coluna
        UNION ALL SELECT 'favorito' AS tabela, 'criado_em' AS coluna
        UNION ALL SELECT 'equivalencia_peca' AS tabela, 'id_peca_a' AS coluna
        UNION ALL SELECT 'equivalencia_peca' AS tabela, 'id_peca_b' AS coluna
        UNION ALL SELECT 'equivalencia_peca' AS tabela, 'tipo_equivalencia' AS coluna
        UNION ALL SELECT 'equivalencia_peca' AS tabela, 'estado' AS coluna
        UNION ALL SELECT 'equivalencia_peca' AS tabela, 'fonte_fabricante' AS coluna
        UNION ALL SELECT 'equivalencia_peca' AS tabela, 'data_confirmacao' AS coluna
        ) requisitos
        LEFT JOIN information_schema.columns col
          ON col.table_schema=DATABASE()
         AND col.table_name=requisitos.tabela AND col.column_name=requisitos.coluna
        WHERE col.column_name IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Banco conexagro com estrutura diferente do projeto POO. A carga de dados foi cancelada.';
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.key_column_usage
        WHERE table_schema=DATABASE() AND table_name='item_catalogo'
          AND column_name='id_categoria' AND referenced_table_name='categoria'
          AND referenced_column_name='id_categoria')
       OR NOT EXISTS(SELECT 1 FROM information_schema.key_column_usage
        WHERE table_schema=DATABASE() AND table_name='peca'
          AND column_name='id_peca' AND referenced_table_name='item_catalogo'
          AND referenced_column_name='id_item') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Relacionamentos de categoria, item e peca diferentes do projeto POO.';
    END IF;

    SELECT GET_LOCK('conexagro_carga_poo_20261119',10) INTO v_lock;
    IF v_lock IS NULL OR v_lock<>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Outra carga DEMO esta em execucao. Tente novamente depois.';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS cx_demo_maquinas;
    DROP TEMPORARY TABLE IF EXISTS cx_demo_pecas;
    CREATE TEMPORARY TABLE cx_demo_maquinas (
        modelo VARCHAR(80) PRIMARY KEY,nome VARCHAR(150) NOT NULL,tipo VARCHAR(80) NOT NULL,
        nome_marca VARCHAR(120) NOT NULL,ano SMALLINT UNSIGNED NOT NULL,id_item INT UNSIGNED
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
    CREATE TEMPORARY TABLE cx_demo_pecas (
        codigo VARCHAR(80) PRIMARY KEY,nome VARCHAR(150) NOT NULL,nome_categoria VARCHAR(80) NOT NULL,
        nome_marca VARCHAR(120) NOT NULL,modelo VARCHAR(80) NOT NULL,
        spec VARCHAR(80) NOT NULL,valor VARCHAR(255) NOT NULL,unidade VARCHAR(30),
        url VARCHAR(2048) NOT NULL,id_item INT UNSIGNED
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

    INSERT INTO cx_demo_maquinas(modelo,nome,tipo,nome_marca,ano) VALUES
        ('TC-100 (DEMO)','Trator TC-100 [DEMO]','Trator','AgroCampo (DEMO)',2020),
        ('TM-200 (DEMO)','Colheitadeira TM-200 [DEMO]','Colheitadeira','TerraMec (DEMO)',2021),
        ('AC-300 (DEMO)','Pulverizador AC-300 [DEMO]','Pulverizador','AgroCampo (DEMO)',2022),
        ('TM-400 (DEMO)','Plantadeira TM-400 [DEMO]','Plantadeira','TerraMec (DEMO)',2023);

    INSERT INTO cx_demo_pecas(codigo,nome,nome_categoria,nome_marca,modelo,spec,valor,unidade,url) VALUES
    ('CX-DEMO-001', 'Filtro de óleo [DEMO]', 'Filtros', 'AgroCampo (DEMO)', 'TC-100 (DEMO)', 'Altura', '140', 'mm', '/uploads/660934d1-0be5-5e60-9bef-6b4f3b9932ad.png'),
    ('CX-DEMO-002', 'Filtro de ar [DEMO]', 'Filtros', 'AgroCampo (DEMO)', 'TC-100 (DEMO)', 'Diâmetro externo', '180', 'mm', '/uploads/72efb2a6-b2a4-58ca-8a30-b493d0a840c5.png'),
    ('CX-DEMO-003', 'Filtro de combustível [DEMO]', 'Filtros', 'TerraMec (DEMO)', 'TM-200 (DEMO)', 'Altura', '115', 'mm', '/uploads/87ebb5b2-3d64-5622-a447-0f2abff0f09f.png'),
    ('CX-DEMO-004', 'Correia de transmissão [DEMO]', 'Transmissão', 'TerraMec (DEMO)', 'TM-200 (DEMO)', 'Comprimento', '1200', 'mm', '/uploads/9fcb6982-47d2-589a-9866-b08b4f463979.png'),
    ('CX-DEMO-005', 'Rolamento de eixo [DEMO]', 'Transmissão', 'TerraMec (DEMO)', 'TM-400 (DEMO)', 'Diâmetro interno', '35', 'mm', '/uploads/20112275-17b4-520b-a0b7-5cd4cda7d974.png'),
    ('CX-DEMO-006', 'Anel de vedação hidráulica [DEMO]', 'Hidráulico', 'AgroCampo (DEMO)', 'TC-100 (DEMO)', 'Diâmetro interno', '40', 'mm', '/uploads/e7159fc7-a97d-5ee1-bcdb-ee2794d74f33.png'),
    ('CX-DEMO-007', 'Mangueira hidráulica [DEMO]', 'Hidráulico', 'AgroCampo (DEMO)', 'AC-300 (DEMO)', 'Comprimento', '1500', 'mm', '/uploads/e5c92bf3-bdf6-5f2c-906e-e28b203dc494.png'),
    ('CX-DEMO-008', 'Disco de corte [DEMO]', 'Plantio', 'TerraMec (DEMO)', 'TM-400 (DEMO)', 'Diâmetro', '450', 'mm', '/uploads/2c645c7c-23b9-5f00-bf1e-19378d6bb070.png'),
    ('CX-DEMO-009', 'Bico de pulverização [DEMO]', 'Pulverização', 'AgroCampo (DEMO)', 'AC-300 (DEMO)', 'Material', 'Polímero', NULL, '/uploads/4f6bf8e5-0cc1-557e-9772-0fd9146f418f.png'),
    ('CX-DEMO-010', 'Câmara de ar agrícola [DEMO]', 'Rodagem', 'TerraMec (DEMO)', 'TC-100 (DEMO)', 'Material', 'Borracha', NULL, '/uploads/1cff58f3-f3bb-5bd1-a322-f8911b7e9ab7.png');

    START TRANSACTION;
    SELECT MIN(id_usuario) INTO v_autor FROM usuario WHERE perfil_acesso='administrador' AND ativo=1;

    -- Reutiliza nomes existentes; IDs sao consultados, nunca presumidos.
    INSERT INTO marca(nome_marca)
        SELECT 'AgroCampo (DEMO)' WHERE NOT EXISTS(SELECT 1 FROM marca WHERE nome_marca='AgroCampo (DEMO)');
    INSERT INTO marca(nome_marca)
        SELECT 'TerraMec (DEMO)' WHERE NOT EXISTS(SELECT 1 FROM marca WHERE nome_marca='TerraMec (DEMO)');
    INSERT INTO categoria(nome_categoria)
        SELECT DISTINCT d.nome_categoria FROM cx_demo_pecas d
        WHERE NOT EXISTS(SELECT 1 FROM categoria c WHERE c.nome_categoria=d.nome_categoria);
    INSERT INTO origem_codigo(nome_origem,tipo_origem)
        SELECT 'Catálogo didático ConexAgro (DEMO)','fabricante'
        WHERE NOT EXISTS(SELECT 1 FROM origem_codigo
                         WHERE nome_origem='Catálogo didático ConexAgro (DEMO)' AND tipo_origem='fabricante');
    SELECT MIN(id_origem) INTO v_origem FROM origem_codigo
        WHERE nome_origem='Catálogo didático ConexAgro (DEMO)' AND tipo_origem='fabricante';

    -- Heranca: o item e seu subtipo maquina compartilham o mesmo ID.
    SET v_fim=FALSE;
    OPEN c_maquinas;
    l_maquinas: LOOP
        FETCH c_maquinas INTO v_nome,v_modelo,v_tipo,v_nome_marca,v_ano;
        IF v_fim THEN LEAVE l_maquinas; END IF;
        SELECT MIN(id_marca) INTO v_marca FROM marca WHERE nome_marca=v_nome_marca;
        SELECT COUNT(*),MIN(m.id_maquina) INTO v_quantidade,v_id
            FROM maquina m JOIN item_catalogo i ON i.id_item=m.id_maquina
            WHERE m.modelo=v_modelo AND i.id_marca=v_marca;
        IF v_quantidade>1 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existe mais de uma maquina para o mesmo modelo DEMO. Revise antes de importar.';
        END IF;
        IF v_id IS NULL THEN
            INSERT INTO item_catalogo(tipo_item,nome,descricao,estado,id_marca,id_usuario_cadastro)
                VALUES('maquina',v_nome,'Máquina fictícia para testes e apresentação acadêmica da ConexAgro. Sem equipamento comercial correspondente.','rascunho',v_marca,v_autor);
            SET v_id=LAST_INSERT_ID();
            INSERT INTO maquina(id_maquina,modelo,tipo_maquina,ano) VALUES(v_id,v_modelo,v_tipo,v_ano);
            UPDATE item_catalogo SET estado='publicado' WHERE id_item=v_id;
        ELSEIF NOT EXISTS(SELECT 1 FROM item_catalogo WHERE id_item=v_id AND nome LIKE '%[DEMO]%') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='O modelo DEMO esta associado a uma maquina sem identificacao DEMO. Nenhum registro foi alterado.';
        END IF;
        UPDATE cx_demo_maquinas SET id_item=v_id WHERE modelo=v_modelo;
    END LOOP;
    CLOSE c_maquinas;

    SET v_fim=FALSE;
    OPEN c_pecas;
    l_pecas: LOOP
        FETCH c_pecas INTO v_codigo,v_nome,v_nome_categoria,v_nome_marca,v_modelo,v_spec,v_valor,v_unidade,v_url;
        IF v_fim THEN LEAVE l_pecas; END IF;
        SELECT MIN(id_marca) INTO v_marca FROM marca WHERE nome_marca=v_nome_marca;
        SELECT MIN(id_categoria) INTO v_categoria FROM categoria WHERE nome_categoria=v_nome_categoria;
        SELECT MIN(id_item) INTO v_maquina FROM cx_demo_maquinas WHERE modelo=v_modelo;
        SELECT COUNT(*),MIN(id_peca) INTO v_quantidade,v_id FROM codigo_peca
            WHERE codigo_normalizado=UPPER(REPLACE(v_codigo,' ','')) AND id_marca=v_marca AND id_origem=v_origem;

        -- O codigo identifica o registro mesmo se o nome DEMO foi editado.
        IF v_id IS NOT NULL AND NOT EXISTS(
            SELECT 1 FROM item_catalogo WHERE id_item=v_id AND tipo_item='peca'
                AND nome LIKE '%[DEMO]%' AND (id_marca=v_marca OR id_marca IS NULL)
        ) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Um codigo DEMO ja esta ligado a outra peca. A carga foi desfeita; revise o codigo em conflito.';
        END IF;

        -- Recupera tambem rascunhos DEMO ainda sem codigo.
        IF v_id IS NULL THEN
            SELECT COUNT(*),MIN(p.id_peca) INTO v_quantidade,v_id
                FROM peca p JOIN item_catalogo i ON i.id_item=p.id_peca
                WHERE i.nome=v_nome AND (i.id_marca=v_marca OR i.id_marca IS NULL);
            IF v_quantidade>1 THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existe mais de um rascunho com o mesmo nome DEMO. A carga foi desfeita.';
            END IF;
        END IF;
        IF v_id IS NULL THEN
            INSERT INTO item_catalogo(tipo_item,nome,descricao,estado,id_marca,id_categoria,id_usuario_cadastro)
                VALUES('peca',v_nome,'Peça fictícia para demonstrar busca por código, filtros e relacionamentos da ConexAgro. Medidas e aplicações simuladas; sem referência comercial real.','rascunho',v_marca,v_categoria,v_autor);
            SET v_id=LAST_INSERT_ID();
            INSERT INTO peca(id_peca) VALUES(v_id);
        ELSE
            -- Preenche apenas lacunas; preserva os campos ja editados pelo usuario.
            UPDATE item_catalogo
                SET id_marca=COALESCE(id_marca,v_marca),
                    id_categoria=COALESCE(id_categoria,v_categoria),
                    descricao=CASE WHEN descricao IS NULL OR CHAR_LENGTH(TRIM(descricao))=0
                        THEN 'Peça fictícia para demonstração acadêmica da ConexAgro. Medidas e aplicações simuladas.' ELSE descricao END
                WHERE id_item=v_id;
        END IF;
        UPDATE cx_demo_pecas SET id_item=v_id WHERE codigo=v_codigo;

        INSERT INTO codigo_peca(id_peca,id_marca,id_origem,codigo,tipo_codigo)
            SELECT v_id,v_marca,v_origem,v_codigo,'original'
            WHERE NOT EXISTS(SELECT 1 FROM codigo_peca WHERE codigo_normalizado=UPPER(REPLACE(v_codigo,' ',''))
                AND id_marca=v_marca AND id_origem=v_origem);

        INSERT INTO especificacao(id_item,nome_especificacao,valor_especificacao,unidade)
            SELECT v_id,v_spec,v_valor,v_unidade
            WHERE NOT EXISTS(SELECT 1 FROM especificacao WHERE id_item=v_id AND nome_especificacao=v_spec);
        INSERT INTO especificacao(id_item,nome_especificacao,valor_especificacao,unidade)
            SELECT v_id,'Finalidade','Demonstração acadêmica; dados fictícios',NULL
            WHERE NOT EXISTS(SELECT 1 FROM especificacao WHERE id_item=v_id AND nome_especificacao='Finalidade');

        INSERT INTO imagem(id_item,url_imagem,descricao_imagem)
            SELECT v_id,v_url,'Ilustração didática [DEMO] — não é uma fotografia comercial'
            WHERE NOT EXISTS(SELECT 1 FROM imagem WHERE id_item=v_id AND url_imagem=v_url);

        -- A imagem correta ja existe: remove apenas a logo provisoria da versao 1.0.
        DELETE FROM imagem WHERE id_item=v_id AND url_imagem='/logo.svg';

        INSERT INTO compatibilidade(id_peca,id_maquina,estado,fonte,ano_inicio,ano_fim,observacoes,data_confirmacao)
            SELECT v_id,v_maquina,'confirmada','Simulação acadêmica ConexAgro [DEMO]; não comprova aplicação em equipamento real.',2018,2026,
                'Relacionamento fictício para testar cadastro, filtros e ficha técnica. Não usar para seleção de peças reais.',CURRENT_TIMESTAMP
            WHERE NOT EXISTS(SELECT 1 FROM compatibilidade WHERE id_peca=v_id AND id_maquina=v_maquina);

        -- Publica somente quando todos os dados exigidos pela regra de negocio existem.
        -- Um registro ja inativado permanece inativo mesmo em nova execucao.
        IF EXISTS(SELECT 1 FROM item_catalogo i WHERE i.id_item=v_id
            AND i.id_marca IS NOT NULL AND i.id_categoria IS NOT NULL
            AND i.descricao IS NOT NULL AND CHAR_LENGTH(TRIM(i.descricao))>0
            AND EXISTS(SELECT 1 FROM codigo_peca c WHERE c.id_peca=v_id)
            AND EXISTS(SELECT 1 FROM imagem im WHERE im.id_item=v_id)
            AND EXISTS(SELECT 1 FROM especificacao e WHERE e.id_item=v_id)
            AND EXISTS(SELECT 1 FROM compatibilidade co WHERE co.id_peca=v_id AND co.estado='confirmada'
                AND co.fonte IS NOT NULL AND CHAR_LENGTH(TRIM(co.fonte))>0 AND co.data_confirmacao IS NOT NULL)
        ) THEN
            UPDATE item_catalogo SET estado='publicado' WHERE id_item=v_id AND estado='rascunho';
        END IF;
    END LOOP;
    CLOSE c_pecas;

    -- Duas pecas tambem se relacionam a uma segunda maquina: exemplo N:N.
    INSERT INTO compatibilidade(id_peca,id_maquina,estado,fonte,ano_inicio,ano_fim,observacoes,data_confirmacao)
        SELECT p.id_item,m.id_item,'confirmada','Simulação acadêmica ConexAgro [DEMO]; segunda aplicação fictícia.',2019,2026,
            'Exemplo didático de relacionamento N:N. Sem validade comercial.',CURRENT_TIMESTAMP
        FROM cx_demo_pecas p JOIN cx_demo_maquinas m ON m.modelo='TM-200 (DEMO)'
        WHERE p.codigo IN ('CX-DEMO-001','CX-DEMO-002')
            AND NOT EXISTS(SELECT 1 FROM compatibilidade co WHERE co.id_peca=p.id_item AND co.id_maquina=m.id_item);


    -- Rascunho independente para o professor experimentar o CRUD completo.
    -- Os dez itens do catalogo ficam publicados; este permanece editavel e excluivel.
    SELECT MIN(id_marca) INTO v_marca FROM marca WHERE nome_marca='AgroCampo (DEMO)';
    SELECT COUNT(*),MIN(id_peca) INTO v_quantidade,v_id FROM codigo_peca
        WHERE codigo_normalizado='CX-POO-CRUD-001' AND id_marca=v_marca AND id_origem=v_origem;
    IF v_id IS NULL THEN
        SELECT COUNT(*),MIN(p.id_peca) INTO v_quantidade,v_id
        FROM peca p JOIN item_catalogo i ON i.id_item=p.id_peca
        WHERE i.nome='Peca para teste CRUD [DEMO]' AND i.id_marca=v_marca;
        IF v_quantidade>1 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Mais de um rascunho CRUD de teste foi encontrado. Revise os registros.';
        END IF;
    END IF;
    IF v_id IS NULL THEN
        INSERT INTO categoria(nome_categoria)
            SELECT 'POO - testes CRUD'
            WHERE NOT EXISTS(SELECT 1 FROM categoria WHERE nome_categoria='POO - testes CRUD');
        SELECT MIN(id_categoria) INTO v_categoria FROM categoria WHERE nome_categoria='POO - testes CRUD';
        INSERT INTO item_catalogo(tipo_item,nome,descricao,estado,id_marca,id_categoria,id_usuario_cadastro)
            VALUES('peca','Peca para teste CRUD [DEMO]',
                   'Rascunho para o professor consultar, editar e excluir pelo sistema Java.',
                   'rascunho',v_marca,v_categoria,v_autor);
        SET v_id=LAST_INSERT_ID();
        INSERT INTO peca(id_peca) VALUES(v_id);
    ELSEIF NOT EXISTS(SELECT 1 FROM item_catalogo WHERE id_item=v_id
                       AND tipo_item='peca' AND nome LIKE '%[DEMO]%') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Codigo de teste CRUD associado a uma peca fora da demonstracao.';
    END IF;
    INSERT INTO codigo_peca(id_peca,id_marca,id_origem,codigo,tipo_codigo)
        SELECT v_id,v_marca,v_origem,'CX-POO-CRUD-001','original'
        WHERE NOT EXISTS(SELECT 1 FROM codigo_peca
             WHERE codigo_normalizado='CX-POO-CRUD-001' AND id_marca=v_marca AND id_origem=v_origem);

    COMMIT;
    SET @cx_poo_instalacao_ok=1;
    DO RELEASE_LOCK('conexagro_carga_poo_20261119');
    SET v_lock=0;

    -- O resultado deve listar as dez pecas. No primeiro carregamento, ficam publicadas.
    SELECT d.codigo,i.nome,i.estado,
        (SELECT COUNT(*) FROM codigo_peca c WHERE c.id_peca=i.id_item) AS codigos,
        (SELECT COUNT(*) FROM especificacao e WHERE e.id_item=i.id_item) AS especificacoes,
        (SELECT COUNT(*) FROM imagem im WHERE im.id_item=i.id_item) AS imagens,
        (SELECT COUNT(*) FROM compatibilidade co WHERE co.id_peca=i.id_item) AS compatibilidades
    FROM cx_demo_pecas d JOIN item_catalogo i ON i.id_item=d.id_item ORDER BY d.codigo;

    DROP TEMPORARY TABLE cx_demo_pecas;
    DROP TEMPORARY TABLE cx_demo_maquinas;
END$$
DELIMITER ;

CALL cx_instalar_dados_poo_20261119();
DROP PROCEDURE cx_instalar_dados_poo_20261119;

-- PARTE 3 - CONFERENCIA DA INSTALACAO (SOMENTE CONSULTAS)
SELECT CASE WHEN @cx_poo_instalacao_ok=1
    THEN 'ConexAgro POO: instalacao concluida. Execute Principal e configure o primeiro administrador.'
    ELSE 'Instalacao nao concluida. Confira a primeira mensagem de erro antes de iniciar o Java.'
    END AS orientacao;

SELECT 'usuario' AS tabela,COUNT(*) AS quantidade FROM usuario
UNION ALL SELECT 'marca',COUNT(*) FROM marca
UNION ALL SELECT 'categoria',COUNT(*) FROM categoria
UNION ALL SELECT 'item_catalogo',COUNT(*) FROM item_catalogo
UNION ALL SELECT 'peca',COUNT(*) FROM peca
UNION ALL SELECT 'maquina',COUNT(*) FROM maquina
UNION ALL SELECT 'codigo_peca',COUNT(*) FROM codigo_peca
UNION ALL SELECT 'especificacao',COUNT(*) FROM especificacao
UNION ALL SELECT 'imagem',COUNT(*) FROM imagem
UNION ALL SELECT 'compatibilidade',COUNT(*) FROM compatibilidade;

-- As duas entidades relacionadas usadas para avaliar o CRUD do trabalho:
SELECT i.id_item AS id_peca,i.nome,i.estado,c.id_categoria,c.nome_categoria,cp.codigo
FROM peca p
JOIN item_catalogo i ON i.id_item=p.id_peca
JOIN categoria c ON c.id_categoria=i.id_categoria
LEFT JOIN codigo_peca cp ON cp.id_peca=p.id_peca
ORDER BY i.id_item,cp.id_codigo;

-- Heranca no banco: o item base e a peca compartilham o mesmo identificador.
SELECT i.id_item,p.id_peca,i.tipo_item,i.nome
FROM item_catalogo i JOIN peca p ON p.id_peca=i.id_item
ORDER BY i.id_item;

-- Este resultado deve ser zero antes do primeiro acesso, em uma instalacao nova.
SELECT COUNT(*) AS administradores_ativos
FROM usuario WHERE perfil_acesso='administrador' AND ativo=1;

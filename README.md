# ConexAgro — Projeto de Programação Orientada a Objetos

Plataforma acadêmica para organizar e consultar informações sobre peças e máquinas agrícolas.

## Integrante

- Gabriel de Jesus Coelho Donha.
- Acrescentar os demais integrantes e suas contribuições, caso o trabalho seja em grupo.

## Tecnologias

- Java 17 ou superior.
- MySQL 8.0.16 ou superior.
- MySQL Connector/J.
- Interface web em HTML, CSS e JavaScript.
- Servidor HTTP local disponibilizado pelo Java.

## Funcionalidades

- Cadastro, consulta, alteração e exclusão de categorias.
- Cadastro, consulta, alteração e exclusão de peças em rascunho.
- Publicação de peças com as informações exigidas pelas regras do projeto.
- Pesquisa de peças publicadas, referências de código e compatibilidades.
- Cadastro e gerenciamento de usuários, com permissões por perfil.
- Imagens, especificações e dados de apoio para o catálogo.

Os registros de demonstração são fictícios e não comprovam aplicação em equipamentos reais.

## Arquitetura e conceitos de POO

- **Model:** entidades e regras de validação.
- **View:** interface na pasta `web`.
- **Controller:** coordenação das operações e verificação das permissões.
- **DAO:** acesso ao banco e persistência dos dados.
- **ConnectionFactory:** abertura das conexões JDBC com o MySQL.
- **Principal:** inicialização do servidor da aplicação.

A classe abstrata `ItemCatalogo` reúne informações comuns de `Peca` e `Maquina`. As subclasses reaproveitam atributos e implementam comportamentos próprios, como a apresentação de seus resumos. Os atributos privados e os métodos de acesso permitem controlar alterações.

No banco, `item_catalogo` compartilha o identificador com seu subtipo `peca` ou `maquina`. A referência à categoria fica em `item_catalogo`.

## Arquivos necessários

- `src/`: código Java.
- `web/`: interface e identidade visual.
- `dados/imagens/`: ilustrações usadas pelos cadastros de demonstração.
- `ConexAgro_POO_Banco_Completo.sql`: estrutura e dados para instalação.
- `conexao.properties.example`: modelo de configuração sem credenciais reais.

O arquivo local `conexao.properties` não deve ser publicado.

## Como executar no IntelliJ

1. Instale o JDK e o MySQL nas versões indicadas.
2. Abra a pasta do projeto no IntelliJ, configure o JDK e marque `src` como pasta de código-fonte se necessário.
3. Adicione o arquivo JAR do MySQL Connector/J às dependências do projeto.
4. Baixe `ConexAgro_POO_Banco_Completo.sql` pelos links da seção **Banco de dados**. No MySQL Workbench, abra e execute o arquivo inteiro. Ele instala o banco `conexagro`.
5. Na raiz do projeto, copie `conexao.properties.example` para `conexao.properties` **somente se esse arquivo ainda não existir**.
6. Preencha usuário, senha, endereço e porta do MySQL com os dados do computador onde o sistema será executado.
7. Configure o diretório de trabalho da execução como a raiz do projeto, onde estão `src`, `web`, `dados` e `conexao.properties`.
8. Execute a classe `Principal`.
9. Abra no navegador o endereço local informado no console.
10. Se ainda não houver administrador, utilize o endereço temporário de configuração mostrado no console para criar a primeira conta administrativa. Depois entre no sistema.

O SQL não cria um usuário administrativo com senha fixa. As imagens devem acompanhar o projeto na pasta `dados/imagens`; o banco guarda seus caminhos.

## Roteiro para testar os dois CRUDs relacionados

1. Entre como administrador.
2. Em **Cadastros de apoio → Categorias**, crie uma categoria de teste, consulte-a e altere seu nome.
3. Em **Peças**, crie um rascunho associado à categoria, consulte-o e altere seus dados.
4. Tente excluir a categoria enquanto a peça estiver vinculada: o relacionamento deve impedir a exclusão.
5. Exclua o rascunho da peça e depois a categoria.
6. Confira as listas para verificar as alterações e exclusões.

A instalação também fornece uma categoria `POO - testes CRUD` e uma peça em rascunho com código `CX-POO-CRUD-001`. Peças publicadas possuem regras de proteção diferentes; utilize rascunhos na demonstração da exclusão.

## Capturas da aplicação

Antes da entrega, acrescentar uma captura do catálogo e outra do cadastro de peças ou categorias, utilizando imagens da aplicação em funcionamento.

## Vídeo

[Assistir ao vídeo de apresentação no OneDrive](https://1drv.ms/v/c/5c4d97af97bbeda3/IQDPNvRTEDYDQr_0GY1Zy1i7AZmVm56UoCKrL6znrMrJCKE?e=22IL3d)

## Banco de dados

[Baixar o script SQL do banco de dados no OneDrive](https://1drv.ms/u/c/5c4d97af97bbeda3/IQDnxKul4GvCSYRzyiHY_HVHAQOU4_I3essWXrwXnWGhdOM?e=hWntF9)

[Consultar a cópia do SQL versão 1.1 usada nos testes](https://github.com/Donha27/ConexAgro-POO/blob/52abda1316032a395c699cffcba56cf548a7bfee/tests/fixtures/ConexAgro_POO_Banco_Completo.sql)

## Validação automatizada

[Consultar a execução aprovada no GitHub Actions](https://github.com/Donha27/ConexAgro-POO/actions/runs/44652229212).

Ambiente: Java 17, MySQL 8.0 e MySQL Connector/J 9.3.0.

- **120 verificações de integração aprovadas**, iniciando a `Principal`, usando a API HTTP real e consultando o banco.
- **45 verificações HTTP em memória aprovadas**, executando `Web.TesteWeb`.
- Compilação dos 52 arquivos Java e verificação de sintaxe de `web/app.js` aprovadas.
- Instalação limpa e reexecução do SQL, imagens, autenticação, permissões, CRUDs de categorias e peças, cadastros de apoio, publicação, rollback e persistência após reiniciar verificados.

O código da aplicação testado corresponde ao commit [`58b1e85195d0`](https://github.com/Donha27/ConexAgro-POO/commit/58b1e85195d03ed3cb65f7b567025fb59cbbbea7). Os testes estão na branch [`codex/validacao-crud-20261119`](https://github.com/Donha27/ConexAgro-POO/tree/codex/validacao-crud-20261119).

O SQL executado foi a **versão 1.1**, disponível no link da cópia validada acima. O download automático pelo OneDrive não ficou disponível, portanto o arquivo externo não pôde ser comparado com essa cópia. A interface visual deve ser conferida manualmente seguindo o roteiro de uso.

## Transparência sobre o uso de IA

A inteligência artificial apoiou a estruturação, a geração e a revisão de partes do código, da interface e dos scripts SQL. Também houve apoio no roteiro e na produção de materiais audiovisuais da apresentação.

Gabriel definiu o escopo, configurou o ambiente e realizou testes locais. Registrar as contribuições reais dos demais integrantes, se houver.



# Cantina Escolar

Projeto desenvolvido pelo Time 7 durante o Hackathon.

## Equipe

- Vitor Metz
- Augusto Martins
- Pedro Holler

## Objetivo

Desenvolver um sistema para facilitar e automatizar processos de uma cantina escolar.


## Funcionalidades

- Painel principal para acessar os módulos
- Gerenciamento de cardápios
- Controle de produtos disponíveis e esgotados
- Registro de pedidos antecipados
- Gerenciamento e retirada de pedidos
- Cadastro de clientes
- Edição de clientes
- Atualização das informações dos clientes

## Telas disponíveis

- `TelaPrincipal.java`: painel de entrada para clientes, cardápios e pedidos;
- `TelaClientes.java`: cadastro, edição e exclusão de clientes;
- `TelaCardapios.java`: criação e edição de cardápios, leitura do JSON e
  disponibilidade individual de cada item;
- `TelaPedidos.java`: cadastro e hub dos pedidos ainda não retirados.

A aplicação impede que um cliente tenha dois pedidos ativos ao mesmo tempo.
Essa regra também está representada no `database/schema.sql` por uma chave
única gerada para os pedidos com `retirado = false`.

Antes de registrar um pedido, a aplicação verifica o saldo do cliente. O nome
do cliente é único e é usado como identificação visível; o `idcliente` continua
existindo internamente para os relacionamentos do banco. Quando o pedido é
confirmado, seu total é descontado do saldo.

### Navegação pelo teclado

- `Tab` e `Shift+Tab`: avançar e voltar entre campos;
- `Enter`: ativar o botão principal da tela;
- `Esc`: fechar uma tela secundária;
- `Ctrl+1`, `Ctrl+2` e `Ctrl+3`: abrir Clientes, Cardápios e Pedidos;
- `Ctrl+S`: salvar um cardápio;
- `Ctrl+N`: iniciar um cardápio novo;
- `Ctrl+Enter`: registrar um pedido;
- `Alt` + letra sublinhada: acionar os demais botões.

## Primeira entrega: tela de clientes

A primeira tela funcional foi criada com Java Swing, sem bibliotecas externas.
Ela permite:

- adicionar clientes;
- selecionar um cliente na tabela;
- editar nome do cliente, responsável, saldo, limite, e-mail e alergias;
- remover um cliente com confirmação;
- validar os campos obrigatórios, o e-mail e os valores monetários.

Os dados da tela seguem a futura tabela `Cliente`:

```text
idcliente
nomecliente
nomeresponsavel
saldo
limitesaldo
emailresponsavel
alergias
```

> Nesta primeira versão, os dados ficam somente na memória e são apagados ao
> fechar o programa. A camada JDBC já está estruturada, mas a tela ainda não foi
> ligada ao banco para continuar utilizável durante a configuração do MySQL.

## Banco de dados e DAO

O projeto está preparado para usar **MySQL 8**. O arquivo
`database/schema.sql` cria as tabelas `cliente`, `cardapio` e `pedido`.

Em `cliente`, os campos monetários foram definidos como:

```sql
saldo DECIMAL(10,2) NOT NULL DEFAULT 0.00,
limitesaldo DECIMAL(10,2) NOT NULL DEFAULT 0.00
```

A estrutura Java da conexão está dividida assim:

```text
src/br/com/time7/cantina/
├── dao/
│   ├── ClienteDAO.java
│   └── ClienteJdbcDAO.java
├── infra/
│   ├── BancoConfig.java
│   ├── BancoDados.java
│   ├── CantinaDataSource.java
│   └── TestarConexao.java
└── model/
    └── Cliente.java
```

As credenciais não ficam salvas no código. Antes de conectar, configure no
PowerShell:

```powershell
$env:CANTINA_DB_URL="jdbc:mysql://localhost:3306/cantina_escolar?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo"
$env:CANTINA_DB_USER="root"
$env:CANTINA_DB_PASSWORD="SUA_SENHA"
```

O driver MySQL Connector/J está declarado no `pom.xml`. É necessário ter o
Maven instalado ou configurar a dependência pelo VS Code antes de executar o
teste de conexão.

O `schema.sql` prepara uma instalação nova. Como usa `CREATE TABLE IF NOT EXISTS`,
executá-lo novamente não modifica tabelas antigas; mudanças futuras na estrutura
deverão ser aplicadas por arquivos de migração próprios.

## Verificações automáticas

Os testes simples da pasta `tests` não dependem de bibliotecas externas. Eles
conferem valores compatíveis com `DECIMAL(10,2)`, leitura das configurações e a
importação completa do JSON do cardápio. A importação rejeita o documento inteiro
quando qualquer item é inválido, sem apagar os itens que já estavam na tela.

## Como executar no VS Code

1. Instale um JDK 8 ou mais recente e a extensão **Extension Pack for Java**.
2. Abra a pasta deste projeto no VS Code.
3. Abra o arquivo `src/TelaPrincipal.java`.
4. Clique em **Run**, exibido acima do método `main`.

Também é possível compilar pelo terminal:

```powershell
New-Item -ItemType Directory -Force out
$fontes = Get-ChildItem src -Recurse -Filter *.java
javac -d out $fontes.FullName
java -cp out TelaPrincipal
```

# Cantina Escolar

Projeto desenvolvido pelo Time 7 durante o Hackathon.

## Equipe

- Vitor Metz
- Augusto Martins
- Pedro Holler

## Objetivo

Desenvolver um sistema para facilitar e automatizar processos de uma cantina escolar.


## Funcionalidades

- Gerenciamento de cardápios
- Controle de produtos disponíveis e esgotados
- Registro de pedidos antecipados
- Gerenciamento e retirada de pedidos
- Cadastro de clientes
- Edição de clientes
- Atualização das informações dos clientes

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

## Como executar no VS Code

1. Instale um JDK 8 ou mais recente e a extensão **Extension Pack for Java**.
2. Abra a pasta deste projeto no VS Code.
3. Abra o arquivo `src/TelaClientes.java`.
4. Clique em **Run**, exibido acima do método `main`.

Também é possível compilar pelo terminal:

```powershell
$fontes = Get-ChildItem src -Recurse -Filter *.java
javac -d out $fontes.FullName
java -cp out TelaClientes
```

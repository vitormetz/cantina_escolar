# Cantina Escolar

Sistema em Java Swing e MySQL desenvolvido pelo Time 7 no Hackathon.

## Equipe

- Vitor Metz
- Augusto Martins
- Pedro Holler

## Funcionalidades

- cadastro, edição, consulta e exclusão de clientes;
- cardápios separados por dia da semana;
- itens disponíveis ou esgotados;
- registro de compra comum ou pedido antecipado;
- pagamento e retirada controlados separadamente;
- histórico de pedidos retirados;
- persistência no MySQL;
- aviso por e-mail ao responsável depois que o pedido é gravado.

## Regras atuais

- Pedidos comuns são registrados como pagos e descontam o saldo imediatamente.
- Pedidos antecipados podem ser registrados como não pagos. Nesse caso, o saldo
  só é descontado ao confirmar o pagamento.
- Um pedido só pode ser retirado depois de pago.
- Um cliente não pode ter dois pedidos ainda não retirados.
- A compra e o desconto do saldo são confirmados na mesma transação.
- Apenas itens disponíveis de cardápios disponíveis para o dia atual aparecem
  em Pedidos. A disponibilidade é validada novamente antes do commit.
- Quando existem vários cardápios disponíveis para o mesmo dia, os itens de
  todos eles são combinados.
- O JSON do cardápio é apenas uma visualização técnica e não pode ser editado.
- O campo `limitesaldo` continua armazenado, mas ainda não altera compras porque
  seu significado precisa ser definido pelo responsável pelo projeto.
- Por enquanto, pedido antecipado significa pedido feito antes da retirada no
  mesmo dia. Data e horário planejados ainda dependem de decisão de negócio.

## Banco de dados

O projeto usa MySQL 8. Para uma instalação nova:

```powershell
cmd /c "mysql -u root -p < database\schema.sql"
```

Se o banco já foi criado por uma versão anterior, execute uma vez:

```powershell
cmd /c "mysql -u root -p < database\migrations\V2__pagamento_email_itens.sql"
```

A migração adiciona pagamento, resultado do e-mail, dia usado pelo pedido e a
tabela normalizada de itens. Ela não apaga os dados existentes. Pedidos antigos
são marcados como pagos, porque a versão anterior debitava o saldo no cadastro,
e seus itens válidos são copiados do JSON para a nova tabela. Se já existirem
dois pedidos não retirados para o mesmo cliente, conclua um deles antes da
migração; a restrição de integridade interrompe a execução sem apagar pedidos.

Configure a conexão na sessão atual do PowerShell:

```powershell
$env:CANTINA_DB_URL="jdbc:mysql://192.168.20.5:3306/cantina_escolar?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo"
$env:CANTINA_DB_USER="root"
$env:CANTINA_DB_PASSWORD="SUA_SENHA"
```

Nunca salve senha no código, README ou GitHub.

## E-mail

O envio usa SMTP por meio do JavaMail, sem gravar credenciais no projeto:

```powershell
$env:CANTINA_EMAIL_SMTP_HOST="smtp.seuprovedor.com"
$env:CANTINA_EMAIL_SMTP_PORT="587"
$env:CANTINA_EMAIL_SMTP_USER="usuario@exemplo.com"
$env:CANTINA_EMAIL_SMTP_PASSWORD="SUA_SENHA_OU_SENHA_DE_APP"
$env:CANTINA_EMAIL_FROM="Cantina Escolar <usuario@exemplo.com>"
$env:CANTINA_EMAIL_SMTP_TLS="true"
```

O pedido é gravado antes do envio. Se o e-mail falhar, o pedido e o débito não
são repetidos: a falha fica registrada e a tela permite tentar o envio novamente.
O aviso contém cliente, itens, quantidades, valores, total, saldo restante e
data/hora. A senha SMTP nunca deve ser salva em arquivo ou commit.

## Compilar e executar

Pré-requisitos: JDK 8 ou superior, Maven e MySQL 8.

No Windows, a forma mais simples é executar na raiz do projeto:

```powershell
powershell -ExecutionPolicy Bypass -File .\iniciar.ps1
```

O script solicita a senha sem exibi-la, compila, conclui a migração pendente e
abre o sistema. A senha fica somente no processo atual.

Também é possível executar manualmente:

```powershell
mvn clean compile
mvn exec:java "-Dexec.mainClass=TelaPrincipal"
```

A aplicação testa a conexão ao iniciar e mostra uma mensagem clara se o banco
não estiver disponível. Ela também detecta e conclui automaticamente a migração
V2, inclusive quando uma tentativa anterior adicionou somente parte das colunas.
Nenhuma tabela ou registro existente é apagado nesse processo.

## Testes

Compile os testes:

```powershell
mvn test-compile
```

Execute os testes locais, que não usam banco nem servidor de e-mail real:

```powershell
$testes = @("ValoresMonetariosTest", "CardapioJsonTest", "PedidoRegrasTest", "BancoConfigTest", "ServicoEmailTest")
foreach ($teste in $testes) {
    mvn -q exec:java "-Dexec.mainClass=$teste" "-Dexec.classpathScope=test"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
```

O teste completo de persistência usa o MySQL configurado acima. Use somente um
banco de teste já criado e migrado:

```powershell
$env:CANTINA_TEST_DB="1"
mvn -q exec:java "-Dexec.mainClass=FluxoPersistenciaTest" "-Dexec.classpathScope=test"
```

Sem `CANTINA_TEST_DB=1`, esse teste é ignorado para não alterar dados por
acidente.

## Estrutura principal

```text
database/
├── schema.sql
└── migrations/
    └── V2__pagamento_email_itens.sql
src/
├── BancoAplicacao.java
├── CardapioDAO.java
├── CardapioJdbcDAO.java
├── EmailCompra.java
├── MigradorBanco.java
├── PedidoDAO.java
├── PedidoJdbcDAO.java
├── ServicoEmail.java
├── ServicoEmailSmtp.java
├── TelaPrincipal.java
├── TelaClientes.java
├── TelaCardapios.java
└── TelaPedidos.java
tests/
├── BancoConfigTest.java
├── CardapioJsonTest.java
├── PedidoRegrasTest.java
├── ServicoEmailTest.java
├── ValoresMonetariosTest.java
└── FluxoPersistenciaTest.java
```

## Extensões fora do escopo atual

- alerta visível de alergias durante a compra;
- cancelamento com devolução automática do saldo;
- estoque por quantidade;
- data e horário planejados para pedidos de outro dia.

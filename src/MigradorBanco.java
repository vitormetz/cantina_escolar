import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Conclui com segurança a migração V2, inclusive quando uma tentativa anterior
 * adicionou somente parte das colunas. Não apaga tabelas nem registros.
 */
public final class MigradorBanco {
    private static final String VERSAO = "V2_FLUXO_PERSISTENTE";

    private MigradorBanco() { }

    public static void aplicar(DataSource dataSource) throws SQLException {
        try (Connection conexao = dataSource.getConnection()) {
            executar(conexao, "CREATE TABLE IF NOT EXISTS cantina_schema_version ("
                    + "versao VARCHAR(50) NOT NULL PRIMARY KEY, "
                    + "aplicada_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            if (versaoAplicada(conexao)) return;

            adicionarColuna(conexao, "pedido", "dia_cardapio",
                    "TINYINT UNSIGNED NULL AFTER idcliente");
            adicionarColuna(conexao, "pedido", "pago",
                    "BOOLEAN NOT NULL DEFAULT FALSE AFTER dia_cardapio");
            adicionarColuna(conexao, "pedido", "email_enviado",
                    "BOOLEAN NOT NULL DEFAULT FALSE AFTER retirado");
            adicionarColuna(conexao, "pedido", "email_erro",
                    "VARCHAR(500) NULL AFTER email_enviado");
            adicionarColuna(conexao, "pedido", "saldo_apos",
                    "DECIMAL(10,2) NULL AFTER email_erro");

            executar(conexao, "UPDATE pedido SET dia_cardapio = WEEKDAY(datapedido) + 1 "
                    + "WHERE dia_cardapio IS NULL");
            executar(conexao, "UPDATE pedido SET pago = TRUE");
            executar(conexao, "UPDATE pedido p JOIN cliente c ON c.idcliente = p.idcliente "
                    + "SET p.saldo_apos = c.saldo WHERE p.saldo_apos IS NULL");

            executar(conexao, "ALTER TABLE pedido "
                    + "MODIFY COLUMN dia_cardapio TINYINT UNSIGNED NOT NULL, "
                    + "MODIFY COLUMN saldo_apos DECIMAL(10,2) NOT NULL");
            adicionarRestricao(conexao, "pedido", "ck_pedido_dia_cardapio",
                    "CHECK (dia_cardapio BETWEEN 1 AND 7)");
            adicionarRestricao(conexao, "pedido", "ck_pedido_saldo_apos",
                    "CHECK (saldo_apos >= 0)");
            adicionarIndiceNomeCliente(conexao);

            executar(conexao, "CREATE TABLE IF NOT EXISTS pedido_ativo ("
                    + "idcliente INT NOT NULL PRIMARY KEY, "
                    + "CONSTRAINT fk_pedido_ativo_cliente FOREIGN KEY (idcliente) "
                    + "REFERENCES cliente (idcliente) ON UPDATE CASCADE ON DELETE CASCADE)");
            verificarPedidosAtivosDuplicados(conexao);
            executar(conexao, "INSERT IGNORE INTO pedido_ativo (idcliente) "
                    + "SELECT idcliente FROM pedido WHERE retirado = FALSE");

            executar(conexao, "CREATE TABLE IF NOT EXISTS pedido_item ("
                    + "id_pedido_item INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                    + "idpedido INT NOT NULL, nome VARCHAR(150) NOT NULL, "
                    + "preco_unitario DECIMAL(10,2) NOT NULL, quantidade INT UNSIGNED NOT NULL, "
                    + "KEY idx_pedido_item_pedido (idpedido), "
                    + "CONSTRAINT ck_pedido_item_preco CHECK (preco_unitario > 0), "
                    + "CONSTRAINT ck_pedido_item_quantidade CHECK (quantidade BETWEEN 1 AND 99), "
                    + "CONSTRAINT fk_pedido_item_pedido FOREIGN KEY (idpedido) "
                    + "REFERENCES pedido (idpedido) ON UPDATE CASCADE ON DELETE CASCADE)");
            migrarItensAntigos(conexao);
            criarGatilhos(conexao);

            try (PreparedStatement comando = conexao.prepareStatement(
                    "INSERT INTO cantina_schema_version (versao) VALUES (?)")) {
                comando.setString(1, VERSAO);
                comando.executeUpdate();
            }
        }
    }

    private static boolean versaoAplicada(Connection conexao) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(
                "SELECT 1 FROM cantina_schema_version WHERE versao = ?")) {
            comando.setString(1, VERSAO);
            try (ResultSet resultado = comando.executeQuery()) {
                return resultado.next();
            }
        }
    }

    private static void adicionarColuna(Connection conexao, String tabela,
            String coluna, String definicao) throws SQLException {
        if (!existe(conexao, "SELECT 1 FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                tabela, coluna)) {
            executar(conexao, "ALTER TABLE " + tabela + " ADD COLUMN " + coluna + " " + definicao);
        }
    }

    private static void adicionarRestricao(Connection conexao, String tabela,
            String nome, String definicao) throws SQLException {
        if (!existe(conexao, "SELECT 1 FROM information_schema.TABLE_CONSTRAINTS "
                + "WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = ? "
                + "AND CONSTRAINT_NAME = ?", tabela, nome)) {
            executar(conexao, "ALTER TABLE " + tabela + " ADD CONSTRAINT " + nome + " " + definicao);
        }
    }

    private static void adicionarIndiceNomeCliente(Connection conexao) throws SQLException {
        if (existe(conexao, "SELECT 1 FROM information_schema.STATISTICS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                "cliente", "uk_cliente_nome")) return;
        if (existe(conexao, "SELECT 1 FROM cliente GROUP BY nomecliente HAVING COUNT(*) > 1 LIMIT 1")) {
            throw new SQLException("Há clientes com nomes duplicados. Corrija-os antes de continuar.");
        }
        executar(conexao, "ALTER TABLE cliente ADD UNIQUE KEY uk_cliente_nome (nomecliente)");
    }

    private static void verificarPedidosAtivosDuplicados(Connection conexao) throws SQLException {
        if (existe(conexao, "SELECT 1 FROM pedido WHERE retirado = FALSE "
                + "GROUP BY idcliente HAVING COUNT(*) > 1 LIMIT 1")) {
            throw new SQLException("Há dois pedidos ativos para o mesmo cliente. "
                    + "Conclua um deles antes de continuar.");
        }
    }

    private static void migrarItensAntigos(Connection conexao) throws SQLException {
        executar(conexao, "INSERT INTO pedido_item (idpedido, nome, preco_unitario, quantidade) "
                + "SELECT p.idpedido, antigo.nome, antigo.preco, antigo.quantidade FROM pedido p "
                + "JOIN JSON_TABLE(IF(JSON_VALID(p.itempedido), p.itempedido, "
                + "JSON_OBJECT('itens', JSON_ARRAY())), '$.itens[*]' COLUMNS ("
                + "nome VARCHAR(150) PATH '$.nome', preco DECIMAL(10,2) PATH '$.preco', "
                + "quantidade INT PATH '$.quantidade')) AS antigo "
                + "WHERE antigo.nome IS NOT NULL AND antigo.preco > 0 "
                + "AND antigo.quantidade BETWEEN 1 AND 99 "
                + "AND NOT EXISTS (SELECT 1 FROM pedido_item pi WHERE pi.idpedido = p.idpedido)");
    }

    private static void criarGatilhos(Connection conexao) throws SQLException {
        criarGatilho(conexao, "trg_pedido_ativo_inserir",
                "CREATE TRIGGER trg_pedido_ativo_inserir AFTER INSERT ON pedido FOR EACH ROW "
                + "BEGIN IF NEW.retirado = FALSE THEN INSERT INTO pedido_ativo (idcliente) "
                + "VALUES (NEW.idcliente); END IF; END");
        criarGatilho(conexao, "trg_pedido_ativo_atualizar",
                "CREATE TRIGGER trg_pedido_ativo_atualizar AFTER UPDATE ON pedido FOR EACH ROW "
                + "BEGIN IF OLD.retirado = FALSE AND (NEW.retirado = TRUE OR "
                + "OLD.idcliente <> NEW.idcliente) THEN DELETE FROM pedido_ativo WHERE "
                + "idcliente = OLD.idcliente; END IF; IF NEW.retirado = FALSE AND "
                + "(OLD.retirado = TRUE OR OLD.idcliente <> NEW.idcliente) THEN "
                + "INSERT INTO pedido_ativo (idcliente) VALUES (NEW.idcliente); END IF; END");
        criarGatilho(conexao, "trg_pedido_ativo_excluir",
                "CREATE TRIGGER trg_pedido_ativo_excluir AFTER DELETE ON pedido FOR EACH ROW "
                + "BEGIN IF OLD.retirado = FALSE THEN DELETE FROM pedido_ativo "
                + "WHERE idcliente = OLD.idcliente; END IF; END");
    }

    private static void criarGatilho(Connection conexao, String nome, String sql)
            throws SQLException {
        if (!existe(conexao, "SELECT 1 FROM information_schema.TRIGGERS "
                + "WHERE TRIGGER_SCHEMA = DATABASE() AND TRIGGER_NAME = ?", nome)) {
            executar(conexao, sql);
        }
    }

    private static boolean existe(Connection conexao, String sql, String... parametros)
            throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) comando.setString(i + 1, parametros[i]);
            try (ResultSet resultado = comando.executeQuery()) {
                return resultado.next();
            }
        }
    }

    private static void executar(Connection conexao, String sql) throws SQLException {
        try (Statement comando = conexao.createStatement()) {
            comando.execute(sql);
        }
    }
}

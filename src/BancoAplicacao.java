import br.com.time7.cantina.dao.ClienteDAO;
import br.com.time7.cantina.dao.ClienteJdbcDAO;
import br.com.time7.cantina.infra.BancoDados;
import br.com.time7.cantina.model.Cliente;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ponto único de persistência usado pelas telas Swing.
 * Nenhuma operação informa sucesso antes do commit no MySQL.
 */
public final class BancoAplicacao {
    private static final DataSource DATA_SOURCE = BancoDados.getDataSource();
    private static final ClienteDAO CLIENTES = new ClienteJdbcDAO(DATA_SOURCE);
    private static final CardapioDAO CARDAPIOS = new CardapioJdbcDAO(DATA_SOURCE);
    private static final PedidoDAO PEDIDOS = new PedidoJdbcDAO(DATA_SOURCE);
    private static ServicoEmail servicoEmail = new ServicoEmailSmtp();

    private BancoAplicacao() { }

    public static void testarConexao() {
        try {
            MigradorBanco.aplicar(DATA_SOURCE);
        } catch (SQLException erro) {
            throw banco("Não foi possível preparar a estrutura do banco", erro);
        }
        try (Connection conexao = DATA_SOURCE.getConnection()) {
            if (!conexao.isValid(3)) throw new SQLException("Conexão inválida.");
            try (PreparedStatement comando = conexao.prepareStatement(
                    "SELECT p.pago, p.retirado, p.email_enviado, p.dia_cardapio, "
                    + "p.saldo_apos, pi.preco_unitario FROM pedido p "
                    + "LEFT JOIN pedido_item pi ON pi.idpedido = p.idpedido WHERE 1 = 0")) {
                comando.executeQuery();
            } catch (SQLException estruturaIncompleta) {
                throw banco("MySQL conectado, mas o schema/migração está incompleto",
                        estruturaIncompleta);
            }
        } catch (SQLException erro) {
            throw banco("Não foi possível conectar ao MySQL", erro);
        }
    }

    public static Cliente inserirCliente(Cliente cliente) {
        try { return CLIENTES.inserir(cliente); }
        catch (SQLException erro) { throw banco("Não foi possível cadastrar o cliente", erro); }
    }

    public static List<Cliente> listarClientes() {
        try { return CLIENTES.listarTodos(); }
        catch (SQLException erro) { throw banco("Não foi possível carregar os clientes", erro); }
    }

    public static Cliente buscarCliente(int idCliente) {
        try { return CLIENTES.buscarPorId(idCliente); }
        catch (SQLException erro) { throw banco("Não foi possível consultar o cliente", erro); }
    }

    public static void atualizarCliente(Cliente cliente) {
        try {
            if (!CLIENTES.atualizar(cliente)) throw new IllegalArgumentException("Cliente não encontrado.");
        } catch (SQLException erro) {
            throw banco("Não foi possível atualizar o cliente", erro);
        }
    }

    /** Evita que uma edição aberta antes de uma compra reponha o saldo debitado. */
    public static void atualizarCliente(Cliente cliente, BigDecimal saldoEsperado) {
        String sql = "UPDATE cliente SET nomecliente = ?, nomeresponsavel = ?, saldo = ?, "
                + "limitesaldo = ?, emailresponsavel = ?, alergias = ? "
                + "WHERE idcliente = ? AND saldo = ?";
        try (Connection conexao = DATA_SOURCE.getConnection();
                PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, cliente.getNomeCliente());
            comando.setString(2, cliente.getNomeResponsavel());
            comando.setBigDecimal(3, cliente.getSaldo());
            comando.setBigDecimal(4, cliente.getLimiteSaldo());
            comando.setString(5, cliente.getEmailResponsavel());
            comando.setString(6, cliente.getAlergias());
            comando.setInt(7, cliente.getIdCliente());
            comando.setBigDecimal(8, saldoEsperado);
            if (comando.executeUpdate() != 1) {
                Cliente atual = buscarCliente(cliente.getIdCliente());
                if (atual == null) throw new IllegalArgumentException("Cliente não encontrado.");
                throw new IllegalArgumentException(
                        "O saldo mudou desde que a tela foi aberta. Atualize e tente novamente.");
            }
        } catch (SQLException erro) {
            throw banco("Não foi possível atualizar o cliente", erro);
        }
    }

    public static void excluirCliente(int idCliente) {
        try {
            if (!CLIENTES.excluir(idCliente)) throw new IllegalArgumentException("Cliente não encontrado.");
        } catch (SQLException erro) {
            throw banco("Não foi possível excluir o cliente. Verifique se ele possui pedidos", erro);
        }
    }

    public static boolean temPedidosDoCliente(int idCliente) {
        try {
            return PEDIDOS.existeParaCliente(idCliente);
        } catch (SQLException erro) {
            throw banco("Não foi possível verificar os pedidos do cliente", erro);
        }
    }

    public static List<TelaCardapios.Cardapio> listarCardapios() {
        try {
            return CARDAPIOS.listarTodos();
        } catch (SQLException erro) {
            throw banco("Não foi possível carregar os cardápios", erro);
        }
    }

    public static TelaCardapios.Cardapio salvarCardapio(
            int idCardapio, int diaSemana, boolean disponivel,
            List<TelaCardapios.ItemCardapio> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("Adicione pelo menos um item ao cardápio.");
        }
        try {
            TelaCardapios.Cardapio salvo = CARDAPIOS.salvar(
                    idCardapio, diaSemana, disponivel, itens);
            if (salvo == null) throw new IllegalArgumentException("Cardápio não encontrado.");
            return salvo;
        } catch (SQLException erro) {
            throw banco("Não foi possível salvar o cardápio", erro);
        }
    }

    public static void excluirCardapio(int idCardapio) {
        try {
            if (!CARDAPIOS.excluir(idCardapio)) {
                throw new IllegalArgumentException("Cardápio não encontrado.");
            }
        } catch (SQLException erro) {
            throw banco("Não foi possível excluir o cardápio", erro);
        }
    }

    /** Mais de um cardápio no mesmo dia é permitido; seus itens são combinados. */
    public static List<TelaCardapios.ItemCardapio> listarItensDisponiveis(int diaSemana) {
        List<TelaCardapios.ItemCardapio> itens = new ArrayList<TelaCardapios.ItemCardapio>();
        for (TelaCardapios.Cardapio cardapio : listarCardapios()) {
            if (cardapio.getDiaSemana() == diaSemana && cardapio.isDisponivel()) {
                for (TelaCardapios.ItemCardapio item : cardapio.getItens()) {
                    if (item.isDisponivel()) itens.add(item);
                }
            }
        }
        return itens;
    }

    public static TelaPedidos.Pedido registrarPedido(int idCliente,
            List<TelaPedidos.ItemPedido> itens, boolean antecipado, boolean pago,
            int diaCardapio) {
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("Adicione pelo menos um item ao pedido.");
        }
        if (!antecipado && !pago) {
            throw new IllegalArgumentException("Uma compra feita na hora deve ser registrada como paga.");
        }
        try (Connection conexao = DATA_SOURCE.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                Cliente cliente = bloquearCliente(conexao, idCliente);
                if (cliente == null) throw new IllegalArgumentException("O cliente não está mais cadastrado.");
                if (temPedidoPendente(conexao, idCliente)) {
                    throw new IllegalArgumentException(
                            "Este cliente já possui um pedido ativo. Conclua o pedido anterior primeiro.");
                }

                BigDecimal total = validarItens(conexao, itens, diaCardapio);
                if (pago) debitarSaldo(conexao, cliente, total);

                BigDecimal saldoApos = pago
                        ? cliente.getSaldo().subtract(total) : cliente.getSaldo();
                LocalDateTime dataPedido = LocalDateTime.now();
                int idPedido = inserirPedido(conexao, cliente, itens, antecipado, pago,
                        diaCardapio, saldoApos, dataPedido);
                inserirItensPedido(conexao, idPedido, itens);
                conexao.commit();

                TelaPedidos.Pedido pedido = new TelaPedidos.Pedido(idPedido, dataPedido,
                        new ArrayList<TelaPedidos.ItemPedido>(itens), antecipado,
                        idCliente, cliente.getNomeCliente(), pago, false,
                        false, null, diaCardapio, saldoApos);
                enviarEmailDepoisDoCommit(pedido, cliente);
                return pedido;
            } catch (Exception erro) {
                try { conexao.rollback(); } catch (SQLException ignorado) { }
                if (erro instanceof IllegalArgumentException) throw (IllegalArgumentException) erro;
                if (erro instanceof SQLException) throw (SQLException) erro;
                throw new IllegalStateException(erro);
            } finally {
                try { conexao.setAutoCommit(true); } catch (SQLException ignorado) { }
            }
        } catch (SQLException erro) {
            if ("23000".equals(erro.getSQLState())) {
                throw new IllegalArgumentException("Este cliente já possui um pedido ativo.");
            }
            throw banco("Não foi possível registrar o pedido", erro);
        }
    }

    public static List<TelaPedidos.Pedido> listarPedidos() {
        try {
            return PEDIDOS.listarTodos();
        } catch (SQLException erro) {
            throw banco("Não foi possível carregar os pedidos", erro);
        }
    }

    public static void marcarPago(int idPedido) {
        try (Connection conexao = DATA_SOURCE.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                int idCliente;
                boolean pago;
                boolean retirado;
                try (PreparedStatement comando = conexao.prepareStatement(
                        "SELECT idcliente, pago, retirado FROM pedido WHERE idpedido = ? FOR UPDATE")) {
                    comando.setInt(1, idPedido);
                    try (ResultSet resultado = comando.executeQuery()) {
                        if (!resultado.next()) throw new IllegalArgumentException("Pedido não encontrado.");
                        idCliente = resultado.getInt("idcliente");
                        pago = resultado.getBoolean("pago");
                        retirado = resultado.getBoolean("retirado");
                    }
                }
                if (pago) return;
                if (retirado) throw new IllegalArgumentException("Pedido retirado não pode ser alterado.");
                Cliente cliente = bloquearCliente(conexao, idCliente);
                BigDecimal total = totalPedido(conexao, idPedido);
                debitarSaldo(conexao, cliente, total);
                try (PreparedStatement comando = conexao.prepareStatement(
                        "UPDATE pedido SET pago = TRUE WHERE idpedido = ?")) {
                    comando.setInt(1, idPedido);
                    comando.executeUpdate();
                }
                conexao.commit();
            } catch (Exception erro) {
                try { conexao.rollback(); } catch (SQLException ignorado) { }
                if (erro instanceof IllegalArgumentException) throw (IllegalArgumentException) erro;
                if (erro instanceof SQLException) throw (SQLException) erro;
                throw new IllegalStateException(erro);
            }
        } catch (SQLException erro) {
            throw banco("Não foi possível confirmar o pagamento", erro);
        }
    }

    public static void marcarRetirado(int idPedido) {
        try {
            if (!PEDIDOS.marcarRetirado(idPedido)) {
                throw new IllegalArgumentException(
                        "O pedido precisa estar pago e ainda não retirado.");
            }
        } catch (SQLException erro) {
            throw banco("Não foi possível confirmar a retirada", erro);
        }
    }

    public static void reenviarEmail(int idPedido) {
        TelaPedidos.Pedido pedido = buscarPedido(idPedido);
        if (pedido == null) throw new IllegalArgumentException("Pedido não encontrado.");
        Cliente cliente = buscarCliente(pedido.getIdCliente());
        enviarEmailDepoisDoCommit(pedido, cliente);
        if (!pedido.isEmailEnviado()) {
            throw new IllegalStateException("E-mail não enviado: " + pedido.getEmailErro());
        }
    }

    static void definirServicoEmailParaTeste(ServicoEmail servico) {
        servicoEmail = servico == null ? new ServicoEmailSmtp() : servico;
    }

    private static Cliente bloquearCliente(Connection conexao, int idCliente) throws SQLException {
        String sql = "SELECT idcliente, nomecliente, nomeresponsavel, saldo, limitesaldo, "
                + "emailresponsavel, alergias FROM cliente WHERE idcliente = ? FOR UPDATE";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, idCliente);
            try (ResultSet r = comando.executeQuery()) {
                if (!r.next()) return null;
                return new Cliente(r.getInt("idcliente"), r.getString("nomecliente"),
                        r.getString("nomeresponsavel"), r.getBigDecimal("saldo"),
                        r.getBigDecimal("limitesaldo"), r.getString("emailresponsavel"),
                        r.getString("alergias"));
            }
        }
    }

    private static boolean temPedidoPendente(Connection conexao, int idCliente) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(
                "SELECT 1 FROM pedido WHERE idcliente = ? AND retirado = FALSE LIMIT 1 FOR UPDATE")) {
            comando.setInt(1, idCliente);
            try (ResultSet resultado = comando.executeQuery()) { return resultado.next(); }
        }
    }

    private static BigDecimal validarItens(Connection conexao,
            List<TelaPedidos.ItemPedido> itens, int diaCardapio) throws SQLException {
        BigDecimal total = BigDecimal.ZERO;
        Map<Integer, List<TelaCardapios.ItemCardapio>> cache =
                new LinkedHashMap<Integer, List<TelaCardapios.ItemCardapio>>();
        for (TelaPedidos.ItemPedido item : itens) {
            if (item == null || item.getOrigem() == null || item.getOrigem().getIdCardapio() <= 0) {
                throw new IllegalArgumentException("Item inválido ou não salvo no cardápio.");
            }
            int idCardapio = item.getOrigem().getIdCardapio();
            List<TelaCardapios.ItemCardapio> atuais = cache.get(idCardapio);
            if (atuais == null) {
                atuais = bloquearCardapio(conexao, idCardapio, diaCardapio);
                cache.put(idCardapio, atuais);
            }
            boolean encontrado = false;
            for (TelaCardapios.ItemCardapio atual : atuais) {
                if (atual.isDisponivel()
                        && atual.getNome().equals(item.getNome())
                        && atual.getPreco().compareTo(item.getPreco()) == 0) {
                    encontrado = true;
                    break;
                }
            }
            if (!encontrado) {
                throw new IllegalArgumentException(
                        "Um item foi alterado, removido ou ficou indisponível. Atualize o pedido.");
            }
            total = total.add(item.subtotal());
        }
        return total;
    }

    private static List<TelaCardapios.ItemCardapio> bloquearCardapio(
            Connection conexao, int idCardapio, int diaCardapio) throws SQLException {
        String sql = "SELECT cardapio_json, disponivel, diasemana FROM cardapio "
                + "WHERE id_cardapio = ? FOR UPDATE";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, idCardapio);
            try (ResultSet resultado = comando.executeQuery()) {
                if (!resultado.next() || !resultado.getBoolean("disponivel")
                        || resultado.getInt("diasemana") != diaCardapio) {
                    throw new IllegalArgumentException(
                            "O cardápio não está disponível para o dia selecionado.");
                }
                return vincularItens(idCardapio,
                        CardapioJson.ler(resultado.getString("cardapio_json")));
            }
        }
    }

    private static void debitarSaldo(Connection conexao, Cliente cliente, BigDecimal total)
            throws SQLException {
        if (cliente == null || cliente.getSaldo().compareTo(total) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente para confirmar o pagamento.");
        }
        try (PreparedStatement comando = conexao.prepareStatement(
                "UPDATE cliente SET saldo = saldo - ? WHERE idcliente = ? AND saldo >= ?")) {
            comando.setBigDecimal(1, total);
            comando.setInt(2, cliente.getIdCliente());
            comando.setBigDecimal(3, total);
            if (comando.executeUpdate() != 1) {
                throw new IllegalArgumentException("O saldo mudou. Atualize os dados e tente novamente.");
            }
        }
    }

    private static int inserirPedido(Connection conexao, Cliente cliente,
            List<TelaPedidos.ItemPedido> itens, boolean antecipado, boolean pago,
            int diaCardapio, BigDecimal saldoApos, LocalDateTime dataPedido) throws SQLException {
        String sql = "INSERT INTO pedido (datapedido, itempedido, antecipado, idcliente, "
                + "dia_cardapio, pago, retirado, email_enviado, saldo_apos) "
                + "VALUES (?, ?, ?, ?, ?, ?, FALSE, FALSE, ?)";
        try (PreparedStatement comando = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            comando.setTimestamp(1, Timestamp.valueOf(dataPedido));
            comando.setString(2, gerarItensJson(itens));
            comando.setBoolean(3, antecipado);
            comando.setInt(4, cliente.getIdCliente());
            comando.setInt(5, diaCardapio);
            comando.setBoolean(6, pago);
            comando.setBigDecimal(7, saldoApos);
            comando.executeUpdate();
            try (ResultSet chaves = comando.getGeneratedKeys()) {
                if (!chaves.next()) throw new SQLException("O banco não devolveu o ID do pedido.");
                return chaves.getInt(1);
            }
        }
    }

    private static void inserirItensPedido(Connection conexao, int idPedido,
            List<TelaPedidos.ItemPedido> itens) throws SQLException {
        String sql = "INSERT INTO pedido_item (idpedido, nome, preco_unitario, quantidade) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            for (TelaPedidos.ItemPedido item : itens) {
                comando.setInt(1, idPedido);
                comando.setString(2, item.getNome());
                comando.setBigDecimal(3, item.getPreco());
                comando.setInt(4, item.getQuantidade());
                comando.addBatch();
            }
            comando.executeBatch();
        }
    }

    private static List<TelaPedidos.ItemPedido> carregarItensPedido(Connection conexao, int idPedido)
            throws SQLException {
        List<TelaPedidos.ItemPedido> itens = new ArrayList<TelaPedidos.ItemPedido>();
        String sql = "SELECT nome, preco_unitario, quantidade FROM pedido_item "
                + "WHERE idpedido = ? ORDER BY id_pedido_item";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, idPedido);
            try (ResultSet r = comando.executeQuery()) {
                while (r.next()) {
                    itens.add(new TelaPedidos.ItemPedido(r.getString("nome"),
                            r.getBigDecimal("preco_unitario"), r.getInt("quantidade")));
                }
            }
        }
        return itens;
    }

    private static BigDecimal totalPedido(Connection conexao, int idPedido) throws SQLException {
        try (PreparedStatement comando = conexao.prepareStatement(
                "SELECT COALESCE(SUM(preco_unitario * quantidade), 0) total "
                + "FROM pedido_item WHERE idpedido = ?")) {
            comando.setInt(1, idPedido);
            try (ResultSet r = comando.executeQuery()) {
                r.next();
                BigDecimal total = r.getBigDecimal("total");
                if (total == null || total.signum() <= 0) {
                    throw new IllegalArgumentException("Pedido sem itens persistidos.");
                }
                return total;
            }
        }
    }

    private static TelaPedidos.Pedido buscarPedido(int idPedido) {
        for (TelaPedidos.Pedido pedido : listarPedidos()) {
            if (pedido.getIdPedido() == idPedido) return pedido;
        }
        return null;
    }

    private static void enviarEmailDepoisDoCommit(TelaPedidos.Pedido pedido, Cliente cliente) {
        String erro = null;
        boolean enviado = false;
        try {
            servicoEmail.enviar(new EmailCompra(pedido.getIdPedido(), cliente.getEmailResponsavel(),
                    cliente.getNomeCliente(), pedido.resumoItensEmLinhas(),
                    pedido.total(), pedido.getSaldoApos(), pedido.getDataPedido(), pedido.isPago()));
            enviado = true;
        } catch (Exception falha) {
            erro = mensagemCurta(falha);
        }
        pedido.definirResultadoEmail(enviado, erro);
        String sql = "UPDATE pedido SET email_enviado = ?, email_erro = ? WHERE idpedido = ?";
        try (Connection conexao = DATA_SOURCE.getConnection();
                PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setBoolean(1, enviado);
            comando.setString(2, erro);
            comando.setInt(3, pedido.getIdPedido());
            comando.executeUpdate();
        } catch (SQLException falhaRegistro) {
            pedido.definirResultadoEmail(false,
                    "Compra salva, mas não foi possível registrar o resultado do e-mail: "
                    + mensagemCurta(falhaRegistro));
        }
    }

    private static String gerarItensJson(List<TelaPedidos.ItemPedido> itens) {
        StringBuilder json = new StringBuilder("{\"itens\":[");
        for (int i = 0; i < itens.size(); i++) {
            if (i > 0) json.append(',');
            TelaPedidos.ItemPedido item = itens.get(i);
            json.append("{\"nome\":\"").append(CardapioJson.escapar(item.getNome()))
                    .append("\",\"preco\":").append(item.getPreco().toPlainString())
                    .append(",\"quantidade\":").append(item.getQuantidade()).append('}');
        }
        return json.append("]}").toString();
    }

    private static List<TelaCardapios.ItemCardapio> vincularItens(int idCardapio,
            List<TelaCardapios.ItemCardapio> itens) {
        List<TelaCardapios.ItemCardapio> vinculados =
                new ArrayList<TelaCardapios.ItemCardapio>();
        for (TelaCardapios.ItemCardapio item : itens) {
            vinculados.add(new TelaCardapios.ItemCardapio(idCardapio,
                    item.getNome(), item.getPreco(), item.isDisponivel()));
        }
        return vinculados;
    }

    private static String mensagemCurta(Throwable erro) {
        String mensagem = erro.getMessage();
        if (mensagem == null || mensagem.trim().isEmpty()) mensagem = erro.getClass().getSimpleName();
        return mensagem.length() > 480 ? mensagem.substring(0, 480) : mensagem;
    }

    private static IllegalStateException banco(String contexto, SQLException erro) {
        return new IllegalStateException(contexto + ": " + mensagemCurta(erro), erro);
    }
}

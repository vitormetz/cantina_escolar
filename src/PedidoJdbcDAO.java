import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** JDBC explícito para consultas e conclusão dos pedidos. */
public final class PedidoJdbcDAO implements PedidoDAO {
    private final DataSource dataSource;

    public PedidoJdbcDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<TelaPedidos.Pedido> listarTodos() throws SQLException {
        String sql = "SELECT p.idpedido, p.datapedido, p.antecipado, p.idcliente, "
                + "c.nomecliente, p.pago, p.retirado, p.email_enviado, p.email_erro, "
                + "p.dia_cardapio, p.saldo_apos FROM pedido p "
                + "JOIN cliente c ON c.idcliente = p.idcliente "
                + "ORDER BY p.retirado, p.datapedido DESC";
        List<Cabecalho> cabecalhos = new ArrayList<Cabecalho>();
        try (Connection conexao = dataSource.getConnection();
                PreparedStatement comando = conexao.prepareStatement(sql);
                ResultSet resultado = comando.executeQuery()) {
            while (resultado.next()) cabecalhos.add(new Cabecalho(resultado));
        }

        List<TelaPedidos.Pedido> pedidos = new ArrayList<TelaPedidos.Pedido>();
        try (Connection conexao = dataSource.getConnection()) {
            for (Cabecalho c : cabecalhos) {
                pedidos.add(c.criar(carregarItens(conexao, c.idPedido)));
            }
        }
        return pedidos;
    }

    @Override
    public boolean existeParaCliente(int idCliente) throws SQLException {
        try (Connection conexao = dataSource.getConnection();
                PreparedStatement comando = conexao.prepareStatement(
                        "SELECT 1 FROM pedido WHERE idcliente = ? LIMIT 1")) {
            comando.setInt(1, idCliente);
            try (ResultSet resultado = comando.executeQuery()) {
                return resultado.next();
            }
        }
    }

    @Override
    public boolean marcarRetirado(int idPedido) throws SQLException {
        String sql = "UPDATE pedido SET retirado = TRUE "
                + "WHERE idpedido = ? AND pago = TRUE AND retirado = FALSE";
        try (Connection conexao = dataSource.getConnection();
                PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, idPedido);
            return comando.executeUpdate() == 1;
        }
    }

    private List<TelaPedidos.ItemPedido> carregarItens(Connection conexao, int idPedido)
            throws SQLException {
        List<TelaPedidos.ItemPedido> itens = new ArrayList<TelaPedidos.ItemPedido>();
        try (PreparedStatement comando = conexao.prepareStatement(
                "SELECT nome, preco_unitario, quantidade FROM pedido_item "
                + "WHERE idpedido = ? ORDER BY id_pedido_item")) {
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

    private static final class Cabecalho {
        private final int idPedido;
        private final java.time.LocalDateTime data;
        private final boolean antecipado;
        private final int idCliente;
        private final String nomeCliente;
        private final boolean pago;
        private final boolean retirado;
        private final boolean emailEnviado;
        private final String emailErro;
        private final int diaCardapio;
        private final java.math.BigDecimal saldoApos;

        private Cabecalho(ResultSet r) throws SQLException {
            idPedido = r.getInt("idpedido");
            data = r.getTimestamp("datapedido").toLocalDateTime();
            antecipado = r.getBoolean("antecipado");
            idCliente = r.getInt("idcliente");
            nomeCliente = r.getString("nomecliente");
            pago = r.getBoolean("pago");
            retirado = r.getBoolean("retirado");
            emailEnviado = r.getBoolean("email_enviado");
            emailErro = r.getString("email_erro");
            diaCardapio = r.getInt("dia_cardapio");
            saldoApos = r.getBigDecimal("saldo_apos");
        }

        private TelaPedidos.Pedido criar(List<TelaPedidos.ItemPedido> itens) {
            return new TelaPedidos.Pedido(idPedido, data, itens, antecipado,
                    idCliente, nomeCliente, pago, retirado, emailEnviado,
                    emailErro, diaCardapio, saldoApos);
        }
    }
}

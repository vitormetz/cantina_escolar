import java.sql.SQLException;
import java.util.List;

/** Consultas e alterações simples da tabela pedido. */
public interface PedidoDAO {
    List<TelaPedidos.Pedido> listarTodos() throws SQLException;
    boolean existeParaCliente(int idCliente) throws SQLException;
    boolean marcarRetirado(int idPedido) throws SQLException;
}

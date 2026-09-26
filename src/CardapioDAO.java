import java.sql.SQLException;
import java.util.List;

/** Operações persistidas dos cardápios. */
public interface CardapioDAO {
    List<TelaCardapios.Cardapio> listarTodos() throws SQLException;

    TelaCardapios.Cardapio salvar(int idCardapio, int diaSemana,
            boolean disponivel, List<TelaCardapios.ItemCardapio> itens) throws SQLException;

    boolean excluir(int idCardapio) throws SQLException;
}

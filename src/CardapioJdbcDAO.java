import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** JDBC explícito da tabela cardapio. */
public final class CardapioJdbcDAO implements CardapioDAO {
    private final DataSource dataSource;

    public CardapioJdbcDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<TelaCardapios.Cardapio> listarTodos() throws SQLException {
        String sql = "SELECT id_cardapio, cardapio_json, disponivel, diasemana "
                + "FROM cardapio ORDER BY diasemana, id_cardapio";
        List<TelaCardapios.Cardapio> cardapios = new ArrayList<TelaCardapios.Cardapio>();
        try (Connection conexao = dataSource.getConnection();
                PreparedStatement comando = conexao.prepareStatement(sql);
                ResultSet resultado = comando.executeQuery()) {
            while (resultado.next()) {
                int id = resultado.getInt("id_cardapio");
                cardapios.add(new TelaCardapios.Cardapio(id,
                        resultado.getInt("diasemana"), resultado.getBoolean("disponivel"),
                        vincular(id, CardapioJson.ler(resultado.getString("cardapio_json")))));
            }
        }
        return cardapios;
    }

    @Override
    public TelaCardapios.Cardapio salvar(int idCardapio, int diaSemana,
            boolean disponivel, List<TelaCardapios.ItemCardapio> itens) throws SQLException {
        String json = CardapioJson.gerar(itens);
        try (Connection conexao = dataSource.getConnection()) {
            if (idCardapio == 0) {
                try (PreparedStatement comando = conexao.prepareStatement(
                        "INSERT INTO cardapio (cardapio_json, disponivel, diasemana) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    preencher(comando, json, disponivel, diaSemana);
                    comando.executeUpdate();
                    try (ResultSet chaves = comando.getGeneratedKeys()) {
                        if (!chaves.next()) throw new SQLException("O banco não devolveu o ID do cardápio.");
                        idCardapio = chaves.getInt(1);
                    }
                }
            } else {
                try (PreparedStatement comando = conexao.prepareStatement(
                        "UPDATE cardapio SET cardapio_json = ?, disponivel = ?, diasemana = ? "
                        + "WHERE id_cardapio = ?")) {
                    preencher(comando, json, disponivel, diaSemana);
                    comando.setInt(4, idCardapio);
                    if (comando.executeUpdate() != 1) return null;
                }
            }
        }
        return new TelaCardapios.Cardapio(
                idCardapio, diaSemana, disponivel, vincular(idCardapio, itens));
    }

    @Override
    public boolean excluir(int idCardapio) throws SQLException {
        try (Connection conexao = dataSource.getConnection();
                PreparedStatement comando = conexao.prepareStatement(
                        "DELETE FROM cardapio WHERE id_cardapio = ?")) {
            comando.setInt(1, idCardapio);
            return comando.executeUpdate() == 1;
        }
    }

    private void preencher(PreparedStatement comando, String json,
            boolean disponivel, int diaSemana) throws SQLException {
        comando.setString(1, json);
        comando.setBoolean(2, disponivel);
        comando.setInt(3, diaSemana);
    }

    private List<TelaCardapios.ItemCardapio> vincular(int idCardapio,
            List<TelaCardapios.ItemCardapio> itens) {
        List<TelaCardapios.ItemCardapio> resultado =
                new ArrayList<TelaCardapios.ItemCardapio>();
        for (TelaCardapios.ItemCardapio item : itens) {
            resultado.add(new TelaCardapios.ItemCardapio(idCardapio,
                    item.getNome(), item.getPreco(), item.isDisponivel()));
        }
        return resultado;
    }
}

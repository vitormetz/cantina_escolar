package br.com.time7.cantina.dao;

import br.com.time7.cantina.model.Cliente;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Implementação do ClienteDAO usando JDBC e comandos SQL preparados. */
public class ClienteJdbcDAO implements ClienteDAO {
    private final DataSource dataSource;

    public ClienteJdbcDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Cliente inserir(Cliente cliente) throws SQLException {
        String sql = "INSERT INTO cliente "
                + "(nomecliente, nomeresponsavel, saldo, limitesaldo, "
                + "emailresponsavel, alergias) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conexao = dataSource.getConnection();
                PreparedStatement comando = conexao.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {

            preencherComando(comando, cliente);
            comando.executeUpdate();

            try (ResultSet chaves = comando.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new SQLException("O banco não devolveu o ID do cliente cadastrado.");
                }

                return new Cliente(
                        chaves.getInt(1),
                        cliente.getNomeCliente(),
                        cliente.getNomeResponsavel(),
                        cliente.getSaldo(),
                        cliente.getLimiteSaldo(),
                        cliente.getEmailResponsavel(),
                        cliente.getAlergias());
            }
        }
    }

    @Override
    public List<Cliente> listarTodos() throws SQLException {
        String sql = "SELECT idcliente, nomecliente, nomeresponsavel, saldo, "
                + "limitesaldo, emailresponsavel, alergias "
                + "FROM cliente ORDER BY nomecliente";
        List<Cliente> clientes = new ArrayList<Cliente>();

        try (Connection conexao = dataSource.getConnection();
                PreparedStatement comando = conexao.prepareStatement(sql);
                ResultSet resultado = comando.executeQuery()) {

            while (resultado.next()) {
                clientes.add(mapearCliente(resultado));
            }
        }

        return clientes;
    }

    @Override
    public Cliente buscarPorId(int idCliente) throws SQLException {
        String sql = "SELECT idcliente, nomecliente, nomeresponsavel, saldo, "
                + "limitesaldo, emailresponsavel, alergias "
                + "FROM cliente WHERE idcliente = ?";

        try (Connection conexao = dataSource.getConnection();
                PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, idCliente);

            try (ResultSet resultado = comando.executeQuery()) {
                return resultado.next() ? mapearCliente(resultado) : null;
            }
        }
    }

    @Override
    public boolean atualizar(Cliente cliente) throws SQLException {
        String sql = "UPDATE cliente SET nomecliente = ?, nomeresponsavel = ?, "
                + "saldo = ?, limitesaldo = ?, emailresponsavel = ?, alergias = ? "
                + "WHERE idcliente = ?";

        try (Connection conexao = dataSource.getConnection();
                PreparedStatement comando = conexao.prepareStatement(sql)) {
            preencherComando(comando, cliente);
            comando.setInt(7, cliente.getIdCliente());
            return comando.executeUpdate() == 1;
        }
    }

    @Override
    public boolean excluir(int idCliente) throws SQLException {
        String sql = "DELETE FROM cliente WHERE idcliente = ?";

        try (Connection conexao = dataSource.getConnection();
                PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, idCliente);
            return comando.executeUpdate() == 1;
        }
    }

    /** Preenche os seis campos usados tanto no INSERT quanto no UPDATE. */
    private void preencherComando(PreparedStatement comando, Cliente cliente)
            throws SQLException {
        comando.setString(1, cliente.getNomeCliente());
        comando.setString(2, cliente.getNomeResponsavel());
        comando.setBigDecimal(3, cliente.getSaldo());
        comando.setBigDecimal(4, cliente.getLimiteSaldo());
        comando.setString(5, cliente.getEmailResponsavel());
        comando.setString(6, cliente.getAlergias());
    }

    /** Converte a linha retornada pelo banco em um objeto Cliente. */
    private Cliente mapearCliente(ResultSet resultado) throws SQLException {
        return new Cliente(
                resultado.getInt("idcliente"),
                resultado.getString("nomecliente"),
                resultado.getString("nomeresponsavel"),
                resultado.getBigDecimal("saldo"),
                resultado.getBigDecimal("limitesaldo"),
                resultado.getString("emailresponsavel"),
                resultado.getString("alergias"));
    }
}

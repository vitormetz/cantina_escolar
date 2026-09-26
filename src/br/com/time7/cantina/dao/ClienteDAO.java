package br.com.time7.cantina.dao;

import br.com.time7.cantina.model.Cliente;

import java.sql.SQLException;
import java.util.List;

/** Define as operações de banco necessárias para trabalhar com clientes. */
public interface ClienteDAO {
    Cliente inserir(Cliente cliente) throws SQLException;

    List<Cliente> listarTodos() throws SQLException;

    Cliente buscarPorId(int idCliente) throws SQLException;

    Cliente buscarPorNome(String nomeCliente) throws SQLException;

    boolean atualizar(Cliente cliente) throws SQLException;

    boolean excluir(int idCliente) throws SQLException;
}

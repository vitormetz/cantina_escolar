package br.com.time7.cantina.infra;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

/** Programa pequeno para conferir a conexão sem precisar abrir a interface. */
public class TestarConexao {
    public static void main(String[] args) {
        try (Connection conexao = BancoDados.getDataSource().getConnection()) {
            DatabaseMetaData dados = conexao.getMetaData();
            System.out.println("Conexão realizada com sucesso.");
            System.out.println("Banco: " + dados.getDatabaseProductName());
            System.out.println("Versão: " + dados.getDatabaseProductVersion());
        } catch (Exception excecao) {
            System.err.println("Não foi possível conectar ao banco.");
            System.err.println(excecao.getMessage());
        }
    }
}

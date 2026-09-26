package br.com.time7.cantina.infra;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * DataSource simples baseado no DriverManager.
 *
 * Ele centraliza a criação das conexões. Assim, os DAOs não precisam conhecer
 * URL, usuário ou senha e podem receber apenas um DataSource no construtor.
 */
public class CantinaDataSource implements DataSource {
    private final String url;
    private final String usuario;
    private final String senha;

    public CantinaDataSource(BancoConfig config) {
        this.url = config.getUrl();
        this.usuario = config.getUsuario();
        this.senha = config.getSenha();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, usuario, senha);
    }

    @Override
    public Connection getConnection(String usuario, String senha) throws SQLException {
        return DriverManager.getConnection(url, usuario, senha);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return DriverManager.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter escritor) throws SQLException {
        DriverManager.setLogWriter(escritor);
    }

    @Override
    public void setLoginTimeout(int segundos) throws SQLException {
        DriverManager.setLoginTimeout(segundos);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return DriverManager.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return Logger.getLogger("br.com.time7.cantina");
    }

    @Override
    public <T> T unwrap(Class<T> interfaceEsperada) throws SQLException {
        if (interfaceEsperada.isInstance(this)) {
            return interfaceEsperada.cast(this);
        }
        throw new SQLException("O DataSource não implementa " + interfaceEsperada.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> interfaceEsperada) {
        return interfaceEsperada.isInstance(this);
    }
}

package br.com.time7.cantina.infra;

import javax.sql.DataSource;

/** Fornece uma única instância compartilhada do DataSource da aplicação. */
public final class BancoDados {
    private static final DataSource DATA_SOURCE =
            new CantinaDataSource(BancoConfig.carregar());

    private BancoDados() {
        // Impede a criação de objetos desta classe utilitária.
    }

    public static DataSource getDataSource() {
        return DATA_SOURCE;
    }
}

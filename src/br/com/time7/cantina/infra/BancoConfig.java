package br.com.time7.cantina.infra;

/**
 * Carrega a configuração do banco a partir de variáveis de ambiente.
 *
 * A senha não fica gravada no GitHub. Cada integrante configura as variáveis
 * no próprio computador antes de iniciar a aplicação.
 */
public final class BancoConfig {
    private static final String URL_PADRAO =
            "jdbc:mysql://localhost:3306/cantina_escolar"
                    + "?useSSL=false&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=America/Sao_Paulo";

    private final String url;
    private final String usuario;
    private final String senha;

    private BancoConfig(String url, String usuario, String senha) {
        this.url = url;
        this.usuario = usuario;
        this.senha = senha;
    }

    public static BancoConfig carregar() {
        return new BancoConfig(
                lerVariavel("CANTINA_DB_URL", URL_PADRAO),
                lerVariavel("CANTINA_DB_USER", "root"),
                lerVariavel("CANTINA_DB_PASSWORD", ""));
    }

    private static String lerVariavel(String nome, String valorPadrao) {
        String valor = System.getenv(nome);
        return valor == null || valor.trim().isEmpty() ? valorPadrao : valor.trim();
    }

    public String getUrl() {
        return url;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getSenha() {
        return senha;
    }
}

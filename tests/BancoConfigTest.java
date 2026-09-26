import br.com.time7.cantina.infra.BancoConfig;

import java.io.File;

/** Testa variáveis em processos separados sem abrir conexão ou expor senhas. */
public class BancoConfigTest {
    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            String original = System.getenv("CANTINA_DB_PASSWORD");
            String esperado = original == null ? "" : original;
            BancoConfig config = BancoConfig.carregar();
            if (!esperado.equals(config.getSenha())) {
                throw new AssertionError("A senha foi alterada durante a leitura.");
            }
            if (!"usuario_teste".equals(config.getUsuario())) {
                throw new AssertionError("O usuário não foi normalizado.");
            }
            return;
        }

        String executavel = new File(System.getProperty("java.home"), "bin/java").getPath();
        if (!new File(executavel).isFile()) {
            executavel += ".exe";
        }
        String[] senhas = { null, "", " senha_de_teste ", "   ", "senha_simples" };
        for (String senha : senhas) {
            ProcessBuilder processo = new ProcessBuilder(executavel, "-cp",
                    System.getProperty("java.class.path"), "BancoConfigTest", "verificar");
            processo.environment().put("CANTINA_DB_USER", " usuario_teste ");
            if (senha == null) {
                processo.environment().remove("CANTINA_DB_PASSWORD");
            } else {
                processo.environment().put("CANTINA_DB_PASSWORD", senha);
            }
            int codigo = processo.inheritIO().start().waitFor();
            if (codigo != 0) {
                throw new AssertionError("Falha no teste de configuração: " + codigo);
            }
        }
        System.out.println("BancoConfigTest: OK");
    }
}

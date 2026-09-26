import br.com.time7.cantina.infra.BancoDados;
import br.com.time7.cantina.model.Cliente;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Teste de integração opcional. Usa um MySQL de teste já criado e migrado.
 * Só executa quando CANTINA_TEST_DB=1 para nunca apagar dados por acidente.
 */
public class FluxoPersistenciaTest {
    public static void main(String[] args) throws Exception {
        if (!"1".equals(System.getenv("CANTINA_TEST_DB"))) {
            System.out.println("FluxoPersistenciaTest: IGNORADO (defina CANTINA_TEST_DB=1)");
            return;
        }

        long sufixo = System.currentTimeMillis();
        int idCliente = 0;
        int idCardapio = 0;
        try {
            BancoAplicacao.testarConexao();
            ContadorEmail email = new ContadorEmail();
            BancoAplicacao.definirServicoEmailParaTeste(email);

            Cliente cliente = BancoAplicacao.inserirCliente(new Cliente(
                    0, "Teste " + sufixo, "Responsável", new BigDecimal("50.00"),
                    BigDecimal.ZERO, "responsavel@teste.com", ""));
            idCliente = cliente.getIdCliente();

            int dia = LocalDate.now().getDayOfWeek().getValue();
            List<TelaCardapios.ItemCardapio> itensCardapio =
                    new ArrayList<TelaCardapios.ItemCardapio>();
            itensCardapio.add(new TelaCardapios.ItemCardapio(
                    "Suco teste", new BigDecimal("5.00"), true));
            TelaCardapios.Cardapio cardapio = BancoAplicacao.salvarCardapio(
                    0, dia, true, itensCardapio);
            idCardapio = cardapio.getIdCardapio();

            List<TelaPedidos.ItemPedido> carrinho = new ArrayList<TelaPedidos.ItemPedido>();
            carrinho.add(new TelaPedidos.ItemPedido(cardapio.getItens().get(0), 2));
            TelaPedidos.Pedido pedido = BancoAplicacao.registrarPedido(
                    idCliente, carrinho, true, false, dia);

            conferir("50.00", BancoAplicacao.buscarCliente(idCliente).getSaldo());
            if (pedido.isPago()) throw new AssertionError("Pedido antecipado nasceu pago.");
            if (email.quantidade != 1 || !pedido.isEmailEnviado()) {
                throw new AssertionError("E-mail simulado não foi registrado exatamente uma vez.");
            }

            BancoAplicacao.marcarPago(pedido.getIdPedido());
            conferir("40.00", BancoAplicacao.buscarCliente(idCliente).getSaldo());
            BancoAplicacao.marcarRetirado(pedido.getIdPedido());

            BancoAplicacao.salvarCardapio(idCardapio, dia, false, itensCardapio);
            try {
                BancoAplicacao.registrarPedido(idCliente, carrinho, false, true, dia);
                throw new AssertionError("Item de cardápio indisponível foi aceito.");
            } catch (IllegalArgumentException esperado) {
                conferir("40.00", BancoAplicacao.buscarCliente(idCliente).getSaldo());
                if (email.quantidade != 1) throw new AssertionError("Houve e-mail de compra recusada.");
            }
            cardapio = BancoAplicacao.salvarCardapio(idCardapio, dia, true, itensCardapio);

            List<TelaPedidos.ItemPedido> compraCara = new ArrayList<TelaPedidos.ItemPedido>();
            compraCara.add(new TelaPedidos.ItemPedido(cardapio.getItens().get(0), 9));
            try {
                BancoAplicacao.registrarPedido(idCliente, compraCara, false, true, dia);
                throw new AssertionError("Pedido sem saldo foi aceito.");
            } catch (IllegalArgumentException esperado) {
                conferir("40.00", BancoAplicacao.buscarCliente(idCliente).getSaldo());
                if (email.quantidade != 1) throw new AssertionError("Houve e-mail de compra recusada.");
            }

            BancoAplicacao.definirServicoEmailParaTeste(new EmailComFalha());
            List<TelaPedidos.ItemPedido> compraFinal = new ArrayList<TelaPedidos.ItemPedido>();
            compraFinal.add(new TelaPedidos.ItemPedido(cardapio.getItens().get(0), 1));
            TelaPedidos.Pedido pedidoComFalhaEmail = BancoAplicacao.registrarPedido(
                    idCliente, compraFinal, false, true, dia);
            conferir("35.00", BancoAplicacao.buscarCliente(idCliente).getSaldo());
            if (pedidoComFalhaEmail.isEmailEnviado()
                    || pedidoComFalhaEmail.getEmailErro() == null) {
                throw new AssertionError("Falha simulada de e-mail não foi registrada.");
            }

            try {
                BancoAplicacao.registrarPedido(idCliente, compraFinal, false, true, dia);
                throw new AssertionError("Segundo pedido ativo foi aceito.");
            } catch (IllegalArgumentException esperado) {
                conferir("35.00", BancoAplicacao.buscarCliente(idCliente).getSaldo());
            }

            BancoAplicacao.definirServicoEmailParaTeste(email);
            BancoAplicacao.reenviarEmail(pedidoComFalhaEmail.getIdPedido());
            if (email.quantidade != 2) {
                throw new AssertionError("Reenvio não ocorreu exatamente uma vez.");
            }
            BancoAplicacao.marcarRetirado(pedidoComFalhaEmail.getIdPedido());
            boolean historico = false;
            for (TelaPedidos.Pedido salvo : BancoAplicacao.listarPedidos()) {
                if (salvo.getIdPedido() == pedidoComFalhaEmail.getIdPedido()) historico = true;
            }
            if (!historico) throw new AssertionError("Pedido retirado sumiu do histórico.");
            System.out.println("FluxoPersistenciaTest: OK");
        } finally {
            BancoAplicacao.definirServicoEmailParaTeste(null);
            limpar(idCardapio, idCliente);
        }
    }

    private static void conferir(String esperado, BigDecimal recebido) {
        if (new BigDecimal(esperado).compareTo(recebido) != 0) {
            throw new AssertionError("Esperado " + esperado + ", recebido " + recebido);
        }
    }

    private static void limpar(int idCardapio, int idCliente) throws Exception {
        try (Connection conexao = BancoDados.getDataSource().getConnection()) {
            if (idCliente > 0) executar(conexao, "DELETE FROM pedido WHERE idcliente = ?", idCliente);
            if (idCardapio > 0) executar(conexao, "DELETE FROM cardapio WHERE id_cardapio = ?", idCardapio);
            if (idCliente > 0) executar(conexao, "DELETE FROM cliente WHERE idcliente = ?", idCliente);
        }
    }

    private static void executar(Connection conexao, String sql, int id) throws Exception {
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, id);
            comando.executeUpdate();
        }
    }

    private static class ContadorEmail implements ServicoEmail {
        private int quantidade;
        @Override
        public void enviar(EmailCompra email) { quantidade++; }
    }

    private static class EmailComFalha implements ServicoEmail {
        @Override
        public void enviar(EmailCompra email) {
            throw new IllegalStateException("Falha simulada de envio");
        }
    }
}

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Regras locais que não precisam abrir o Swing nem acessar o MySQL. */
public class PedidoRegrasTest {
    public static void main(String[] args) {
        TelaCardapios.ItemCardapio item = new TelaCardapios.ItemCardapio(
                10, "Suco", new BigDecimal("5.00"), true);
        TelaPedidos.ItemPedido compra = new TelaPedidos.ItemPedido(item, 2);
        conferir(new BigDecimal("10.00"), compra.subtotal());

        List<TelaPedidos.ItemPedido> itens = new ArrayList<TelaPedidos.ItemPedido>();
        itens.add(compra);
        TelaPedidos.Pedido pedido = new TelaPedidos.Pedido(
                1, LocalDateTime.now(), itens, true, 4, "Ana",
                false, false, false, "não configurado", 1,
                new BigDecimal("20.00"));
        conferir(new BigDecimal("10.00"), pedido.total());
        if (pedido.isPago()) throw new AssertionError("Pedido deveria estar não pago.");
        if (pedido.isEmailEnviado()) throw new AssertionError("E-mail deveria estar pendente.");
        if (!pedido.resumoItensEmLinhas().contains("2x Suco")) {
            throw new AssertionError("Resumo dos itens incorreto.");
        }

        rejeitar(() -> new TelaPedidos.ItemPedido(item, 0));
        rejeitar(() -> new TelaPedidos.ItemPedido(item, 100));
        System.out.println("PedidoRegrasTest: OK");
    }

    private static void conferir(BigDecimal esperado, BigDecimal recebido) {
        if (esperado.compareTo(recebido) != 0) {
            throw new AssertionError("Esperado " + esperado + ", recebido " + recebido);
        }
    }

    private static void rejeitar(Runnable acao) {
        try {
            acao.run();
        } catch (IllegalArgumentException esperado) {
            return;
        }
        throw new AssertionError("Regra inválida foi aceita.");
    }
}

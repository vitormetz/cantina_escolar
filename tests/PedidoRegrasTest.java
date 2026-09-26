import br.com.time7.cantina.model.Cliente;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Confere as regras críticas de pedido sem abrir as janelas Swing. */
public class PedidoRegrasTest {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        List<Cliente> clientes = (List<Cliente>) campo(TelaClientes.class, "CLIENTES").get(null);
        List<Object> cardapios = (List<Object>) campo(TelaCardapios.class, "CARDAPIOS").get(null);
        List<Object> pedidos = (List<Object>) campo(TelaPedidos.class, "PEDIDOS").get(null);
        clientes.clear();
        cardapios.clear();
        pedidos.clear();

        Cliente cliente = cliente(1, "Ana", "20.00");
        clientes.add(cliente);
        TelaCardapios.ItemCardapio item = new TelaCardapios.ItemCardapio(
                "Suco", new BigDecimal("5.00"), true);
        cardapios.add(novoCardapio(item));
        List<TelaPedidos.ItemPedido> carrinho = new ArrayList<TelaPedidos.ItemPedido>();
        carrinho.add(new TelaPedidos.ItemPedido(item, 1));

        TelaPedidos.Pedido primeiro = TelaPedidos.registrarNovoPedido(1, carrinho, false);
        conferir("15.00", TelaClientes.buscarClientePorId(1).getSaldo());

        // Renomear mantém o mesmo ID e não libera um segundo pedido ativo.
        clientes.set(0, cliente(1, "Ana Maria", "15.00"));
        rejeitar(() -> TelaPedidos.registrarNovoPedido(1, carrinho, false), "pedido ativo");
        conferir("15.00", TelaClientes.buscarClientePorId(1).getSaldo());

        Field retirado = campo(primeiro.getClass(), "retirado");
        retirado.setBoolean(primeiro, true);
        cardapios.clear();
        // Um item removido ou esgotado depois de entrar no carrinho não é cobrado.
        rejeitar(() -> TelaPedidos.registrarNovoPedido(1, carrinho, false), "indisponível");
        conferir("15.00", TelaClientes.buscarClientePorId(1).getSaldo());

        System.out.println("PedidoRegrasTest: OK");
    }

    private static Cliente cliente(int id, String nome, String saldo) {
        return new Cliente(id, nome, "Responsável", new BigDecimal(saldo),
                BigDecimal.ZERO, "responsavel@teste.com", "");
    }

    private static Object novoCardapio(TelaCardapios.ItemCardapio item) throws Exception {
        Class<?> tipo = Class.forName("TelaCardapios$Cardapio");
        Constructor<?> construtor = tipo.getDeclaredConstructor(
                int.class, int.class, boolean.class, List.class);
        construtor.setAccessible(true);
        List<TelaCardapios.ItemCardapio> itens = new ArrayList<TelaCardapios.ItemCardapio>();
        itens.add(item);
        return construtor.newInstance(1, 1, true, itens);
    }

    private static Field campo(Class<?> tipo, String nome) throws Exception {
        Field campo = tipo.getDeclaredField(nome);
        campo.setAccessible(true);
        return campo;
    }

    private static void conferir(String esperado, BigDecimal recebido) {
        if (!new BigDecimal(esperado).equals(recebido)) {
            throw new AssertionError("Esperado " + esperado + ", recebido " + recebido);
        }
    }

    private static void rejeitar(Runnable acao, String trechoEsperado) {
        try {
            acao.run();
        } catch (IllegalArgumentException erro) {
            if (erro.getMessage().contains(trechoEsperado)) return;
            throw new AssertionError("Mensagem inesperada: " + erro.getMessage());
        }
        throw new AssertionError("Pedido inválido foi aceito.");
    }
}

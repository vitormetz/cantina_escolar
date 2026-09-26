import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Testes do contrato JSON executáveis sem bibliotecas externas. */
public class CardapioJsonTest {
    public static void main(String[] args) {
        List<TelaCardapios.ItemCardapio> itens = CardapioJson.ler(
                "{\"itens\":["
                + "{\"disponivel\":true,\"preco\":5.50,\"nome\":\"Suco\"},"
                + "{\"nome\":\"Pão \\\"especial\\\"\\n\",\"preco\":8,\"disponivel\":false}]}" );
        conferir(2, itens.size());
        conferir("Suco", itens.get(0).getNome());
        conferir(new BigDecimal("5.50"), itens.get(0).getPreco());
        conferir("Pão \"especial\"\n", itens.get(1).getNome());

        String gerado = CardapioJson.gerar(itens);
        List<TelaCardapios.ItemCardapio> recarregados = CardapioJson.ler(gerado);
        conferir(2, recarregados.size());
        conferir(itens.get(1).getNome(), recarregados.get(1).getNome());

        List<TelaCardapios.ItemCardapio> unicode = CardapioJson.ler(
                "{\"itens\":[{\"nome\":\"Café \\u2615\",\"preco\":1.00,\"disponivel\":true}]}" );
        conferir("Café ☕", unicode.get(0).getNome());

        String[] invalidos = {
                "texto {\"nome\":\"Suco\",\"preco\":5,\"disponivel\":true}",
                "{\"itens\":[{\"nome\":\"\",\"preco\":5,\"disponivel\":true}]}",
                "{\"itens\":[{\"nome\":\"Suco\",\"preco\":0,\"disponivel\":true}]}",
                "{\"itens\":[{\"nome\":\"Suco\",\"preco\":1.001,\"disponivel\":true}]}",
                "{\"itens\":[{\"nome\":\"Suco\",\"preco\":5}]}",
                "{\"itens\":[{\"nome\":\"Suco\",\"preco\":5,\"disponivel\":true}],\"extra\":1}",
                "{\"itens\":[{\"nome\":\"Suco\",\"preco\":5,\"disponivel\":true}]} lixo"
        };
        for (String invalido : invalidos) rejeitar(invalido);

        // A leitura inválida não modifica a lista que já existia na tela.
        List<TelaCardapios.ItemCardapio> atuais = new ArrayList<TelaCardapios.ItemCardapio>(itens);
        try {
            List<TelaCardapios.ItemCardapio> novos = CardapioJson.ler(invalidos[0]);
            atuais.clear();
            atuais.addAll(novos);
        } catch (IllegalArgumentException esperado) {
            // O estado só seria substituído depois de uma leitura completa.
        }
        conferir(2, atuais.size());
        System.out.println("CardapioJsonTest: OK");
    }

    private static void rejeitar(String json) {
        try {
            CardapioJson.ler(json);
        } catch (IllegalArgumentException esperado) {
            return;
        }
        throw new AssertionError("JSON inválido aceito: " + json);
    }

    private static void conferir(Object esperado, Object recebido) {
        if (!esperado.equals(recebido)) {
            throw new AssertionError("Esperado " + esperado + ", recebido " + recebido);
        }
    }
}

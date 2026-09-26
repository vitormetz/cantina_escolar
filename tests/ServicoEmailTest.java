import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Simula o envio e garante que nenhum servidor real seja necessário. */
public class ServicoEmailTest {
    public static void main(String[] args) throws Exception {
        EmailCompra email = new EmailCompra(15, "responsavel@teste.com", "Ana",
                "- 2x Suco — R$ 5,00 cada — subtotal R$ 10,00",
                new BigDecimal("10.00"), new BigDecimal("25.00"),
                LocalDateTime.of(2026, 9, 26, 6, 30), true);
        Simulador simulador = new Simulador();
        simulador.enviar(email);
        if (simulador.quantidade != 1) throw new AssertionError("Envio deveria ocorrer uma vez.");
        if (!simulador.ultima.mensagem().contains("R$ 10,00")) {
            throw new AssertionError("Total não apareceu na mensagem.");
        }
        if (!simulador.ultima.mensagem().contains("Saldo restante: R$ 25,00")
                || !simulador.ultima.mensagem().contains("26/09/2026 06:30")) {
            throw new AssertionError("Saldo ou data não apareceram na mensagem.");
        }
        System.out.println("ServicoEmailTest: OK");
    }

    private static class Simulador implements ServicoEmail {
        private int quantidade;
        private EmailCompra ultima;

        @Override
        public void enviar(EmailCompra email) {
            quantidade++;
            ultima = email;
        }
    }
}

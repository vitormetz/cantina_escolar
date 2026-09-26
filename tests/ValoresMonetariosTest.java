import br.com.time7.cantina.model.Cliente;
import br.com.time7.cantina.util.ValoresMonetarios;

import java.math.BigDecimal;

/** Testes executáveis com java, sem bibliotecas externas ou banco. */
public class ValoresMonetariosTest {
    public static void main(String[] args) {
        conferir("10.50", ValoresMonetarios.converter("10,50", "Saldo"));
        conferir("10.50", ValoresMonetarios.converter("10.50", "Saldo"));
        conferir("1234.56", ValoresMonetarios.converter("1.234,56", "Saldo"));
        conferir("1234567.89", ValoresMonetarios.converter("1.234.567,89", "Saldo"));
        conferir("10.50", ValoresMonetarios.converter(" 10,5 ", "Saldo"));
        conferir("0.00", ValoresMonetarios.converter("", "Saldo"));
        conferir("0.00", ValoresMonetarios.converter("   ", "Saldo"));
        conferir("99999999.99", ValoresMonetarios.converter("99.999.999,99", "Saldo"));
        conferir("10.50", ValoresMonetarios.validar(new BigDecimal("10.5000"), "Saldo"));

        String[] invalidos = { "-1", "-0.00", "+1", "1e2", "1E+2", "1.234",
                "10,500", "1,234.56", "12.34,56", "1..234,56", "1,", ".50",
                "R$ 10,50", "NaN", "100000000", "99.999.999,999", "1 000,00", null };
        for (String invalido : invalidos) {
            rejeitar(() -> ValoresMonetarios.converter(invalido, "Saldo"));
        }
        rejeitar(() -> ValoresMonetarios.validar(null, "Saldo"));
        rejeitar(() -> ValoresMonetarios.validar(new BigDecimal("-0.01"), "Saldo"));
        rejeitar(() -> ValoresMonetarios.validar(new BigDecimal("0.001"), "Saldo"));
        rejeitar(() -> ValoresMonetarios.validar(new BigDecimal("100000000.00"), "Saldo"));

        Cliente cliente = new Cliente(1, "Teste", "Responsável", BigDecimal.TEN,
                BigDecimal.ZERO, "", "");
        conferir("10.00", cliente.getSaldo());
        conferir("0.00", cliente.getLimiteSaldo());
        rejeitar(() -> new Cliente(1, "Teste", "", new BigDecimal("100000000"),
                BigDecimal.ZERO, "", ""));
        rejeitar(() -> new Cliente(1, "Teste", "", BigDecimal.ZERO,
                new BigDecimal("-1"), "", ""));
        System.out.println("ValoresMonetariosTest: OK");
    }

    private static void conferir(String esperado, BigDecimal valor) {
        if (!new BigDecimal(esperado).equals(valor)) {
            throw new AssertionError("Esperado " + esperado + ", recebido " + valor);
        }
    }

    private static void rejeitar(Runnable acao) {
        try {
            acao.run();
        } catch (IllegalArgumentException esperado) {
            return;
        }
        throw new AssertionError("Um valor monetário inválido foi aceito.");
    }
}

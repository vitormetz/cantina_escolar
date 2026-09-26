import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Dados mínimos usados para avisar o responsável sobre uma compra. */
public final class EmailCompra {
    private final int idPedido;
    private final String destinatario;
    private final String cliente;
    private final String itens;
    private final BigDecimal total;
    private final BigDecimal saldoRestante;
    private final LocalDateTime dataPedido;
    private final boolean pago;

    public EmailCompra(String destinatario, String cliente, String itens,
            BigDecimal total, boolean pago) {
        this(0, destinatario, cliente, itens, total, BigDecimal.ZERO,
                LocalDateTime.now(), pago);
    }

    public EmailCompra(int idPedido, String destinatario, String cliente, String itens,
            BigDecimal total, BigDecimal saldoRestante, LocalDateTime dataPedido,
            boolean pago) {
        this.idPedido = idPedido;
        this.destinatario = destinatario;
        this.cliente = cliente;
        this.itens = itens;
        this.total = total;
        this.saldoRestante = saldoRestante;
        this.dataPedido = dataPedido;
        this.pago = pago;
    }

    public int getIdPedido() { return idPedido; }
    public String getDestinatario() { return destinatario; }
    public String getCliente() { return cliente; }
    public String getItens() { return itens; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getSaldoRestante() { return saldoRestante; }
    public LocalDateTime getDataPedido() { return dataPedido; }
    public boolean isPago() { return pago; }

    public String assunto() {
        return "Compra registrada na Cantina Escolar";
    }

    public String mensagem() {
        return "Olá!\n\nFoi registrada uma compra para " + cliente + ".\n\n"
                + "Itens:\n" + itens + "\n\nTotal: R$ "
                + total.toPlainString().replace('.', ',')
                + "\nSaldo restante: R$ "
                + saldoRestante.toPlainString().replace('.', ',')
                + "\nPagamento: " + (pago ? "pago" : "pendente")
                + "\nData/hora: "
                + dataPedido.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                + "\n\nCantina Escolar - Time 7";
    }
}

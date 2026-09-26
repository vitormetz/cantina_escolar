/** Permite usar o envio real na aplicação e uma simulação nos testes. */
public interface ServicoEmail {
    void enviar(EmailCompra email) throws Exception;
}

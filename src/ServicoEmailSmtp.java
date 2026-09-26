import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/** Envia o aviso por SMTP sem manter usuário ou senha no código. */
public final class ServicoEmailSmtp implements ServicoEmail {
    @Override
    public void enviar(EmailCompra email) throws Exception {
        String host = obrigatoria("CANTINA_EMAIL_SMTP_HOST");
        String porta = valor("CANTINA_EMAIL_SMTP_PORT", "587");
        String usuario = obrigatoria("CANTINA_EMAIL_SMTP_USER");
        String senha = obrigatoriaSemTrim("CANTINA_EMAIL_SMTP_PASSWORD");
        String remetente = valor("CANTINA_EMAIL_FROM", usuario);
        String tls = valor("CANTINA_EMAIL_SMTP_TLS", "true");

        Properties propriedades = new Properties();
        propriedades.put("mail.smtp.host", host);
        propriedades.put("mail.smtp.port", porta);
        propriedades.put("mail.smtp.auth", "true");
        propriedades.put("mail.smtp.starttls.enable", tls);
        propriedades.put("mail.smtp.connectiontimeout", "8000");
        propriedades.put("mail.smtp.timeout", "10000");
        propriedades.put("mail.smtp.writetimeout", "10000");

        Session sessao = Session.getInstance(propriedades, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuario, senha);
            }
        });

        MimeMessage mensagem = new MimeMessage(sessao);
        mensagem.setFrom(new InternetAddress(remetente));
        mensagem.setRecipient(Message.RecipientType.TO,
                new InternetAddress(email.getDestinatario()));
        mensagem.setSubject(email.assunto(), "UTF-8");
        mensagem.setText(email.mensagem(), "UTF-8");
        mensagem.setHeader("X-Cantina-Pedido", String.valueOf(email.getIdPedido()));
        Transport.send(mensagem);
    }

    private static String obrigatoria(String nome) {
        String resultado = System.getenv(nome);
        if (resultado == null || resultado.trim().isEmpty()) {
            throw new IllegalStateException("Configure a variável " + nome + ".");
        }
        return resultado.trim();
    }

    private static String obrigatoriaSemTrim(String nome) {
        String resultado = System.getenv(nome);
        if (resultado == null || resultado.isEmpty()) {
            throw new IllegalStateException("Configure a variável " + nome + ".");
        }
        return resultado;
    }

    private static String valor(String nome, String padrao) {
        String resultado = System.getenv(nome);
        return resultado == null || resultado.trim().isEmpty() ? padrao : resultado.trim();
    }
}

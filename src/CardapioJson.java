import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Leitor do contrato {"itens":[{"nome":...,"preco":...,"disponivel":...}]}.
 * Valida o documento inteiro antes de devolver a lista: nenhum item é ignorado.
 * Não precisa de bibliotecas externas e funciona no Java 8.
 */
public final class CardapioJson {
    private static final BigDecimal PRECO_MAXIMO = new BigDecimal("99999999.99");
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private CardapioJson() { }

    public static List<TelaCardapios.ItemCardapio> ler(String json) {
        if (json == null) {
            throw new IllegalArgumentException("Informe o JSON do cardápio.");
        }
        return new Leitor(json).lerCardapio();
    }

    public static String gerar(List<TelaCardapios.ItemCardapio> itens) {
        StringBuilder json = new StringBuilder("{\"itens\":[");
        for (int i = 0; i < itens.size(); i++) {
            if (i > 0) json.append(',');
            TelaCardapios.ItemCardapio item = itens.get(i);
            json.append("{\"nome\":\"").append(escapar(item.getNome()))
                    .append("\",\"preco\":").append(item.getPreco().toPlainString())
                    .append(",\"disponivel\":").append(item.isDisponivel()).append('}');
        }
        return json.append("]}").toString();
    }

    /** Escapa o conteúdo de uma string; as aspas externas ficam com o chamador. */
    public static String escapar(String texto) {
        if (texto == null) throw new IllegalArgumentException("Texto não pode ser nulo.");
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char caractere = texto.charAt(i);
            switch (caractere) {
                case '"': resultado.append("\\\""); break;
                case '\\': resultado.append("\\\\"); break;
                case '\b': resultado.append("\\b"); break;
                case '\f': resultado.append("\\f"); break;
                case '\n': resultado.append("\\n"); break;
                case '\r': resultado.append("\\r"); break;
                case '\t': resultado.append("\\t"); break;
                default:
                    if (caractere < 0x20 || Character.isSurrogate(caractere)) {
                        resultado.append("\\u")
                                .append(HEX[(caractere >> 12) & 15])
                                .append(HEX[(caractere >> 8) & 15])
                                .append(HEX[(caractere >> 4) & 15])
                                .append(HEX[caractere & 15]);
                    } else {
                        resultado.append(caractere);
                    }
            }
        }
        return resultado.toString();
    }

    private static final class Leitor {
        private final String texto;
        private int posicao;

        private Leitor(String texto) { this.texto = texto; }

        private List<TelaCardapios.ItemCardapio> lerCardapio() {
            exigir('{');
            if (!"itens".equals(lerString())) {
                throw erro("A propriedade principal deve ser 'itens'.");
            }
            exigir(':');
            exigir('[');
            List<TelaCardapios.ItemCardapio> itens = new ArrayList<TelaCardapios.ItemCardapio>();
            if (!consumir(']')) {
                do {
                    itens.add(lerItem());
                } while (consumir(','));
                exigir(']');
            }
            exigir('}');
            pularEspacos();
            if (posicao != texto.length()) {
                throw erro("Há conteúdo depois do fim do JSON.");
            }
            return itens;
        }

        private TelaCardapios.ItemCardapio lerItem() {
            exigir('{');
            String nome = null;
            BigDecimal preco = null;
            Boolean disponivel = null;
            if (!consumir('}')) {
                do {
                    String propriedade = lerString();
                    exigir(':');
                    switch (propriedade) {
                        case "nome":
                            if (nome != null) throw erro("Propriedade 'nome' repetida.");
                            nome = lerString();
                            if (nome.trim().isEmpty()) throw erro("Todo item precisa de um nome.");
                            break;
                        case "preco":
                            if (preco != null) throw erro("Propriedade 'preco' repetida.");
                            preco = lerPreco();
                            break;
                        case "disponivel":
                            if (disponivel != null) throw erro("Propriedade 'disponivel' repetida.");
                            disponivel = lerBooleano();
                            break;
                        default:
                            throw erro("Propriedade desconhecida: " + propriedade + ".");
                    }
                } while (consumir(','));
                exigir('}');
            }
            if (nome == null || preco == null || disponivel == null) {
                throw erro("Cada item precisa de nome, preco e disponivel.");
            }
            return new TelaCardapios.ItemCardapio(nome, preco, disponivel.booleanValue());
        }

        private Boolean lerBooleano() {
            pularEspacos();
            if (texto.startsWith("true", posicao)) {
                posicao += 4;
                return Boolean.TRUE;
            }
            if (texto.startsWith("false", posicao)) {
                posicao += 5;
                return Boolean.FALSE;
            }
            throw erro("Disponibilidade deve ser true ou false, sem aspas.");
        }

        private BigDecimal lerPreco() {
            pularEspacos();
            int inicio = posicao;
            if (atual() == '-') posicao++;
            if (atual() == '0') {
                posicao++;
            } else {
                exigirDigitos();
            }
            if (atual() == '.') {
                posicao++;
                exigirDigitos();
            }
            if (atual() == 'e' || atual() == 'E') {
                posicao++;
                if (atual() == '+' || atual() == '-') posicao++;
                exigirDigitos();
            }
            // Limita a representação antes de construir números desnecessariamente grandes.
            if (posicao - inicio > 64) throw erro("Preço fora do intervalo permitido.");
            try {
                BigDecimal preco = new BigDecimal(texto.substring(inicio, posicao));
                if (preco.signum() <= 0 || preco.compareTo(PRECO_MAXIMO) > 0) {
                    throw erro("Preço deve estar entre 0.01 e 99999999.99.");
                }
                // Examina a escala antes de setScale para rejeitar frações de centavo sem arredondar.
                if (preco.stripTrailingZeros().scale() > 2) {
                    throw erro("Preço deve ter no máximo duas casas decimais.");
                }
                return preco.setScale(2, RoundingMode.UNNECESSARY);
            } catch (NumberFormatException | ArithmeticException excecao) {
                throw erro("Preço inválido; use um número JSON como 8.50.");
            }
        }

        private void exigirDigitos() {
            int inicio = posicao;
            while (atual() >= '0' && atual() <= '9') posicao++;
            if (inicio == posicao) throw erro("Esperado um número JSON válido.");
        }

        private String lerString() {
            exigir('"');
            StringBuilder valor = new StringBuilder();
            while (posicao < texto.length()) {
                char caractere = texto.charAt(posicao++);
                if (caractere == '"') return valor.toString();
                if (caractere < 0x20) throw erro("Caracteres de controle devem ser escapados.");
                if (caractere != '\\') {
                    valor.append(caractere);
                    continue;
                }
                if (posicao == texto.length()) throw erro("Escape incompleto.");
                char escape = texto.charAt(posicao++);
                switch (escape) {
                    case '"': valor.append('"'); break;
                    case '\\': valor.append('\\'); break;
                    case '/': valor.append('/'); break;
                    case 'b': valor.append('\b'); break;
                    case 'f': valor.append('\f'); break;
                    case 'n': valor.append('\n'); break;
                    case 'r': valor.append('\r'); break;
                    case 't': valor.append('\t'); break;
                    case 'u':
                        int unicode = 0;
                        for (int i = 0; i < 4; i++) {
                            if (posicao == texto.length()) throw erro("Escape Unicode incompleto.");
                            char digito = texto.charAt(posicao++);
                            int numero = "0123456789abcdef".indexOf(Character.toLowerCase(digito));
                            if (numero < 0) throw erro("Escape Unicode inválido.");
                            unicode = unicode * 16 + numero;
                        }
                        valor.append((char) unicode);
                        break;
                    default: throw erro("Escape JSON inválido.");
                }
            }
            throw erro("Texto sem aspas de fechamento.");
        }

        private char atual() { return posicao < texto.length() ? texto.charAt(posicao) : '\0'; }

        private boolean consumir(char esperado) {
            pularEspacos();
            if (atual() != esperado) return false;
            posicao++;
            return true;
        }

        private void exigir(char esperado) {
            if (!consumir(esperado)) throw erro("Esperado '" + esperado + "'.");
        }

        private void pularEspacos() {
            while (posicao < texto.length()) {
                char caractere = texto.charAt(posicao);
                if (caractere != ' ' && caractere != '\t' && caractere != '\r' && caractere != '\n') break;
                posicao++;
            }
        }

        private IllegalArgumentException erro(String mensagem) {
            return new IllegalArgumentException(mensagem + " Posição: " + (posicao + 1) + ".");
        }
    }
}

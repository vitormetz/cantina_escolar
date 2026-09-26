import br.com.time7.cantina.model.Cliente;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Compra, pagamento, preparação e histórico dos pedidos persistidos. */
public class TelaPedidos extends JFrame {
    private final List<ItemPedido> itensDoNovoPedido = new ArrayList<ItemPedido>();
    private final List<Pedido> pedidosVisiveis = new ArrayList<Pedido>();
    private final NumberFormat formatoMoeda = NumberFormat.getCurrencyInstance(
            new Locale.Builder().setLanguage("pt").setRegion("BR").build());
    private final DateTimeFormatter formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private JComboBox<Cliente> seletorCliente;
    private JLabel rotuloSaldoCliente;
    private JLabel rotuloDia;
    private JCheckBox campoAntecipado;
    private JCheckBox campoPago;
    private JComboBox<TelaCardapios.ItemCardapio> seletorItem;
    private JSpinner campoQuantidade;
    private JTable tabelaNovoPedido;
    private DefaultTableModel modeloNovoPedido;
    private JLabel rotuloTotal;
    private JTable tabelaPedidos;
    private DefaultTableModel modeloPedidos;

    public TelaPedidos() {
        setTitle("Cantina Escolar - Pedidos");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1240, 760);
        setMinimumSize(new java.awt.Dimension(1020, 650));
        setLocationRelativeTo(null);
        montarInterface();
        atualizarClientes();
        atualizarItensDisponiveis();
        atualizarHub();
        configurarAtalhos();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent evento) {
                atualizarTudo();
            }
        });
    }

    private void montarInterface() {
        JPanel principal = new JPanel(new BorderLayout(14, 14));
        principal.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        principal.setBackground(new Color(245, 247, 250));
        setContentPane(principal);

        JLabel titulo = new JLabel("Compras, preparação e retiradas");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 25));
        titulo.setForeground(new Color(32, 45, 64));
        principal.add(titulo, BorderLayout.NORTH);

        JSplitPane divisao = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, criarNovoPedido(), criarHubPedidos());
        divisao.setResizeWeight(0.48);
        divisao.setBorder(null);
        principal.add(divisao, BorderLayout.CENTER);
    }

    private JPanel criarNovoPedido() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createTitledBorder("Registrar compra ou pedido"));

        JPanel cliente = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        seletorCliente = new JComboBox<Cliente>();
        seletorCliente.setPrototypeDisplayValue(new Cliente(
                0, "Nome de cliente relativamente longo", "",
                BigDecimal.ZERO, BigDecimal.ZERO, "", ""));
        rotuloSaldoCliente = new JLabel("Saldo: R$ 0,00");
        rotuloDia = new JLabel();
        campoAntecipado = new JCheckBox("Pedido antecipado");
        campoPago = new JCheckBox("Pago", true);
        campoPago.setEnabled(false);
        cliente.add(new JLabel("Cliente *"));
        cliente.add(seletorCliente);
        cliente.add(rotuloSaldoCliente);
        cliente.add(rotuloDia);
        cliente.add(campoAntecipado);
        cliente.add(campoPago);

        seletorCliente.addActionListener(evento -> atualizarSaldoClienteSelecionado());
        campoAntecipado.addActionListener(evento -> atualizarRegraPagamento());
        painel.add(cliente, BorderLayout.NORTH);

        JPanel itens = new JPanel(new BorderLayout(6, 6));
        JPanel escolha = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        seletorItem = new JComboBox<TelaCardapios.ItemCardapio>();
        seletorItem.setPrototypeDisplayValue(new TelaCardapios.ItemCardapio(
                "Nome de um item relativamente longo", new BigDecimal("99.99"), true));
        campoQuantidade = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
        JButton atualizarCardapio = new JButton("Atualizar cardápio");
        JButton adicionarItem = new JButton("Adicionar ao pedido");
        adicionarItem.setMnemonic(KeyEvent.VK_A);
        atualizarCardapio.setMnemonic(KeyEvent.VK_T);
        atualizarCardapio.addActionListener(evento -> atualizarItensDisponiveis());
        adicionarItem.addActionListener(evento -> adicionarItemAoPedido());
        escolha.add(new JLabel("Item disponível"));
        escolha.add(seletorItem);
        escolha.add(new JLabel("Quantidade"));
        escolha.add(campoQuantidade);
        escolha.add(adicionarItem);
        escolha.add(atualizarCardapio);
        itens.add(escolha, BorderLayout.NORTH);

        modeloNovoPedido = new DefaultTableModel(
                new String[] {"Item", "Preço unitário", "Quantidade", "Subtotal"}, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) { return false; }
        };
        tabelaNovoPedido = new JTable(modeloNovoPedido);
        tabelaNovoPedido.setRowHeight(25);
        tabelaNovoPedido.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itens.add(new JScrollPane(tabelaNovoPedido), BorderLayout.CENTER);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 3));
        rotuloTotal = new JLabel("Total: R$ 0,00");
        JButton removerItem = new JButton("Remover item");
        JButton limpar = new JButton("Limpar pedido");
        JButton registrar = new JButton("Registrar");
        removerItem.addActionListener(evento -> removerItemDoPedido());
        limpar.addActionListener(evento -> limparNovoPedido());
        registrar.addActionListener(evento -> registrarPedido());
        botoes.add(rotuloTotal);
        botoes.add(removerItem);
        botoes.add(limpar);
        botoes.add(registrar);
        getRootPane().setDefaultButton(registrar);
        itens.add(botoes, BorderLayout.SOUTH);
        painel.add(itens, BorderLayout.CENTER);
        return painel;
    }

    private JPanel criarHubPedidos() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createTitledBorder(
                "Pedidos ativos e histórico de retiradas"));

        modeloPedidos = new DefaultTableModel(new String[] {
                "Pedido", "Data", "Cliente", "Itens", "Total",
                "Antecipado", "Pago", "Retirado", "E-mail"
        }, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) { return false; }
        };
        tabelaPedidos = new JTable(modeloPedidos);
        tabelaPedidos.setRowHeight(28);
        tabelaPedidos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        painel.add(new JScrollPane(tabelaPedidos), BorderLayout.CENTER);

        JButton atualizar = new JButton("Atualizar");
        JButton reenviar = new JButton("Reenviar e-mail");
        JButton pagar = new JButton("Confirmar pagamento");
        JButton concluir = new JButton("Confirmar retirada");
        atualizar.addActionListener(evento -> atualizarTudo());
        reenviar.addActionListener(evento -> reenviarEmail());
        pagar.addActionListener(evento -> confirmarPagamento());
        concluir.addActionListener(evento -> concluirPedido());
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botoes.add(atualizar);
        botoes.add(reenviar);
        botoes.add(pagar);
        botoes.add(concluir);
        painel.add(botoes, BorderLayout.SOUTH);
        return painel;
    }

    private int diaDoPedido() {
        return LocalDate.now().getDayOfWeek().getValue();
    }

    private void atualizarTudo() {
        try {
            atualizarClientes();
            atualizarItensDisponiveis();
            atualizarHub();
        } catch (RuntimeException erro) {
            avisar(erro.getMessage());
        }
    }

    private void atualizarItensDisponiveis() {
        Object selecionado = seletorItem.getSelectedItem();
        seletorItem.removeAllItems();
        int dia = diaDoPedido();
        rotuloDia.setText("Cardápio: " + nomeDia(dia));
        for (TelaCardapios.ItemCardapio item : TelaCardapios.listarItensDisponiveis(dia)) {
            seletorItem.addItem(item);
        }
        seletorItem.setSelectedItem(selecionado);
    }

    private void atualizarClientes() {
        Cliente selecionado = (Cliente) seletorCliente.getSelectedItem();
        int idSelecionado = selecionado == null ? 0 : selecionado.getIdCliente();
        seletorCliente.removeAllItems();
        for (Cliente cliente : TelaClientes.listarClientes()) {
            seletorCliente.addItem(cliente);
            if (cliente.getIdCliente() == idSelecionado) seletorCliente.setSelectedItem(cliente);
        }
        atualizarSaldoClienteSelecionado();
    }

    private void atualizarSaldoClienteSelecionado() {
        Cliente cliente = (Cliente) seletorCliente.getSelectedItem();
        rotuloSaldoCliente.setText(cliente == null
                ? "Saldo: nenhum cliente selecionado"
                : "Saldo: " + formatoMoeda.format(cliente.getSaldo()));
    }

    private void atualizarRegraPagamento() {
        if (campoAntecipado.isSelected()) {
            campoPago.setEnabled(true);
            campoPago.setSelected(false);
        } else {
            campoPago.setSelected(true);
            campoPago.setEnabled(false);
        }
    }

    private void adicionarItemAoPedido() {
        TelaCardapios.ItemCardapio item =
                (TelaCardapios.ItemCardapio) seletorItem.getSelectedItem();
        if (item == null) {
            avisar("Não há itens disponíveis para o dia atual.");
            return;
        }
        try {
            campoQuantidade.commitEdit();
            int quantidade = ((Number) campoQuantidade.getValue()).intValue();
            itensDoNovoPedido.add(new ItemPedido(item, quantidade));
            atualizarTabelaNovoPedido();
        } catch (ParseException | IllegalArgumentException erro) {
            avisar("Informe uma quantidade inteira entre 1 e 99.");
        }
    }

    private void removerItemDoPedido() {
        int linha = tabelaNovoPedido.getSelectedRow();
        if (linha < 0) {
            avisar("Selecione um item do pedido para remover.");
            return;
        }
        itensDoNovoPedido.remove(linha);
        atualizarTabelaNovoPedido();
    }

    private void registrarPedido() {
        Cliente cliente = (Cliente) seletorCliente.getSelectedItem();
        if (cliente == null) {
            avisar("Cadastre e selecione um cliente antes de registrar o pedido.");
            return;
        }
        try {
            Pedido pedido = BancoAplicacao.registrarPedido(cliente.getIdCliente(),
                    itensDoNovoPedido, campoAntecipado.isSelected(),
                    campoPago.isSelected(), diaDoPedido());
            limparNovoPedido();
            atualizarTudo();
            if (pedido.isEmailEnviado()) {
                JOptionPane.showMessageDialog(this,
                        "Pedido registrado e e-mail enviado ao responsável.");
            } else {
                JOptionPane.showMessageDialog(this,
                        "Pedido registrado, mas o e-mail não foi enviado.\n"
                        + pedido.getEmailErro(),
                        "Compra salva com aviso", JOptionPane.WARNING_MESSAGE);
            }
        } catch (RuntimeException erro) {
            atualizarTudo();
            avisar(erro.getMessage());
        }
    }

    private Pedido pedidoSelecionado() {
        int linha = tabelaPedidos.getSelectedRow();
        if (linha < 0) {
            avisar("Selecione um pedido.");
            return null;
        }
        return pedidosVisiveis.get(tabelaPedidos.convertRowIndexToModel(linha));
    }

    private void confirmarPagamento() {
        Pedido pedido = pedidoSelecionado();
        if (pedido == null) return;
        try {
            BancoAplicacao.marcarPago(pedido.getIdPedido());
            atualizarTudo();
            JOptionPane.showMessageDialog(this, "Pagamento confirmado.");
        } catch (RuntimeException erro) {
            avisar(erro.getMessage());
        }
    }

    private void concluirPedido() {
        Pedido pedido = pedidoSelecionado();
        if (pedido == null) return;
        int resposta = JOptionPane.showConfirmDialog(this,
                "Confirmar retirada do pedido #" + pedido.getIdPedido() + "?",
                "Concluir pedido", JOptionPane.YES_NO_OPTION);
        if (resposta != JOptionPane.YES_OPTION) return;
        try {
            BancoAplicacao.marcarRetirado(pedido.getIdPedido());
            atualizarTudo();
        } catch (RuntimeException erro) {
            avisar(erro.getMessage());
        }
    }

    private void reenviarEmail() {
        Pedido pedido = pedidoSelecionado();
        if (pedido == null) return;
        if (pedido.isEmailEnviado()) {
            avisar("O e-mail deste pedido já foi enviado.");
            return;
        }
        try {
            BancoAplicacao.reenviarEmail(pedido.getIdPedido());
            atualizarTudo();
            JOptionPane.showMessageDialog(this, "E-mail enviado ao responsável.");
        } catch (RuntimeException erro) {
            atualizarTudo();
            avisar(erro.getMessage());
        }
    }

    private void atualizarTabelaNovoPedido() {
        modeloNovoPedido.setRowCount(0);
        for (ItemPedido item : itensDoNovoPedido) {
            modeloNovoPedido.addRow(new Object[] {
                    item.nome, formatoMoeda.format(item.preco), item.quantidade,
                    formatoMoeda.format(item.subtotal())
            });
        }
        rotuloTotal.setText("Total: " + formatoMoeda.format(calcularTotalNovoPedido()));
    }

    private void atualizarHub() {
        int idSelecionado = 0;
        int linha = tabelaPedidos.getSelectedRow();
        if (linha >= 0 && linha < pedidosVisiveis.size()) {
            idSelecionado = pedidosVisiveis.get(
                    tabelaPedidos.convertRowIndexToModel(linha)).getIdPedido();
        }
        modeloPedidos.setRowCount(0);
        pedidosVisiveis.clear();
        pedidosVisiveis.addAll(BancoAplicacao.listarPedidos());
        for (Pedido pedido : pedidosVisiveis) {
            modeloPedidos.addRow(new Object[] {
                    pedido.idPedido, formatoData.format(pedido.dataPedido),
                    pedido.nomeCliente, pedido.resumoItens(),
                    formatoMoeda.format(pedido.total()),
                    pedido.antecipado ? "Sim" : "Não",
                    pedido.pago ? "Sim" : "Não",
                    pedido.retirado ? "Sim" : "Não",
                    pedido.emailEnviado ? "Enviado" : "Pendente"
            });
        }
        for (int i = 0; i < pedidosVisiveis.size(); i++) {
            if (pedidosVisiveis.get(i).getIdPedido() == idSelecionado) {
                tabelaPedidos.setRowSelectionInterval(i, i);
                break;
            }
        }
    }

    private void limparNovoPedido() {
        seletorCliente.setSelectedItem(null);
        campoAntecipado.setSelected(false);
        atualizarRegraPagamento();
        campoQuantidade.setValue(1);
        itensDoNovoPedido.clear();
        atualizarTabelaNovoPedido();
        atualizarSaldoClienteSelecionado();
    }

    private BigDecimal calcularTotalNovoPedido() {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : itensDoNovoPedido) total = total.add(item.subtotal());
        return total;
    }

    private void configurarAtalhos() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "fechar");
        getRootPane().getActionMap().put("fechar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evento) { dispose(); }
        });
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK),
                        "registrarPedido");
        getRootPane().getActionMap().put("registrarPedido", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evento) { registrarPedido(); }
        });
    }

    private void avisar(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    private String nomeDia(int dia) {
        String[] dias = {"Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"};
        return dias[dia - 1];
    }

    public static boolean temPedidosDoCliente(int idCliente) {
        return BancoAplicacao.temPedidosDoCliente(idCliente);
    }

    static class ItemPedido {
        private final TelaCardapios.ItemCardapio origem;
        private final String nome;
        private final BigDecimal preco;
        private final int quantidade;

        ItemPedido(TelaCardapios.ItemCardapio origem, int quantidade) {
            if (origem == null || origem.getPreco().signum() <= 0 || quantidade < 1 || quantidade > 99) {
                throw new IllegalArgumentException("Item ou quantidade inválida.");
            }
            this.origem = origem;
            this.nome = origem.getNome();
            this.preco = origem.getPreco();
            this.quantidade = quantidade;
        }

        ItemPedido(String nome, BigDecimal preco, int quantidade) {
            this.origem = null;
            this.nome = nome;
            this.preco = preco;
            this.quantidade = quantidade;
        }

        TelaCardapios.ItemCardapio getOrigem() { return origem; }
        String getNome() { return nome; }
        BigDecimal getPreco() { return preco; }
        int getQuantidade() { return quantidade; }

        BigDecimal subtotal() {
            return preco.multiply(new BigDecimal(quantidade));
        }
    }

    static class Pedido {
        private final int idPedido;
        private final LocalDateTime dataPedido;
        private final List<ItemPedido> itens;
        private final boolean antecipado;
        private final int idCliente;
        private final String nomeCliente;
        private boolean pago;
        private boolean retirado;
        private boolean emailEnviado;
        private String emailErro;
        private final int diaCardapio;
        private final BigDecimal saldoApos;

        Pedido(int id, LocalDateTime data, List<ItemPedido> itens,
                boolean antecipado, int idCliente, String nomeCliente,
                boolean pago, boolean retirado, boolean emailEnviado,
                String emailErro, int diaCardapio, BigDecimal saldoApos) {
            this.idPedido = id;
            this.dataPedido = data;
            this.itens = itens;
            this.antecipado = antecipado;
            this.idCliente = idCliente;
            this.nomeCliente = nomeCliente;
            this.pago = pago;
            this.retirado = retirado;
            this.emailEnviado = emailEnviado;
            this.emailErro = emailErro;
            this.diaCardapio = diaCardapio;
            this.saldoApos = saldoApos;
        }

        int getIdPedido() { return idPedido; }
        int getIdCliente() { return idCliente; }
        LocalDateTime getDataPedido() { return dataPedido; }
        boolean isPago() { return pago; }
        boolean isEmailEnviado() { return emailEnviado; }
        String getEmailErro() { return emailErro; }
        BigDecimal getSaldoApos() { return saldoApos; }

        void definirResultadoEmail(boolean enviado, String erro) {
            this.emailEnviado = enviado;
            this.emailErro = erro;
        }

        BigDecimal total() {
            BigDecimal total = BigDecimal.ZERO;
            for (ItemPedido item : itens) total = total.add(item.subtotal());
            return total;
        }

        String resumoItens() {
            StringBuilder resumo = new StringBuilder();
            for (int i = 0; i < itens.size(); i++) {
                if (i > 0) resumo.append(", ");
                resumo.append(itens.get(i).quantidade).append("x ").append(itens.get(i).nome);
            }
            return resumo.toString();
        }

        String resumoItensEmLinhas() {
            StringBuilder resumo = new StringBuilder();
            for (ItemPedido item : itens) {
                resumo.append("- ").append(item.quantidade).append("x ")
                        .append(item.nome).append(" — R$ ")
                        .append(item.preco.toPlainString().replace('.', ','))
                        .append(" cada — subtotal R$ ")
                        .append(item.subtotal().toPlainString().replace('.', ',')).append('\n');
            }
            return resumo.toString().trim();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TelaPedidos().setVisible(true));
    }
}

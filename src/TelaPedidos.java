import br.com.time7.cantina.model.Cliente;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JComponent;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Hub dos pedidos ainda não retirados.
 *
 * Os campos seguem a tabela pedido: idpedido, datapedido, itempedido em JSON,
 * antecipado, idcliente e retirado. A regra de apenas um pedido ativo por
 * cliente é verificada antes de cada cadastro.
 */
public class TelaPedidos extends JFrame {
    private static final List<Pedido> PEDIDOS = new ArrayList<Pedido>();
    private static int proximoId = 1;

    private final List<ItemPedido> itensDoNovoPedido = new ArrayList<ItemPedido>();
    private final List<Pedido> pedidosVisiveis = new ArrayList<Pedido>();
    private final NumberFormat formatoMoeda = NumberFormat.getCurrencyInstance(
            new Locale.Builder().setLanguage("pt").setRegion("BR").build());
    private final DateTimeFormatter formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private JComboBox<Cliente> seletorCliente;
    private JLabel rotuloSaldoCliente;
    private JLabel rotuloTotal;
    private JCheckBox campoAntecipado;
    private JComboBox<TelaCardapios.ItemCardapio> seletorItem;
    private JSpinner campoQuantidade;
    private JTable tabelaNovoPedido;
    private DefaultTableModel modeloNovoPedido;
    private JTable tabelaPendentes;
    private DefaultTableModel modeloPendentes;

    public TelaPedidos() {
        setTitle("Cantina Escolar - Pedidos pendentes");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1120, 720);
        setMinimumSize(new java.awt.Dimension(950, 620));
        setLocationRelativeTo(null);
        montarInterface();
        atualizarClientes();
        atualizarItensDisponiveis();
        atualizarHub();
        configurarAtalhos();

        // Recarrega o cardápio quando a pessoa volta para esta janela.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent evento) {
                atualizarClientes();
                atualizarItensDisponiveis();
                atualizarHub();
            }
        });
    }

    private void montarInterface() {
        JPanel principal = new JPanel(new BorderLayout(14, 14));
        principal.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        principal.setBackground(new Color(245, 247, 250));
        setContentPane(principal);

        JLabel titulo = new JLabel("Hub de pedidos para preparar");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 25));
        titulo.setForeground(new Color(32, 45, 64));
        principal.add(titulo, BorderLayout.NORTH);

        JSplitPane divisao = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, criarNovoPedido(), criarHubPendentes());
        divisao.setResizeWeight(0.48);
        divisao.setBorder(null);
        principal.add(divisao, BorderLayout.CENTER);
    }

    private JPanel criarNovoPedido() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createTitledBorder("Registrar novo pedido"));

        JPanel cliente = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        seletorCliente = new JComboBox<Cliente>();
        seletorCliente.setPrototypeDisplayValue(new Cliente(
                0, "Nome de cliente relativamente longo", "",
                BigDecimal.ZERO, BigDecimal.ZERO, "", ""));
        rotuloSaldoCliente = new JLabel("Saldo: R$ 0,00");
        campoAntecipado = new JCheckBox("Pedido antecipado");
        cliente.add(new JLabel("Cliente *"));
        cliente.add(seletorCliente);
        cliente.add(rotuloSaldoCliente);
        cliente.add(campoAntecipado);
        seletorCliente.addActionListener(evento -> atualizarSaldoClienteSelecionado());
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
        rotuloTotal = new JLabel("Total: " + formatoMoeda.format(BigDecimal.ZERO));
        botoes.add(rotuloTotal);
        JButton removerItem = new JButton("Remover item");
        JButton limpar = new JButton("Limpar pedido");
        JButton registrar = new JButton("Registrar pedido");
        removerItem.setMnemonic(KeyEvent.VK_R);
        limpar.setMnemonic(KeyEvent.VK_L);
        registrar.setMnemonic(KeyEvent.VK_P);
        registrar.setToolTipText("Registrar pedido (Alt+P ou Ctrl+Enter)");
        removerItem.addActionListener(evento -> removerItemDoPedido());
        limpar.addActionListener(evento -> limparNovoPedido());
        registrar.addActionListener(evento -> registrarPedido());
        botoes.add(removerItem);
        botoes.add(limpar);
        botoes.add(registrar);
        getRootPane().setDefaultButton(registrar);
        itens.add(botoes, BorderLayout.SOUTH);
        painel.add(itens, BorderLayout.CENTER);
        return painel;
    }

    private JPanel criarHubPendentes() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createTitledBorder(
                "Pedidos não retirados — devem ser preparados"));

        modeloPendentes = new DefaultTableModel(
                new String[] {"Pedido", "Data", "Cliente", "Itens", "Total", "Antecipado"}, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) { return false; }
        };
        tabelaPendentes = new JTable(modeloPendentes);
        tabelaPendentes.setRowHeight(28);
        tabelaPendentes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        painel.add(new JScrollPane(tabelaPendentes), BorderLayout.CENTER);

        JButton concluir = new JButton("Marcar como retirado / concluído");
        concluir.setMnemonic(KeyEvent.VK_C);
        concluir.addActionListener(evento -> concluirPedido());
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botoes.add(concluir);
        painel.add(botoes, BorderLayout.SOUTH);
        return painel;
    }

    private void atualizarItensDisponiveis() {
        Object selecionado = seletorItem.getSelectedItem();
        seletorItem.removeAllItems();
        for (TelaCardapios.ItemCardapio item : TelaCardapios.listarItensDisponiveis()) {
            seletorItem.addItem(item);
        }
        // Na primeira carga, mantém o primeiro item; uma seleção removida vira vazia.
        if (selecionado != null) {
            seletorItem.setSelectedItem(null);
            for (int i = 0; i < seletorItem.getItemCount(); i++) {
                if (seletorItem.getItemAt(i) == selecionado) {
                    seletorItem.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void atualizarClientes() {
        Cliente selecionado = (Cliente) seletorCliente.getSelectedItem();
        Cliente paraSelecionar = null;
        seletorCliente.removeAllItems();
        for (Cliente cliente : TelaClientes.listarClientes()) {
            seletorCliente.addItem(cliente);
            if (selecionado != null && cliente.getIdCliente() == selecionado.getIdCliente()) {
                paraSelecionar = cliente;
            }
        }
        if (selecionado != null) seletorCliente.setSelectedItem(paraSelecionar);
        atualizarSaldoClienteSelecionado();
    }

    private void atualizarSaldoClienteSelecionado() {
        Cliente cliente = (Cliente) seletorCliente.getSelectedItem();
        rotuloSaldoCliente.setText(cliente == null
                ? "Saldo: nenhum cliente selecionado"
                : "Saldo: " + formatoMoeda.format(cliente.getSaldo()));
    }

    private void adicionarItemAoPedido() {
        TelaCardapios.ItemCardapio item =
                (TelaCardapios.ItemCardapio) seletorItem.getSelectedItem();
        if (item == null) {
            avisar("Não há itens disponíveis. Cadastre e salve um cardápio primeiro.");
            return;
        }
        if (!TelaCardapios.listarItensDisponiveis().contains(item)) {
            atualizarItensDisponiveis();
            avisar("Este item mudou ou ficou indisponível. Selecione novamente.");
            return;
        }
        try {
            // Confirma também a quantidade digitada, sem depender de sair do campo.
            campoQuantidade.commitEdit();
            int quantidade = ((Number) campoQuantidade.getValue()).intValue();
            itensDoNovoPedido.add(new ItemPedido(item, quantidade));
        } catch (ParseException | IllegalArgumentException excecao) {
            avisar("Informe uma quantidade inteira entre 1 e 99.");
            return;
        }
        atualizarTabelaNovoPedido();
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
            registrarNovoPedido(cliente.getIdCliente(), itensDoNovoPedido, campoAntecipado.isSelected());
        } catch (IllegalArgumentException excecao) {
            atualizarClientes();
            atualizarItensDisponiveis();
            avisar(excecao.getMessage());
            return;
        }

        limparNovoPedido();
        atualizarClientes();
        atualizarHub();
        JOptionPane.showMessageDialog(this, "Pedido registrado e enviado para preparação!");
    }

    /** Valida tudo antes de debitar. A regra usa o ID mesmo quando o nome é editado. */
    static Pedido registrarNovoPedido(int idCliente, List<ItemPedido> itens, boolean antecipado) {
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("Adicione pelo menos um item ao pedido.");
        }
        synchronized (PEDIDOS) {
            Cliente cliente = TelaClientes.buscarClientePorId(idCliente);
            if (cliente == null) {
                throw new IllegalArgumentException("O cliente não está mais cadastrado.");
            }
            if (temPedidoPendente(idCliente)) {
                throw new IllegalArgumentException(
                        "Este cliente já possui um pedido ativo. Conclua o pedido anterior primeiro.");
            }
            List<TelaCardapios.ItemCardapio> disponiveis = TelaCardapios.listarItensDisponiveis();
            BigDecimal totalPedido = BigDecimal.ZERO;
            for (ItemPedido item : itens) {
                // A referência distingue produtos de mesmo nome. Editar preço ou
                // disponibilidade cria outro item e exige atualizar o carrinho.
                if (item == null || !disponiveis.contains(item.origem)) {
                    throw new IllegalArgumentException(
                            "Um item foi alterado, removido ou ficou indisponível. Remova-o do pedido e selecione novamente.");
                }
                totalPedido = totalPedido.add(item.subtotal());
            }
            if (cliente.getSaldo().compareTo(totalPedido) < 0) {
                throw new IllegalArgumentException("Saldo insuficiente. Saldo atual: R$ "
                        + cliente.getSaldo().toPlainString().replace('.', ',')
                        + ". Total do pedido: R$ " + totalPedido.toPlainString().replace('.', ',') + ".");
            }
            Pedido pedido = new Pedido(proximoId, LocalDateTime.now(), new ArrayList<ItemPedido>(itens),
                    antecipado, idCliente, cliente.getNomeCliente(), false);
            if (!TelaClientes.debitarSaldo(idCliente, totalPedido)) {
                throw new IllegalArgumentException("O saldo mudou. Confira os dados e tente novamente.");
            }
            PEDIDOS.add(pedido);
            proximoId++;
            return pedido;
        }
    }

    static boolean temPedidoPendente(int idCliente) {
        synchronized (PEDIDOS) {
            for (Pedido pedido : PEDIDOS) {
                if (pedido.idCliente == idCliente && !pedido.retirado) return true;
            }
        }
        return false;
    }

    /** Mantém a mesma proteção da chave estrangeira ON DELETE RESTRICT do banco. */
    static boolean temPedidosDoCliente(int idCliente) {
        synchronized (PEDIDOS) {
            for (Pedido pedido : PEDIDOS) {
                if (pedido.idCliente == idCliente) return true;
            }
        }
        return false;
    }

    private void concluirPedido() {
        int linha = tabelaPendentes.getSelectedRow();
        if (linha < 0) {
            avisar("Selecione um pedido pendente.");
            return;
        }
        Pedido pedido = pedidosVisiveis.get(linha);
        int resposta = JOptionPane.showConfirmDialog(
                this, "Confirmar retirada do pedido #" + pedido.idPedido + "?",
                "Concluir pedido", JOptionPane.YES_NO_OPTION);
        if (resposta == JOptionPane.YES_OPTION) {
            pedido.retirado = true;
            atualizarHub();
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
        int linha = tabelaPendentes.getSelectedRow();
        Pedido selecionado = linha < 0 ? null : pedidosVisiveis.get(linha);
        modeloPendentes.setRowCount(0);
        pedidosVisiveis.clear();
        synchronized (PEDIDOS) {
            for (Pedido pedido : PEDIDOS) {
                if (!pedido.retirado) {
                    pedidosVisiveis.add(pedido);
                    modeloPendentes.addRow(new Object[] {
                            pedido.idPedido,
                            formatoData.format(pedido.dataPedido),
                            nomeAtualDoCliente(pedido),
                            pedido.resumoItens(),
                            formatoMoeda.format(pedido.total()),
                            pedido.antecipado ? "Sim" : "Não"
                    });
                }
            }
        }
        int indice = pedidosVisiveis.indexOf(selecionado);
        if (indice >= 0) tabelaPendentes.setRowSelectionInterval(indice, indice);
    }

    private String nomeAtualDoCliente(Pedido pedido) {
        Cliente cliente = TelaClientes.buscarClientePorId(pedido.idCliente);
        return cliente == null ? pedido.nomeCliente : cliente.getNomeCliente();
    }

    private void limparNovoPedido() {
        seletorCliente.setSelectedItem(null);
        campoAntecipado.setSelected(false);
        campoQuantidade.setValue(1);
        itensDoNovoPedido.clear();
        atualizarTabelaNovoPedido();
        atualizarSaldoClienteSelecionado();
    }

    private BigDecimal calcularTotalNovoPedido() {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : itensDoNovoPedido) {
            total = total.add(item.subtotal());
        }
        return total;
    }

    private void configurarAtalhos() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "fechar");
        getRootPane().getActionMap().put("fechar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evento) {
                dispose();
            }
        });
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK),
                        "registrarPedido");
        getRootPane().getActionMap().put("registrarPedido", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evento) {
                registrarPedido();
            }
        });
    }

    private void avisar(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Atenção", JOptionPane.WARNING_MESSAGE);
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

        private BigDecimal subtotal() {
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
        private boolean retirado;

        private Pedido(int id, LocalDateTime data, List<ItemPedido> itens,
                boolean antecipado, int idCliente, String nomeCliente, boolean retirado) {
            this.idPedido = id;
            this.dataPedido = data;
            this.itens = itens;
            this.antecipado = antecipado;
            this.idCliente = idCliente;
            this.nomeCliente = nomeCliente;
            this.retirado = retirado;
        }

        private BigDecimal total() {
            BigDecimal total = BigDecimal.ZERO;
            for (ItemPedido item : itens) {
                total = total.add(item.subtotal());
            }
            return total;
        }

        private String resumoItens() {
            StringBuilder resumo = new StringBuilder();
            for (int i = 0; i < itens.size(); i++) {
                if (i > 0) resumo.append(", ");
                resumo.append(itens.get(i).quantidade).append("x ").append(itens.get(i).nome);
            }
            return resumo.toString();
        }

        /** Representação que será persistida em itempedido (JSON). */
        @SuppressWarnings("unused")
        private String gerarItensJson() {
            StringBuilder json = new StringBuilder("{\"itens\":[");
            for (int i = 0; i < itens.size(); i++) {
                if (i > 0) json.append(',');
                ItemPedido item = itens.get(i);
                json.append("{\"nome\":\"")
                        .append(CardapioJson.escapar(item.nome))
                        .append("\",\"preco\":").append(item.preco.toPlainString())
                        .append(",\"quantidade\":").append(item.quantidade).append('}');
            }
            return json.append("]}").toString();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TelaPedidos().setVisible(true));
    }
}

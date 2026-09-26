import br.com.time7.cantina.util.ValoresMonetarios;
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
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gerencia os cardápios e os itens armazenados em cardapio_json.
 *
 * A disponibilidade geral continua sendo uma coluna do cardápio. Dentro do
 * JSON, cada item também possui sua própria disponibilidade.
 */
public class TelaCardapios extends JFrame {
    private final List<Cardapio> cardapios = new ArrayList<Cardapio>();
    private final List<ItemCardapio> itensEmEdicao = new ArrayList<ItemCardapio>();
    private final NumberFormat formatoMoeda = NumberFormat.getCurrencyInstance(
            new Locale.Builder().setLanguage("pt").setRegion("BR").build());

    private JComboBox<Cardapio> seletorCardapio;
    private JComboBox<String> campoDiaSemana;
    private JCheckBox campoCardapioDisponivel;
    private JTextField campoNomeItem;
    private JTextField campoPrecoItem;
    private JCheckBox campoItemDisponivel;
    private JTable tabelaItens;
    private DefaultTableModel modeloItens;
    private JTextArea campoJson;
    private int idCardapioEmEdicao;
    private boolean atualizandoSeletor;

    public TelaCardapios() {
        setTitle("Cantina Escolar - Cardápios");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1050, 720);
        setMinimumSize(new java.awt.Dimension(900, 620));
        setLocationRelativeTo(null);
        montarInterface();
        configurarAtalhos();
        atualizarSeletorCardapios(0);
        novoCardapio();
    }

    private void montarInterface() {
        JPanel principal = new JPanel(new BorderLayout(14, 14));
        principal.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        principal.setBackground(new Color(245, 247, 250));
        setContentPane(principal);

        JLabel titulo = new JLabel("Gerenciamento de cardápios");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 25));
        titulo.setForeground(new Color(32, 45, 64));
        principal.add(titulo, BorderLayout.NORTH);

        JPanel editor = new JPanel(new BorderLayout(10, 10));
        editor.setOpaque(false);
        editor.add(criarBarraCardapios(), BorderLayout.NORTH);
        editor.add(criarEditorItens(), BorderLayout.CENTER);

        JSplitPane divisao = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, editor, criarPainelJson());
        divisao.setResizeWeight(0.72);
        divisao.setBorder(null);
        principal.add(divisao, BorderLayout.CENTER);
        principal.add(criarBotoesCardapio(), BorderLayout.SOUTH);
    }

    private JPanel criarBarraCardapios() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBackground(Color.WHITE);
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        seletorCardapio = new JComboBox<Cardapio>();
        campoDiaSemana = new JComboBox<String>(new String[] {
                "Segunda-feira", "Terça-feira", "Quarta-feira",
                "Quinta-feira", "Sexta-feira", "Sábado", "Domingo"
        });
        campoCardapioDisponivel = new JCheckBox("Cardápio disponível", true);
        campoCardapioDisponivel.setOpaque(false);

        GridBagConstraints p = posicao(0, 0);
        painel.add(new JLabel("Cardápio salvo"), p);
        p = posicao(1, 0);
        p.weightx = 1;
        p.fill = GridBagConstraints.HORIZONTAL;
        painel.add(seletorCardapio, p);
        p = posicao(2, 0);
        painel.add(new JLabel("Dia da semana"), p);
        p = posicao(3, 0);
        painel.add(campoDiaSemana, p);
        p = posicao(4, 0);
        painel.add(campoCardapioDisponivel, p);

        seletorCardapio.addActionListener(evento -> {
            if (!atualizandoSeletor) {
                carregarCardapioSelecionado();
            }
        });
        return painel;
    }

    private JPanel criarEditorItens() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setOpaque(false);

        JPanel formulario = new JPanel(new GridLayout(2, 1, 5, 5));
        formulario.setBackground(Color.WHITE);
        formulario.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel campos = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        campos.setOpaque(false);
        campoNomeItem = new JTextField(22);
        campoPrecoItem = new JTextField("0,00", 9);
        campoItemDisponivel = new JCheckBox("Disponível", true);
        campoItemDisponivel.setOpaque(false);
        campos.add(new JLabel("Item *"));
        campos.add(campoNomeItem);
        campos.add(new JLabel("Preço *"));
        campos.add(campoPrecoItem);
        campos.add(campoItemDisponivel);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botoes.setOpaque(false);
        JButton adicionar = new JButton("Adicionar item");
        JButton editar = new JButton("Salvar edição do item");
        JButton alternar = new JButton("Disponível / esgotado");
        JButton remover = new JButton("Remover item");
        adicionar.setMnemonic(KeyEvent.VK_A);
        editar.setMnemonic(KeyEvent.VK_E);
        alternar.setMnemonic(KeyEvent.VK_D);
        remover.setMnemonic(KeyEvent.VK_R);
        adicionar.addActionListener(evento -> adicionarItem());
        editar.addActionListener(evento -> editarItem());
        alternar.addActionListener(evento -> alternarDisponibilidadeItem());
        remover.addActionListener(evento -> removerItem());
        botoes.add(adicionar);
        botoes.add(editar);
        botoes.add(alternar);
        botoes.add(remover);

        formulario.add(campos);
        formulario.add(botoes);
        painel.add(formulario, BorderLayout.NORTH);

        modeloItens = new DefaultTableModel(
                new String[] {"Item", "Preço", "Disponibilidade"}, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
        tabelaItens = new JTable(modeloItens);
        tabelaItens.setRowHeight(27);
        tabelaItens.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaItens.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                preencherItemSelecionado();
            }
        });
        painel.add(new JScrollPane(tabelaItens), BorderLayout.CENTER);
        return painel;
    }

    private JPanel criarPainelJson() {
        JPanel painel = new JPanel(new BorderLayout(6, 6));
        painel.setBorder(BorderFactory.createTitledBorder(
                "cardapio_json — nome, preço e disponibilidade de cada item"));
        campoJson = new JTextArea(6, 40);
        campoJson.setLineWrap(true);
        campoJson.setWrapStyleWord(true);
        campoJson.setEditable(false);
        campoJson.setBackground(new Color(245, 247, 250));
        campoJson.setFocusTraversalKeys(
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                KeyboardFocusManager.getCurrentKeyboardFocusManager()
                        .getDefaultFocusTraversalKeys(
                                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        campoJson.setFocusTraversalKeys(
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                KeyboardFocusManager.getCurrentKeyboardFocusManager()
                        .getDefaultFocusTraversalKeys(
                                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        painel.add(new JScrollPane(campoJson), BorderLayout.CENTER);

        return painel;
    }

    private JPanel criarBotoesCardapio() {
        JPanel painel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        painel.setOpaque(false);
        JButton novo = new JButton("Novo cardápio");
        JButton excluir = new JButton("Excluir cardápio");
        JButton salvar = new JButton("Salvar cardápio");
        novo.setMnemonic(KeyEvent.VK_N);
        excluir.setMnemonic(KeyEvent.VK_X);
        salvar.setMnemonic(KeyEvent.VK_S);
        salvar.setToolTipText("Salvar cardápio (Alt+S ou Ctrl+S)");
        novo.addActionListener(evento -> novoCardapio());
        excluir.addActionListener(evento -> excluirCardapio());
        salvar.addActionListener(evento -> salvarCardapio());
        painel.add(novo);
        painel.add(excluir);
        painel.add(salvar);
        getRootPane().setDefaultButton(salvar);
        return painel;
    }

    private void adicionarItem() {
        ItemCardapio item = lerItemDoFormulario();
        if (item != null) {
            itensEmEdicao.add(item);
            limparFormularioItem();
            atualizarTabelaEJson();
        }
    }

    private void editarItem() {
        int linha = tabelaItens.getSelectedRow();
        if (linha < 0) {
            avisar("Selecione um item para editar.");
            return;
        }
        ItemCardapio item = lerItemDoFormulario();
        if (item != null) {
            itensEmEdicao.set(linha, item);
            limparFormularioItem();
            atualizarTabelaEJson();
        }
    }

    private void alternarDisponibilidadeItem() {
        int linha = tabelaItens.getSelectedRow();
        if (linha < 0) {
            avisar("Selecione um item para alterar sua disponibilidade.");
            return;
        }
        ItemCardapio atual = itensEmEdicao.get(linha);
        itensEmEdicao.set(linha, new ItemCardapio(
                atual.getNome(), atual.getPreco(), !atual.isDisponivel()));
        atualizarTabelaEJson();
    }

    private void removerItem() {
        int linha = tabelaItens.getSelectedRow();
        if (linha < 0) {
            avisar("Selecione um item para remover.");
            return;
        }
        itensEmEdicao.remove(linha);
        limparFormularioItem();
        atualizarTabelaEJson();
    }

    private ItemCardapio lerItemDoFormulario() {
        String nome = campoNomeItem.getText().trim();
        if (nome.isEmpty()) {
            avisar("Informe o nome do item.");
            return null;
        }
        try {
            BigDecimal preco = ValoresMonetarios.converter(campoPrecoItem.getText(), "Preço");
            if (preco.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Informe um preço maior que zero, por exemplo: 8,50.");
            }
            return new ItemCardapio(nome, preco, campoItemDisponivel.isSelected());
        } catch (IllegalArgumentException excecao) {
            avisar(excecao.getMessage());
            return null;
        }
    }

    private void salvarCardapio() {
        if (itensEmEdicao.isEmpty()) {
            avisar("Adicione pelo menos um item ao cardápio.");
            return;
        }
        try {
            Cardapio salvo = BancoAplicacao.salvarCardapio(idCardapioEmEdicao,
                    campoDiaSemana.getSelectedIndex() + 1,
                    campoCardapioDisponivel.isSelected(), copiarItens(itensEmEdicao));
            idCardapioEmEdicao = salvo.getIdCardapio();
            atualizarSeletorCardapios(idCardapioEmEdicao);
            JOptionPane.showMessageDialog(this, "Cardápio salvo com sucesso!");
        } catch (RuntimeException erro) {
            avisar(erro.getMessage());
        }
    }

    private void excluirCardapio() {
        if (idCardapioEmEdicao == 0) {
            avisar("Selecione um cardápio salvo para excluir.");
            return;
        }
        int resposta = JOptionPane.showConfirmDialog(
                this, "Excluir este cardápio?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (resposta == JOptionPane.YES_OPTION) {
            try {
                BancoAplicacao.excluirCardapio(idCardapioEmEdicao);
                atualizarSeletorCardapios(0);
                novoCardapio();
            } catch (RuntimeException erro) {
                avisar(erro.getMessage());
            }
        }
    }

    private void novoCardapio() {
        idCardapioEmEdicao = 0;
        itensEmEdicao.clear();
        campoDiaSemana.setSelectedIndex(0);
        campoCardapioDisponivel.setSelected(true);
        seletorCardapio.setSelectedItem(null);
        limparFormularioItem();
        atualizarTabelaEJson();
    }

    private void carregarCardapioSelecionado() {
        Cardapio selecionado = (Cardapio) seletorCardapio.getSelectedItem();
        if (selecionado == null) {
            return;
        }
        idCardapioEmEdicao = selecionado.getIdCardapio();
        campoDiaSemana.setSelectedIndex(selecionado.getDiaSemana() - 1);
        campoCardapioDisponivel.setSelected(selecionado.isDisponivel());
        itensEmEdicao.clear();
        itensEmEdicao.addAll(copiarItens(selecionado.getItens()));
        limparFormularioItem();
        atualizarTabelaEJson();
    }

    private void atualizarSeletorCardapios(int idSelecionado) {
        atualizandoSeletor = true;
        seletorCardapio.removeAllItems();
        cardapios.clear();
        cardapios.addAll(BancoAplicacao.listarCardapios());
        Cardapio paraSelecionar = null;
        for (Cardapio cardapio : cardapios) {
            seletorCardapio.addItem(cardapio);
            if (cardapio.getIdCardapio() == idSelecionado) {
                paraSelecionar = cardapio;
            }
        }
        seletorCardapio.setSelectedItem(paraSelecionar);
        atualizandoSeletor = false;
    }

    private void atualizarTabelaEJson() {
        modeloItens.setRowCount(0);
        for (ItemCardapio item : itensEmEdicao) {
            modeloItens.addRow(new Object[] {
                    item.getNome(), formatoMoeda.format(item.getPreco()),
                    item.isDisponivel() ? "Disponível" : "Esgotado"
            });
        }
        campoJson.setText(CardapioJson.gerar(itensEmEdicao));
    }

    private void preencherItemSelecionado() {
        int linha = tabelaItens.getSelectedRow();
        if (linha >= 0 && linha < itensEmEdicao.size()) {
            ItemCardapio item = itensEmEdicao.get(linha);
            campoNomeItem.setText(item.getNome());
            campoPrecoItem.setText(item.getPreco().toPlainString().replace('.', ','));
            campoItemDisponivel.setSelected(item.isDisponivel());
        }
    }

    private void limparFormularioItem() {
        campoNomeItem.setText("");
        campoPrecoItem.setText("0,00");
        campoItemDisponivel.setSelected(true);
        tabelaItens.clearSelection();
    }

    private List<ItemCardapio> copiarItens(List<ItemCardapio> origem) {
        return new ArrayList<ItemCardapio>(origem);
    }

    /** Entrega à tela de pedidos somente os itens realmente disponíveis. */
    public static List<ItemCardapio> listarItensDisponiveis(int diaSemana) {
        return BancoAplicacao.listarItensDisponiveis(diaSemana);
    }

    private GridBagConstraints posicao(int coluna, int linha) {
        GridBagConstraints p = new GridBagConstraints();
        p.gridx = coluna;
        p.gridy = linha;
        p.insets = new Insets(4, 6, 4, 6);
        p.anchor = GridBagConstraints.WEST;
        return p;
    }

    private void avisar(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    private void configurarAtalhos() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "fechar");
        getRootPane().getActionMap().put("fechar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evento) { dispose(); }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "salvar");
        getRootPane().getActionMap().put("salvar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evento) { salvarCardapio(); }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK), "novo");
        getRootPane().getActionMap().put("novo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evento) { novoCardapio(); }
        });
    }

    public static class ItemCardapio {
        private final int idCardapio;
        private final String nome;
        private final BigDecimal preco;
        private final boolean disponivel;

        public ItemCardapio(String nome, BigDecimal preco, boolean disponivel) {
            this(0, nome, preco, disponivel);
        }

        public ItemCardapio(int idCardapio, String nome, BigDecimal preco, boolean disponivel) {
            this.idCardapio = idCardapio;
            this.nome = nome;
            this.preco = preco;
            this.disponivel = disponivel;
        }

        public int getIdCardapio() { return idCardapio; }
        public String getNome() { return nome; }
        public BigDecimal getPreco() { return preco; }
        public boolean isDisponivel() { return disponivel; }

        @Override
        public String toString() {
            return nome + " - R$ " + preco.toPlainString().replace('.', ',');
        }
    }

    static class Cardapio {
        private final int idCardapio;
        private final int diaSemana;
        private final boolean disponivel;
        private final List<ItemCardapio> itens;

        Cardapio(int id, int dia, boolean disponivel, List<ItemCardapio> itens) {
            this.idCardapio = id;
            this.diaSemana = dia;
            this.disponivel = disponivel;
            this.itens = itens;
        }

        int getIdCardapio() { return idCardapio; }
        int getDiaSemana() { return diaSemana; }
        boolean isDisponivel() { return disponivel; }
        List<ItemCardapio> getItens() { return itens; }

        @Override
        public String toString() {
            String[] dias = {"Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"};
            return "#" + idCardapio + " - " + dias[diaSemana - 1]
                    + (disponivel ? " (disponível)" : " (indisponível)");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TelaCardapios().setVisible(true));
    }
}

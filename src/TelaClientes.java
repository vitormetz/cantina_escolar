import br.com.time7.cantina.model.Cliente;
import br.com.time7.cantina.util.ValoresMonetarios;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tela de clientes da Cantina Escolar.
 *
 * Os nomes e tipos dos dados desta tela seguem a futura tabela Cliente:
 * idcliente, nomecliente, nomeresponsavel, saldo, limitesaldo,
 * emailresponsavel e alergias.
 *
 * Por enquanto, os registros ficam em uma lista na memória. Quando o banco de
 * dados for conectado, a interface poderá continuar praticamente igual: será
 * necessário trocar as operações na lista por INSERT, UPDATE, SELECT e DELETE.
 */
public class TelaClientes extends JFrame {

    // Campos correspondentes às colunas da futura tabela Cliente.
    private JTextField campoNomeCliente;
    private JTextField campoNomeResponsavel;
    private JTextField campoSaldo;
    private JTextField campoLimiteSaldo;
    private JTextField campoEmailResponsavel;
    private JTextArea campoAlergias;

    private JTable tabelaClientes;
    private DefaultTableModel modeloTabela;
    // A seleção aponta para o registro exibido, nunca para uma posição de outra janela.
    private final List<Cliente> clientesVisiveis = new ArrayList<Cliente>();
    private boolean atualizandoTabela;

    // Simula a coleção de clientes que futuramente virá do banco de dados.
    private static final List<Cliente> CLIENTES = new ArrayList<Cliente>();

    // Simula um ID gerado automaticamente pelo banco (AUTO_INCREMENT).
    private static int proximoId = 1;

    // Formata valores como R$ 10,00 somente para exibição na tabela.
    private final NumberFormat formatoMoeda =
            NumberFormat.getCurrencyInstance(
                    new Locale.Builder().setLanguage("pt").setRegion("BR").build());

    public TelaClientes() {
        configurarJanela();
        montarInterface();
        configurarAtalhos();
        atualizarTabela();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent evento) {
                atualizarTabela();
            }
        });
    }

    /** Define as configurações gerais da janela. */
    private void configurarJanela() {
        setTitle("Cantina Escolar - Clientes");
        // Ao fechar esta tela, o painel principal continua aberto.
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(980, 650));
        setSize(1120, 700);
        setLocationRelativeTo(null);
    }

    /** Monta e organiza todos os componentes visuais. */
    private void montarInterface() {
        JPanel painelPrincipal = new JPanel(new BorderLayout(16, 16));
        painelPrincipal.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        painelPrincipal.setBackground(new Color(245, 247, 250));
        setContentPane(painelPrincipal);

        painelPrincipal.add(criarCabecalho(), BorderLayout.NORTH);

        JPanel painelConteudo = new JPanel(new BorderLayout(12, 12));
        painelConteudo.setOpaque(false);
        painelConteudo.add(criarFormulario(), BorderLayout.NORTH);
        painelConteudo.add(criarTabela(), BorderLayout.CENTER);
        painelConteudo.add(criarBotoes(), BorderLayout.SOUTH);

        painelPrincipal.add(painelConteudo, BorderLayout.CENTER);
    }

    /** Cria o título e o texto explicativo da página. */
    private JPanel criarCabecalho() {
        JPanel cabecalho = new JPanel(new BorderLayout(0, 4));
        cabecalho.setOpaque(false);

        JLabel titulo = new JLabel("Gerenciamento de clientes");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 26));
        titulo.setForeground(new Color(32, 45, 64));

        JLabel subtitulo = new JLabel(
                "Cadastre o aluno, o responsável, os valores e as informações de segurança alimentar.");
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitulo.setForeground(new Color(90, 101, 116));

        cabecalho.add(titulo, BorderLayout.NORTH);
        cabecalho.add(subtitulo, BorderLayout.SOUTH);
        return cabecalho;
    }

    /** Cria o formulário usando os mesmos dados previstos para o banco. */
    private JPanel criarFormulario() {
        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBackground(Color.WHITE);
        formulario.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(218, 223, 230)),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        campoNomeCliente = new JTextField(22);
        campoNomeResponsavel = new JTextField(22);
        campoSaldo = new JTextField("0,00", 10);
        campoLimiteSaldo = new JTextField("0,00", 10);
        campoEmailResponsavel = new JTextField(24);
        campoAlergias = new JTextArea(2, 24);
        campoAlergias.setLineWrap(true);
        campoAlergias.setWrapStyleWord(true);
        // Em JTextArea, Tab normalmente insere uma tabulação. Aqui ele passa
        // para o próximo campo, como acontece no restante do formulário.
        campoAlergias.setFocusTraversalKeys(
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                KeyboardFocusManager.getCurrentKeyboardFocusManager()
                        .getDefaultFocusTraversalKeys(
                                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        campoAlergias.setFocusTraversalKeys(
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                KeyboardFocusManager.getCurrentKeyboardFocusManager()
                        .getDefaultFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));

        adicionarCampo(formulario, "Nome do cliente *", campoNomeCliente, 0, 0, 1);
        adicionarCampo(formulario, "Nome do responsável *", campoNomeResponsavel, 2, 0, 1);
        adicionarCampo(formulario, "Saldo", campoSaldo, 0, 1, 1);
        adicionarCampo(formulario, "Limite de saldo", campoLimiteSaldo, 2, 1, 1);
        adicionarCampo(formulario, "E-mail do responsável *", campoEmailResponsavel, 0, 2, 3);

        GridBagConstraints posicao = criarPosicao(0, 3);
        posicao.anchor = GridBagConstraints.NORTHWEST;
        formulario.add(new JLabel("Alergias"), posicao);

        posicao = criarPosicao(1, 3);
        posicao.gridwidth = 3;
        posicao.weightx = 1;
        posicao.weighty = 1;
        posicao.fill = GridBagConstraints.BOTH;
        formulario.add(new JScrollPane(campoAlergias), posicao);

        return formulario;
    }

    /** Evita repetir a configuração de cada linha do formulário. */
    private void adicionarCampo(
            JPanel painel,
            String rotulo,
            JTextField campo,
            int coluna,
            int linha,
            int largura) {

        GridBagConstraints posicaoRotulo = criarPosicao(coluna, linha);
        painel.add(new JLabel(rotulo), posicaoRotulo);

        GridBagConstraints posicaoCampo = criarPosicao(coluna + 1, linha);
        posicaoCampo.gridwidth = largura;
        posicaoCampo.weightx = 1;
        posicaoCampo.fill = GridBagConstraints.HORIZONTAL;
        painel.add(campo, posicaoCampo);
    }

    /** Cria uma configuração básica de posição para o GridBagLayout. */
    private GridBagConstraints criarPosicao(int coluna, int linha) {
        GridBagConstraints posicao = new GridBagConstraints();
        posicao.gridx = coluna;
        posicao.gridy = linha;
        posicao.insets = new Insets(5, 6, 5, 6);
        posicao.anchor = GridBagConstraints.WEST;
        return posicao;
    }

    /** Cria a tabela que apresenta os clientes cadastrados. */
    private JScrollPane criarTabela() {
        String[] colunas = {
                "ID", "Cliente", "Responsável", "Saldo", "Limite", "E-mail", "Alergias"
        };

        modeloTabela = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                // As edições são feitas pelo formulário, não pela célula.
                return false;
            }
        };

        tabelaClientes = new JTable(modeloTabela);
        tabelaClientes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaClientes.setRowHeight(28);
        tabelaClientes.getTableHeader().setReorderingAllowed(false);

        tabelaClientes.getColumnModel().getColumn(0).setPreferredWidth(40);
        tabelaClientes.getColumnModel().getColumn(1).setPreferredWidth(160);
        tabelaClientes.getColumnModel().getColumn(2).setPreferredWidth(170);
        tabelaClientes.getColumnModel().getColumn(3).setPreferredWidth(80);
        tabelaClientes.getColumnModel().getColumn(4).setPreferredWidth(80);
        tabelaClientes.getColumnModel().getColumn(5).setPreferredWidth(190);
        tabelaClientes.getColumnModel().getColumn(6).setPreferredWidth(180);

        // Ao selecionar uma linha, seus dados voltam para o formulário.
        tabelaClientes.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting() && !atualizandoTabela) {
                preencherFormularioComClienteSelecionado();
            }
        });

        JScrollPane rolagem = new JScrollPane(tabelaClientes);
        rolagem.setBorder(BorderFactory.createLineBorder(new Color(218, 223, 230)));
        return rolagem;
    }

    /** Cria os botões e liga cada botão à sua respectiva ação. */
    private JPanel criarBotoes() {
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.setOpaque(false);

        JButton botaoLimpar = new JButton("Limpar");
        JButton botaoExcluir = new JButton("Excluir");
        JButton botaoEditar = new JButton("Salvar edição");
        JButton botaoAdicionar = new JButton("Adicionar cliente");

        botaoLimpar.setMnemonic(KeyEvent.VK_L);
        botaoExcluir.setMnemonic(KeyEvent.VK_X);
        botaoEditar.setMnemonic(KeyEvent.VK_E);
        botaoAdicionar.setMnemonic(KeyEvent.VK_A);
        botaoAdicionar.setToolTipText("Adicionar cliente (Alt+A ou Enter)");

        botaoAdicionar.addActionListener(evento -> adicionarCliente());
        botaoEditar.addActionListener(evento -> editarCliente());
        botaoExcluir.addActionListener(evento -> excluirCliente());
        botaoLimpar.addActionListener(evento -> limparFormulario());

        botoes.add(botaoLimpar);
        botoes.add(botaoExcluir);
        botoes.add(botaoEditar);
        botoes.add(botaoAdicionar);
        getRootPane().setDefaultButton(botaoAdicionar);
        return botoes;
    }

    /** Adiciona um cliente à lista e atualiza a tabela. */
    private void adicionarCliente() {
        Cliente novoCliente = lerClienteDoFormulario(proximoId);
        if (novoCliente == null) {
            return;
        }

        if (nomeJaExiste(novoCliente.getNomeCliente(), 0)) {
            mostrarAviso("Já existe um cliente com esse nome. O nome deve ser único.");
            return;
        }

        synchronized (CLIENTES) {
            CLIENTES.add(novoCliente);
            proximoId++;
        }
        atualizarTabela();
        limparFormulario();

        JOptionPane.showMessageDialog(
                this,
                "Cliente cadastrado com sucesso!",
                "Cadastro concluído",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /** Atualiza o objeto correspondente à linha selecionada. */
    private void editarCliente() {
        int linhaSelecionada = tabelaClientes.getSelectedRow();

        if (linhaSelecionada == -1) {
            mostrarAviso("Selecione um cliente na tabela antes de editar.");
            return;
        }

        // O ID não muda durante a edição.
        Cliente original = clientesVisiveis.get(tabelaClientes.convertRowIndexToModel(linhaSelecionada));
        int idAtual = original.getIdCliente();
        Cliente clienteEditado = lerClienteDoFormulario(idAtual);
        if (clienteEditado == null) {
            return;
        }

        if (nomeJaExiste(clienteEditado.getNomeCliente(), idAtual)) {
            mostrarAviso("Já existe outro cliente com esse nome.");
            return;
        }

        synchronized (CLIENTES) {
            // Um pedido pode ter descontado o saldo depois que o formulário foi preenchido.
            // Não sobrescrevemos esse débito com os dados antigos da tela.
            if (buscarClientePorId(idAtual) != original) {
                atualizarTabela();
                mostrarAviso("Os dados deste cliente mudaram. Confira os valores e edite novamente.");
                return;
            }
            CLIENTES.set(CLIENTES.indexOf(original), clienteEditado);
        }
        atualizarTabela();
        limparFormulario();

        JOptionPane.showMessageDialog(
                this,
                "Dados do cliente atualizados!",
                "Edição concluída",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /** Exclui o cliente selecionado depois de pedir confirmação. */
    private void excluirCliente() {
        int linhaSelecionada = tabelaClientes.getSelectedRow();

        if (linhaSelecionada == -1) {
            mostrarAviso("Selecione um cliente na tabela antes de excluir.");
            return;
        }

        Cliente cliente = clientesVisiveis.get(tabelaClientes.convertRowIndexToModel(linhaSelecionada));
        if (TelaPedidos.temPedidosDoCliente(cliente.getIdCliente())) {
            mostrarAviso("Este cliente possui pedidos registrados e não pode ser excluído.");
            return;
        }
        int resposta = JOptionPane.showConfirmDialog(
                this,
                "Deseja realmente excluir o cliente " + cliente.getNomeCliente() + "?",
                "Confirmar exclusão",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (resposta == JOptionPane.YES_OPTION) {
            if (TelaPedidos.temPedidosDoCliente(cliente.getIdCliente())) {
                mostrarAviso("Este cliente possui pedidos registrados e não pode ser excluído.");
                return;
            }
            synchronized (CLIENTES) {
                Cliente atual = buscarClientePorId(cliente.getIdCliente());
                if (atual != null) CLIENTES.remove(atual);
            }
            atualizarTabela();
            limparFormulario();
        }
    }

    /**
     * Lê, valida e converte os campos da tela em um objeto Cliente.
     * Retorna null quando algum valor é inválido.
     */
    private Cliente lerClienteDoFormulario(int idCliente) {
        String nomeCliente = campoNomeCliente.getText().trim();
        String nomeResponsavel = campoNomeResponsavel.getText().trim();
        String emailResponsavel = campoEmailResponsavel.getText().trim();
        String alergias = campoAlergias.getText().trim();

        if (nomeCliente.isEmpty()
                || nomeResponsavel.isEmpty()
                || emailResponsavel.isEmpty()) {
            mostrarAviso("Preencha o nome do cliente, o responsável e o e-mail.");
            return null;
        }

        if (nomeCliente.length() > 150 || nomeResponsavel.length() > 150
                || emailResponsavel.length() > 255) {
            mostrarAviso("Use até 150 caracteres nos nomes e até 255 no e-mail.");
            return null;
        }

        if (!emailResponsavel.matches("[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+")) {
            mostrarAviso("Informe um e-mail válido para o responsável.");
            return null;
        }

        try {
            BigDecimal saldo = converterValorMonetario(campoSaldo.getText(), "Saldo");
            BigDecimal limiteSaldo =
                    converterValorMonetario(campoLimiteSaldo.getText(), "Limite de saldo");

            return new Cliente(
                    idCliente,
                    nomeCliente,
                    nomeResponsavel,
                    saldo,
                    limiteSaldo,
                    emailResponsavel,
                    alergias);
        } catch (IllegalArgumentException excecao) {
            mostrarAviso(excecao.getMessage());
            return null;
        }
    }

    /**
     * Converte 10,50 ou 10.50 em BigDecimal. Esse tipo é mais seguro que
     * double para valores monetários e poderá ser usado em uma coluna DECIMAL.
     */
    private BigDecimal converterValorMonetario(String texto, String nomeCampo) {
        return ValoresMonetarios.converter(texto, nomeCampo);
    }

    /** Recria as linhas da tabela a partir da lista de objetos Cliente. */
    private void atualizarTabela() {
        List<Cliente> atuais = listarClientes();
        // Voltar de uma mensagem não deve apagar a seleção nem a edição em andamento.
        if (atuais.equals(clientesVisiveis)) return;
        int linha = tabelaClientes.getSelectedRow();
        Cliente selecionado = linha < 0 ? null
                : clientesVisiveis.get(tabelaClientes.convertRowIndexToModel(linha));
        atualizandoTabela = true;
        try {
            modeloTabela.setRowCount(0);
            clientesVisiveis.clear();
            clientesVisiveis.addAll(atuais);
            for (Cliente cliente : clientesVisiveis) {
                modeloTabela.addRow(new Object[] {
                        cliente.getIdCliente(),
                        cliente.getNomeCliente(),
                        cliente.getNomeResponsavel(),
                        formatoMoeda.format(cliente.getSaldo()),
                        formatoMoeda.format(cliente.getLimiteSaldo()),
                        cliente.getEmailResponsavel(),
                        cliente.getAlergias()
                });
            }
            if (selecionado != null) {
                for (int i = 0; i < clientesVisiveis.size(); i++) {
                    if (clientesVisiveis.get(i).getIdCliente() == selecionado.getIdCliente()) {
                        tabelaClientes.setRowSelectionInterval(i, i);
                        break;
                    }
                }
            }
        } finally {
            atualizandoTabela = false;
        }
        if (selecionado != null && buscarClientePorId(selecionado.getIdCliente()) != selecionado) {
            if (tabelaClientes.getSelectedRow() < 0) limparFormulario();
            else preencherFormularioComClienteSelecionado();
        }
    }

    /** Copia o cliente selecionado para os campos de edição. */
    private void preencherFormularioComClienteSelecionado() {
        int linhaSelecionada = tabelaClientes.getSelectedRow();

        if (linhaSelecionada != -1) {
            Cliente cliente = clientesVisiveis.get(tabelaClientes.convertRowIndexToModel(linhaSelecionada));
            campoNomeCliente.setText(cliente.getNomeCliente());
            campoNomeResponsavel.setText(cliente.getNomeResponsavel());
            campoSaldo.setText(valorParaCampo(cliente.getSaldo()));
            campoLimiteSaldo.setText(valorParaCampo(cliente.getLimiteSaldo()));
            campoEmailResponsavel.setText(cliente.getEmailResponsavel());
            campoAlergias.setText(cliente.getAlergias());
        }
    }

    /** Converte o valor numérico para o formato 10,50 usado no formulário. */
    private String valorParaCampo(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP)
                .toPlainString()
                .replace('.', ',');
    }

    /** Limpa o formulário e remove a seleção atual da tabela. */
    private void limparFormulario() {
        campoNomeCliente.setText("");
        campoNomeResponsavel.setText("");
        campoSaldo.setText("0,00");
        campoLimiteSaldo.setText("0,00");
        campoEmailResponsavel.setText("");
        campoAlergias.setText("");
        tabelaClientes.clearSelection();
        campoNomeCliente.requestFocusInWindow();
    }

    /** Evita repetir a configuração das mensagens de aviso. */
    private void mostrarAviso(String mensagem) {
        JOptionPane.showMessageDialog(
                this,
                mensagem,
                "Atenção",
                JOptionPane.WARNING_MESSAGE);
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
    }

    /** Retorna uma cópia para preencher o seletor da tela de pedidos. */
    public static List<Cliente> listarClientes() {
        synchronized (CLIENTES) {
            return new ArrayList<Cliente>(CLIENTES);
        }
    }

    /**
     * Desconta pelo ID estável; o nome continua sendo a identificação visível.
     * Retorna false se o cliente não existir ou não possuir saldo suficiente.
     */
    public static boolean debitarSaldo(int idCliente, BigDecimal valor) {
        valor = ValoresMonetarios.validar(valor, "Total do pedido");
        if (valor.signum() <= 0) return false;
        synchronized (CLIENTES) {
            for (int i = 0; i < CLIENTES.size(); i++) {
                Cliente atual = CLIENTES.get(i);
                if (atual.getIdCliente() == idCliente
                        && atual.getSaldo().compareTo(valor) >= 0) {
                    CLIENTES.set(i, new Cliente(
                            atual.getIdCliente(),
                            atual.getNomeCliente(),
                            atual.getNomeResponsavel(),
                            atual.getSaldo().subtract(valor),
                            atual.getLimiteSaldo(),
                            atual.getEmailResponsavel(),
                            atual.getAlergias()));
                    return true;
                }
            }
        }
        return false;
    }

    public static Cliente buscarClientePorId(int idCliente) {
        synchronized (CLIENTES) {
            for (Cliente cliente : CLIENTES) {
                if (cliente.getIdCliente() == idCliente) return cliente;
            }
        }
        return null;
    }

    private boolean nomeJaExiste(String nome, int idIgnorado) {
        synchronized (CLIENTES) {
            for (Cliente cliente : CLIENTES) {
                if (cliente.getIdCliente() != idIgnorado
                        && cliente.getNomeCliente().equalsIgnoreCase(nome)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Ponto de entrada: é aqui que o Java inicia o programa. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception excecao) {
                // Se o tema do Windows falhar, o tema padrão do Java será usado.
            }

            TelaClientes tela = new TelaClientes();
            tela.setVisible(true);
        });
    }
}

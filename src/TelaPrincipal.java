import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.function.Supplier;

/** Tela inicial usada para acessar os três módulos da cantina. */
public class TelaPrincipal extends JFrame {
    private JFrame telaClientes;
    private JFrame telaCardapios;
    private JFrame telaPedidos;

    public TelaPrincipal() {
        setTitle("Cantina Escolar - Painel principal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 540);
        setMinimumSize(new Dimension(760, 480));
        setLocationRelativeTo(null);
        montarInterface();
    }

    private void montarInterface() {
        JPanel principal = new JPanel(new BorderLayout(20, 20));
        principal.setBorder(BorderFactory.createEmptyBorder(35, 45, 35, 45));
        principal.setBackground(new Color(245, 247, 250));
        setContentPane(principal);

        JPanel cabecalho = new JPanel(new GridLayout(2, 1, 0, 6));
        cabecalho.setOpaque(false);
        JLabel titulo = new JLabel("Cantina Escolar", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 34));
        titulo.setForeground(new Color(32, 45, 64));
        JLabel subtitulo = new JLabel(
                "Escolha uma área para começar", SwingConstants.CENTER);
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 16));
        subtitulo.setForeground(new Color(90, 101, 116));
        cabecalho.add(titulo);
        cabecalho.add(subtitulo);
        principal.add(cabecalho, BorderLayout.NORTH);

        JPanel modulos = new JPanel(new GridLayout(1, 3, 18, 0));
        modulos.setOpaque(false);

        JButton clientes = criarBotaoModulo(
                "CLIENTES",
                "Cadastrar, editar, consultar saldo e alergias");
        JButton cardapios = criarBotaoModulo(
                "CARDÁPIOS",
                "Editar itens, preços e disponibilidade");
        JButton pedidos = criarBotaoModulo(
                "PEDIDOS",
                "Acompanhar pedidos que devem ser preparados");

        clientes.setMnemonic(KeyEvent.VK_C);
        cardapios.setMnemonic(KeyEvent.VK_A);
        pedidos.setMnemonic(KeyEvent.VK_P);
        clientes.setToolTipText("Abrir clientes (Alt+C ou Ctrl+1)");
        cardapios.setToolTipText("Abrir cardápios (Alt+A ou Ctrl+2)");
        pedidos.setToolTipText("Abrir pedidos (Alt+P ou Ctrl+3)");

        clientes.addActionListener(evento -> telaClientes = abrirTela(telaClientes, TelaClientes::new));
        cardapios.addActionListener(evento -> telaCardapios = abrirTela(telaCardapios, TelaCardapios::new));
        pedidos.addActionListener(evento -> telaPedidos = abrirTela(telaPedidos, TelaPedidos::new));

        modulos.add(clientes);
        modulos.add(cardapios);
        modulos.add(pedidos);
        principal.add(modulos, BorderLayout.CENTER);

        JLabel rodape = new JLabel(
                "Time 7 • Sistema de gerenciamento da cantina",
                SwingConstants.CENTER);
        rodape.setForeground(new Color(110, 119, 132));
        principal.add(rodape, BorderLayout.SOUTH);

        configurarAtalho(KeyEvent.VK_1, "clientes", clientes);
        configurarAtalho(KeyEvent.VK_2, "cardapios", cardapios);
        configurarAtalho(KeyEvent.VK_3, "pedidos", pedidos);
        getRootPane().setDefaultButton(clientes);
    }

    /** Reutiliza a janela aberta para evitar edições concorrentes do mesmo cadastro. */
    private JFrame abrirTela(JFrame atual, Supplier<JFrame> criar) {
        if (atual == null || !atual.isDisplayable()) atual = criar.get();
        atual.setVisible(true);
        atual.setExtendedState(atual.getExtendedState() & ~JFrame.ICONIFIED);
        atual.toFront();
        atual.requestFocus();
        return atual;
    }

    private void configurarAtalho(int tecla, String nome, JButton botao) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(tecla, KeyEvent.CTRL_DOWN_MASK), nome);
        getRootPane().getActionMap().put(nome, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evento) {
                botao.doClick();
            }
        });
    }

    private JButton criarBotaoModulo(String titulo, String descricao) {
        String html = "<html><div style='text-align:center'>"
                + "<b style='font-size:17px'>" + titulo + "</b><br><br>"
                + "<span style='font-size:12px'>" + descricao + "</span>"
                + "</div></html>";
        JButton botao = new JButton(html);
        botao.setFont(new Font("SansSerif", Font.PLAIN, 14));
        botao.setBackground(Color.WHITE);
        botao.setForeground(new Color(32, 45, 64));
        botao.setFocusPainted(true);
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botao.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 217, 226)),
                BorderFactory.createEmptyBorder(20, 18, 20, 18)));
        return botao;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception excecao) {
                // O tema padrão do Java será usado se o tema do Windows falhar.
            }
            new TelaPrincipal().setVisible(true);
        });
    }
}

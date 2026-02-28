package com.acme.herald.copilot.ui;

import com.acme.herald.copilot.core.ConnectorState;
import com.formdev.flatlaf.FlatLightLaf;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.InputStream;

public class HeraldConnectorFrame extends JFrame {

    private static final Color INK_BLUE   = Color.decode("#0d47a1");
    private static final Color INK_INDIGO = Color.decode("#3F37C9");

    private static final Color BG      = Color.decode("#ffffff");
    private static final Color PANEL   = Color.decode("#ffffff");
    private static final Color PANEL_2 = Color.decode("#f8fafc");
    private static final Color PANEL_3 = Color.decode("#f1f5f9");

    private static final Color TEXT   = Color.decode("#1f2937");
    private static final Color TEXT_2 = Color.decode("#0f172a");
    private static final Color MUTED  = Color.decode("#64748b");

    private static final Color BORDER = Color.decode("#d7deea");

    private static volatile boolean lafInited = false;

    private final ConnectorState state;
    private final int port;
    private final Runnable onClose;

    private JTextArea logs;
    private JButton startBtn, stopBtn, clearLogsBtn;
    private JPasswordField tokenField;
    private JCheckBox showToken;
    private StatusPill statusPill;

    public HeraldConnectorFrame(ConnectorState state, int port, Runnable onClose) {
        super("Herald Copilot Connector");
        this.state = state;
        this.port = port;
        this.onClose = onClose;

        setupLookAndFeelOnce();
        buildUi();
        refreshFromState();

        append("Wklej GitHub Copilot token (PAT) i kliknij Start.");
        append("Jak wygenerować token:");
        append("1) Wejdź na: https://github.com/settings/personal-access-tokens");
        append("2) Kliknij: Generate new token");
        append("3) Nazwij token (dowolnie) i ustaw datę ważności (Expiration)");
        append("4) Kliknij: Add permissions");
        append("5) Wybierz uprawnienie: Copilot requests");
        append("6) Zapisz/utwórz token i skopiuj go tutaj (token pokaże się tylko raz).");
    }

    private static void setupLookAndFeelOnce() {
        if (lafInited) return;
        lafInited = true;

        FlatLightLaf.setup();

        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("Component.focusWidth", 1);

        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Panel.background", BG);
    }

    private void buildUi() {
        Image appIcon = loadImage("/assets/herald-icon.png");
        if (appIcon != null) setIconImage(appIcon);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(980, 640));
        setSize(1060, 720);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { safeClose(); }
            @Override public void windowClosed(WindowEvent e)  { safeClose(); }
        });

        var root = new JPanel(new BorderLayout(14, 14));
        root.setBorder(new EmptyBorder(18, 18, 18, 18));
        root.setBackground(BG);

        var header = new HeaderCard(loadImage("/assets/herald-mascot.png"));
        header.setLayout(new BorderLayout(12, 8));
        header.setBorder(new EmptyBorder(16, 16, 16, 16));

        var titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        var title = new JLabel("Herald Copilot Connector");
        title.setForeground(TEXT_2);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));

        var subtitle = new JLabel("Spring Boot most do GitHub Copilot SDK — token tylko w pamięci, REST OpenAI-like");
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 13f));

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(6));
        titleBox.add(subtitle);

        statusPill = new StatusPill();
        statusPill.setStateOff();

        var headerRight = new JPanel(new BorderLayout());
        headerRight.setOpaque(false);
        headerRight.setBorder(new EmptyBorder(0, 0, 0, 110));
        headerRight.add(statusPill, BorderLayout.NORTH);

        header.add(titleBox, BorderLayout.WEST);
        header.add(headerRight, BorderLayout.EAST);

        var content = new JPanel(new BorderLayout(14, 14));
        content.setOpaque(false);

        content.add(buildControlsCard(), BorderLayout.NORTH);
        content.add(buildLogsCard(), BorderLayout.CENTER);

        root.add(header, BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);

        setContentPane(root);
    }

    private JPanel buildControlsCard() {
        var card = new CardPanel();
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        tokenField = new JPasswordField();
        tokenField.setEchoChar('•');
        tokenField.setToolTipText("Wklej token GitHub (PAT). Token nie jest zapisywany na dysku.");

        showToken = new JCheckBox("Pokaż token");
        showToken.setOpaque(false);
        showToken.setForeground(TEXT);
        showToken.addActionListener(e -> tokenField.setEchoChar(showToken.isSelected() ? (char) 0 : '•'));

        startBtn = new JButton("Start");
        stopBtn = new JButton("Stop");
        stopBtn.setEnabled(false);

        startBtn.setBackground(INK_BLUE);
        startBtn.setForeground(Color.WHITE);

        startBtn.addActionListener(e -> start());
        stopBtn.addActionListener(e -> stop());

        clearLogsBtn = new JButton("Wyczyść logi");
        clearLogsBtn.addActionListener(e -> logs.setText(""));

        // Row 0: token
        c.gridx = 0; c.gridy = 0; c.gridwidth = 1; c.weightx = 0;
        card.add(label("GitHub token (PAT)"), c);

        c.gridx = 1; c.gridy = 0; c.gridwidth = 2; c.weightx = 1;
        card.add(tokenField, c);

        c.gridx = 3; c.gridy = 0; c.gridwidth = 1; c.weightx = 0;
        card.add(showToken, c);

        // Row 1: start/stop + clear logs
        c.gridy = 1; c.gridwidth = 1; c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.weightx = 1;
        card.add(startBtn, c);

        c.gridx = 1; c.weightx = 1;
        card.add(stopBtn, c);

        c.gridx = 2; c.weightx = 1;
        card.add(clearLogsBtn, c);

        c.gridx = 3; c.weightx = 1;
        card.add(Box.createHorizontalStrut(1), c);

        return card;
    }

    private JPanel buildLogsCard() {
        var logsWrap = new CardPanel();
        logsWrap.setLayout(new BorderLayout());
        logsWrap.setBorder(new EmptyBorder(10, 10, 10, 10));

        logs = new JTextArea();
        logs.setEditable(false);
        logs.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logs.setBackground(PANEL_3);
        logs.setForeground(TEXT);
        logs.setBorder(new EmptyBorder(10, 10, 10, 10));

        var scroll = new JScrollPane(logs);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        scroll.getViewport().setBackground(PANEL_3);

        logsWrap.add(scroll, BorderLayout.CENTER);
        return logsWrap;
    }

    private JLabel label(String s) {
        var l = new JLabel(s);
        l.setForeground(TEXT_2);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 13f));
        return l;
    }

    private String endpoint() {
        return "http://localhost:" + port + "/chat/completions";
    }

    private void start() {
        if (state.isEnabled()) return;

        String token = new String(tokenField.getPassword()).trim();
        try {
            validateTokenOrThrow(token);
            state.enableWithToken(token);

            statusPill.setStateOn("ON");
            append("Start.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Niepoprawny token", JOptionPane.WARNING_MESSAGE);
        }

        refreshFromState();
    }

    private void stop() {
        state.disableAndClearToken();
        statusPill.setStateOff();
        append("Stop.");
        refreshFromState();
    }

    private void refreshFromState() {
        boolean on = state.isEnabled();

        startBtn.setEnabled(!on);
        stopBtn.setEnabled(on);

        tokenField.setEnabled(!on);
        showToken.setEnabled(!on);

        if (on) statusPill.setStateOn("ON");
        else statusPill.setStateOff();
    }

    private void safeClose() {
        state.disableAndClearToken();
        if (onClose != null) onClose.run();
    }

    private void append(String s) {
        SwingUtilities.invokeLater(() -> {
            logs.append(s + "\n");
            logs.setCaretPosition(logs.getDocument().getLength());
        });
    }

    private static void validateTokenOrThrow(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Wklej token GitHub (PAT).");
        }
        if (!token.startsWith("github_pat")) {
            throw new IllegalArgumentException("Token powinien rozpoczynać się od 'github_pat'.");
        }
    }

    private static Image loadImage(String resourcePath) {
        try (InputStream is = HeraldConnectorFrame.class.getResourceAsStream(resourcePath)) {
            if (is == null) return null;
            return ImageIO.read(is);
        } catch (Exception e) {
            return null;
        }
    }

    static class CardPanel extends JPanel {
        CardPanel() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            g2.setColor(PANEL);
            g2.fillRoundRect(0, 0, w, h, 18, 18);

            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 18, 18);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class HeaderCard extends JPanel {
        private final Image mascot;

        HeaderCard(Image mascot) {
            this.mascot = mascot;
            setOpaque(false);
            setPreferredSize(new Dimension(900, 120));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            var rr = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 18, 18);

            g2.setColor(PANEL);
            g2.fill(rr);

            Shape oldClip = g2.getClip();
            g2.setClip(rr);
            g2.setColor(INK_BLUE);
            g2.fillRect(1, 1, w - 2, 4);
            g2.setClip(oldClip);

            g2.setColor(BORDER);
            g2.draw(rr);

            if (mascot != null) {
                int size = 88;
                int x = w - size - 14;
                int y = (h - size) / 2;
                Image scaled = mascot.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                g2.drawImage(scaled, x, y, null);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class StatusPill extends JLabel {
        private Color bg;

        StatusPill() {
            setOpaque(false);
            setBorder(new EmptyBorder(6, 10, 6, 10));
            setFont(getFont().deriveFont(Font.BOLD, 12f));
            setStateOff();
        }

        void setStateOff() {
            setText("OFF");
            setForeground(MUTED);
            bg = new Color(100, 116, 139, 30);
            repaint();
        }

        void setStateOn(String text) {
            setText(text);
            setForeground(new Color(22, 101, 52));
            bg = new Color(34, 197, 94, 35);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, w, h, 999, 999);

            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 999, 999);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
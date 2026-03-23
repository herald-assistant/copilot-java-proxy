package com.acme.herald.copilot.ui;

import com.acme.herald.copilot.core.ConnectorState;
import com.acme.herald.copilot.core.security.DpapiCrypto;
import com.acme.herald.copilot.core.security.LocalTokenStore;
import com.formdev.flatlaf.FlatLightLaf;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class HeraldConnectorFrame extends JFrame {

    private static final Color INK_BLUE = Color.decode("#0d47a1");
    private static final Color INK_INDIGO = Color.decode("#3F37C9");

    private static final Color BG = Color.decode("#f4f7fb");
    private static final Color PANEL = Color.decode("#ffffff");
    private static final Color PANEL_2 = Color.decode("#f8fafc");
    private static final Color PANEL_3 = Color.decode("#eef3f9");

    private static final Color TEXT = Color.decode("#334155");
    private static final Color TEXT_2 = Color.decode("#0f172a");
    private static final Color MUTED = Color.decode("#64748b");

    private static final Color BORDER = Color.decode("#d7deea");
    private static final Color SUCCESS = Color.decode("#166534");
    private static final Color DANGER = Color.decode("#b42318");

    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static volatile boolean lafInited = false;

    private final ConnectorState state;
    private final int port;
    private final Runnable onClose;
    private final LocalTokenStore tokenStore = new LocalTokenStore();

    private JTextArea logs;
    private JButton startBtn;
    private JButton stopBtn;
    private JButton clearLogsBtn;
    private JButton deleteSavedTokenBtn;

    private JPasswordField tokenField;
    private JCheckBox showToken;
    private JCheckBox persistToken;

    private StatusPill statusPill;
    private JLabel stateHeadline;
    private JLabel stateSubline;
    private JLabel endpointValue;
    private JLabel portValue;
    private JLabel storageValue;
    private JLabel runtimeValue;

    private boolean closeHandled = false;

    public HeraldConnectorFrame(ConnectorState state, int port, Runnable onClose) {
        super("Herald Copilot Connector");
        this.state = state;
        this.port = port;
        this.onClose = onClose;

        setupLookAndFeelOnce();
        buildUi();

        tryAutoLoadSavedToken();
        refreshFromState();

        append("Connector gotowy.");
        append("Wklej token GitHub PAT z uprawnieniem 'Copilot requests' i uruchom connector.");
        append("Endpoint lokalny: http://localhost:" + port);
    }

    private static void setupLookAndFeelOnce() {
        if (lafInited) {
            return;
        }
        lafInited = true;

        FlatLightLaf.setup();

        UIManager.put("Component.arc", 14);
        UIManager.put("Button.arc", 14);
        UIManager.put("TextComponent.arc", 14);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Panel.background", BG);
    }

    private void buildUi() {
        Image appIcon = loadImage("/assets/herald-icon.png");
        if (appIcon != null) {
            setIconImage(appIcon);
        }

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 760));
        setSize(1240, 860);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                safeClose();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                safeClose();
            }
        });

        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBorder(new EmptyBorder(20, 20, 20, 20));
        root.setBackground(BG);

        root.add(buildHeaderCard(loadImage("/assets/herald-mascot.png")), BorderLayout.NORTH);
        root.add(buildMainSplitPane(), BorderLayout.CENTER);

        setContentPane(root);
    }

    private JComponent buildMainSplitPane() {
        JPanel dashboard = buildTopRow();
        dashboard.setMinimumSize(new Dimension(320, 320));

        JPanel logsCard = buildLogsCard();
        logsCard.setMinimumSize(new Dimension(320, 220));
        logsCard.setPreferredSize(new Dimension(320, 280));

        JComponent topSection = wrapSplitSection(dashboard);
        JComponent bottomSection = wrapSplitSection(logsCard);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSection, bottomSection);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setDividerSize(10);
        split.setResizeWeight(0.64);
        split.setContinuousLayout(true);
        split.setOneTouchExpandable(false);
        split.setOpaque(true);
        split.setBackground(BG);
        split.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            topSection.repaint();
            bottomSection.repaint();
            split.repaint();
        });

        SwingUtilities.invokeLater(() -> split.setDividerLocation(430));
        return split;
    }

    private JPanel buildHeaderCard(Image mascot) {
        HeaderCard header = new HeaderCard(mascot);
        header.setLayout(new BorderLayout(16, 0));
        header.setBorder(new EmptyBorder(16, 20, 16, 128));

        JPanel textBox = new JPanel();
        textBox.setOpaque(false);
        textBox.setLayout(new BoxLayout(textBox, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("DESKTOP CONNECTOR");
        eyebrow.setForeground(INK_BLUE);
        eyebrow.setFont(eyebrow.getFont().deriveFont(Font.BOLD, 11f));
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Herald Copilot Connector");
        title.setForeground(TEXT_2);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 27f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Lokalny bridge Spring Boot do GitHub Copilot SDK");
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 13f));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel description = htmlLabel(
                "Przewidywalny lokalny connector dla Heralda. " +
                        "UI upraszcza autoryzację, start runtime i podstawową diagnostykę."
        );
        description.setForeground(TEXT);
        description.setBorder(new EmptyBorder(8, 0, 0, 0));
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);
        chips.setBorder(new EmptyBorder(12, 0, 0, 0));
        chips.setAlignmentX(Component.LEFT_ALIGNMENT);
        chips.add(createChip("Endpoint: http://localhost:" + port));
        chips.add(createChip(DpapiCrypto.isWindows()
                ? "Token lokalny: Windows DPAPI"
                : "Token lokalny: tylko pamięć sesji"));

        textBox.add(eyebrow);
        textBox.add(Box.createVerticalStrut(6));
        textBox.add(title);
        textBox.add(Box.createVerticalStrut(6));
        textBox.add(subtitle);
        textBox.add(description);
        textBox.add(chips);

        header.add(textBox, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildTopRow() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;

        c.gridx = 0;
        c.weightx = 1.08;
        c.insets = new Insets(0, 0, 0, 8);
        row.add(buildControlsCard(), c);

        c.gridx = 1;
        c.weightx = 0.92;
        c.insets = new Insets(0, 8, 0, 0);
        row.add(buildOverviewCard(), c);

        return row;
    }

    private JPanel buildControlsCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        card.add(sectionHeader(
                "Połączenie i autoryzacja",
                "Wklej token PAT, zdecyduj czy zapamiętać go lokalnie i uruchom connector."
        ), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        tokenField = new JPasswordField();
        tokenField.setEchoChar('•');
        tokenField.putClientProperty("JTextField.placeholderText", "github_pat_...");
        tokenField.setToolTipText("Wklej token GitHub PAT.");

        tokenField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshFromState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshFromState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshFromState();
            }
        });

        showToken = new JCheckBox("Pokaż token");
        styleCheckBox(showToken);
        showToken.addActionListener(e -> tokenField.setEchoChar(showToken.isSelected() ? (char) 0 : '•'));

        persistToken = new JCheckBox("Zapamiętaj token na tym komputerze");
        styleCheckBox(persistToken);
        persistToken.setToolTipText("Token zostanie zapisany lokalnie w profilu użytkownika.");

        deleteSavedTokenBtn = new JButton("Usuń zapisany token");
        styleGhostButton(deleteSavedTokenBtn);
        deleteSavedTokenBtn.setForeground(DANGER);
        deleteSavedTokenBtn.addActionListener(e -> deleteSavedToken());

        startBtn = new JButton("Uruchom connector");
        stylePrimaryButton(startBtn);
        startBtn.addActionListener(e -> start());

        stopBtn = new JButton("Zatrzymaj");
        styleSecondaryButton(stopBtn);
        stopBtn.addActionListener(e -> stop());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 4;
        c.insets = new Insets(0, 0, 6, 0);
        form.add(fieldLabel("GitHub token (PAT)"), c);

        c.gridy = 1;
        c.gridwidth = 3;
        c.insets = new Insets(0, 0, 8, 8);
        form.add(tokenField, c);

        c.gridx = 3;
        c.gridwidth = 1;
        c.weightx = 0;
        c.insets = new Insets(0, 0, 8, 0);
        form.add(showToken, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 4;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 12, 0);
        form.add(metaLabel("Wymagane uprawnienie: Copilot requests. Bez zapisu lokalnego token pozostaje tylko w pamięci procesu."), c);

        c.gridy = 3;
        c.gridwidth = 3;
        c.insets = new Insets(0, 0, 12, 8);
        form.add(persistToken, c);

        c.gridx = 3;
        c.gridwidth = 1;
        c.insets = new Insets(0, 0, 12, 0);
        form.add(deleteSavedTokenBtn, c);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionRow.setOpaque(false);
        actionRow.add(startBtn);
        actionRow.add(stopBtn);

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 4;
        c.insets = new Insets(0, 0, 0, 0);
        form.add(actionRow, c);

        card.add(form, BorderLayout.CENTER);
        card.add(buildSecurityBanner(), BorderLayout.SOUTH);

        updatePersistControls();
        return card;
    }

    private JPanel buildOverviewCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);

        top.add(sectionHeader(
                "Stan connectora",
                "Najważniejsze informacje runtime w jednym miejscu."
        ), BorderLayout.CENTER);

        statusPill = new StatusPill();
        statusPill.setStateOff();
        top.add(statusPill, BorderLayout.EAST);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        stateHeadline = new JLabel("Connector wyłączony");
        stateHeadline.setForeground(TEXT_2);
        stateHeadline.setFont(stateHeadline.getFont().deriveFont(Font.BOLD, 21f));
        stateHeadline.setAlignmentX(Component.LEFT_ALIGNMENT);

        stateSubline = new JLabel("Wklej token i kliknij Uruchom connector.");
        stateSubline.setForeground(MUTED);
        stateSubline.setFont(stateSubline.getFont().deriveFont(Font.PLAIN, 13f));
        stateSubline.setAlignmentX(Component.LEFT_ALIGNMENT);

        endpointValue = valueLabel("");
        portValue = valueLabel("");
        storageValue = valueLabel("");
        runtimeValue = valueLabel("");

        JComponent sectionDivider = divider();

        content.add(lockToPreferredHeight(stateHeadline));
        content.add(Box.createVerticalStrut(6));
        content.add(lockToPreferredHeight(stateSubline));
        content.add(Box.createVerticalStrut(14));
        content.add(sectionDivider);
        content.add(Box.createVerticalStrut(14));
        content.add(infoRow("Endpoint", endpointValue));
        content.add(Box.createVerticalStrut(10));
        content.add(infoRow("Port", portValue));
        content.add(Box.createVerticalStrut(10));
        content.add(infoRow("Token lokalny", storageValue));
        content.add(Box.createVerticalStrut(10));
        content.add(infoRow("Runtime", runtimeValue));

        body.add(content, BorderLayout.NORTH);

        card.add(top, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        card.add(buildGuideBanner(), BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildLogsCard() {
        CardPanel logsWrap = new CardPanel();
        logsWrap.setLayout(new BorderLayout(0, 14));
        logsWrap.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        top.add(sectionHeader(
                "Logi runtime",
                "Lokalne zdarzenia connectora i podstawowa diagnostyka."
        ), BorderLayout.CENTER);

        clearLogsBtn = new JButton("Wyczyść logi");
        styleGhostButton(clearLogsBtn);
        clearLogsBtn.addActionListener(e -> logs.setText(""));
        top.add(clearLogsBtn, BorderLayout.EAST);

        logs = new JTextArea();
        logs.setEditable(false);
        logs.setRows(12);
        logs.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logs.setBackground(PANEL_3);
        logs.setForeground(TEXT_2);
        logs.setMargin(new Insets(12, 12, 12, 12));
        logs.setBorder(BorderFactory.createEmptyBorder());

        JScrollPane scroll = new JScrollPane(logs);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        scroll.getViewport().setBackground(PANEL_3);
        scroll.setPreferredSize(new Dimension(200, 260));

        logsWrap.add(top, BorderLayout.NORTH);
        logsWrap.add(scroll, BorderLayout.CENTER);
        return logsWrap;
    }

    private JPanel buildSecurityBanner() {
        InfoBannerPanel banner = new InfoBannerPanel();
        banner.setLayout(new BorderLayout(0, 6));
        banner.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel title = new JLabel("Bezpieczeństwo tokena");
        title.setForeground(TEXT_2);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));

        String bodyText = DpapiCrypto.isWindows()
                ? "Token może zostać zapisany lokalnie z użyciem Windows DPAPI. Bez tej opcji pozostaje wyłącznie w pamięci procesu."
                : "Na tym systemie token działa wyłącznie w pamięci bieżącej sesji.";

        JLabel body = htmlLabel(bodyText);
        body.setForeground(TEXT);

        banner.add(title, BorderLayout.NORTH);
        banner.add(body, BorderLayout.CENTER);
        return banner;
    }

    private JPanel buildGuideBanner() {
        InfoBannerPanel banner = new InfoBannerPanel();
        banner.setLayout(new BorderLayout(0, 6));
        banner.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel title = new JLabel("Szybki start");
        title.setForeground(TEXT_2);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));

        JLabel body = htmlLabel(
                "1. GitHub -> Settings -> Developer settings -> Personal access tokens.<br>" +
                        "2. Wygeneruj nowy token i dodaj permission <b>Copilot requests</b>.<br>" +
                        "3. Wklej token i kliknij <b>Uruchom connector</b>."
        );
        body.setForeground(TEXT);

        banner.add(title, BorderLayout.NORTH);
        banner.add(body, BorderLayout.CENTER);
        return banner;
    }

    private void tryAutoLoadSavedToken() {
        if (!DpapiCrypto.isWindows()) {
            if (persistToken != null) {
                persistToken.setSelected(false);
                persistToken.setEnabled(false);
                persistToken.setToolTipText("Zapamiętywanie tokena jest dostępne tylko na Windows.");
            }
            if (deleteSavedTokenBtn != null) {
                deleteSavedTokenBtn.setEnabled(false);
            }
            append("Info: zapamiętywanie tokena jest dostępne tylko na Windows.");
            return;
        }

        if (!tokenStore.exists()) {
            updatePersistControls();
            return;
        }

        try {
            Optional<String> opt = tokenStore.loadPlainToken();
            if (opt.isPresent()) {
                tokenField.setText(opt.get());
                persistToken.setSelected(true);
                append("Wczytano token zapisany lokalnie.");
            }
        } catch (Exception e) {
            append("Uwaga: nie udało się wczytać tokena z dysku.");
        }

        updatePersistControls();
    }

    private void start() {
        if (state.isEnabled()) {
            return;
        }

        String token = new String(tokenField.getPassword()).trim();

        try {
            validateTokenOrThrow(token);

            if (persistToken != null && persistToken.isSelected()) {
                if (!DpapiCrypto.isWindows()) {
                    throw new IllegalArgumentException("Zapamiętywanie tokena działa tylko na Windows.");
                }
                try {
                    tokenStore.savePlainToken(token);
                    append("Token zapisany lokalnie.");
                } catch (IOException ioe) {
                    throw new IllegalArgumentException("Nie udało się zapisać tokena lokalnie: " + ioe.getMessage());
                }
            }

            state.enableWithToken(token);
            append("Connector uruchomiony.");
            append("Nasłuchiwanie lokalne: http://localhost:" + port);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Niepoprawny token", JOptionPane.WARNING_MESSAGE);
        }

        refreshFromState();
    }

    private void stop() {
        state.disableAndClearToken();
        append("Connector zatrzymany.");
        refreshFromState();
    }

    private void deleteSavedToken() {
        if (state.isEnabled()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Zatrzymaj connector, aby usunąć zapisany token.",
                    "Connector aktywny",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        if (!DpapiCrypto.isWindows()) {
            return;
        }

        if (!tokenStore.exists()) {
            append("Brak zapisanego tokena do usunięcia.");
            refreshFromState();
            return;
        }

        int res = JOptionPane.showConfirmDialog(
                this,
                "Usunąć token zapisany lokalnie na tym komputerze?",
                "Potwierdź usunięcie",
                JOptionPane.YES_NO_OPTION
        );
        if (res != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            tokenStore.delete();
            tokenField.setText("");
            if (persistToken != null) {
                persistToken.setSelected(false);
            }
            append("Usunięto zapisany token.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Nie udało się usunąć tokena: " + ex.getMessage(),
                    "Błąd",
                    JOptionPane.ERROR_MESSAGE
            );
        }

        refreshFromState();
    }

    private void refreshFromState() {
        boolean on = state.isEnabled();
        boolean hasToken = tokenField != null && !new String(tokenField.getPassword()).trim().isEmpty();

        startBtn.setEnabled(!on && hasToken);
        stopBtn.setEnabled(on);

        tokenField.setEnabled(!on);
        showToken.setEnabled(!on);

        updatePersistControls();

        if (on) {
            statusPill.setStateOn("AKTYWNY");
            stateHeadline.setText("Connector aktywny");
            stateHeadline.setForeground(SUCCESS);
            stateSubline.setText("Lokalny bridge działa i odpowiada pod wskazanym endpointem.");
        } else {
            statusPill.setStateOff();
            stateHeadline.setText("Connector wyłączony");
            stateHeadline.setForeground(TEXT_2);
            stateSubline.setText("Wklej token i kliknij Uruchom connector.");
        }

        endpointValue.setText("http://localhost:" + port);
        portValue.setText(String.valueOf(port));
        runtimeValue.setText(on ? "Sesja aktywna" : "Oczekuje na start");
        storageValue.setText(describeStorageState());
    }

    private String describeStorageState() {
        if (!DpapiCrypto.isWindows()) {
            return "Tylko pamięć sesji";
        }
        if (tokenStore.exists()) {
            return "Zapisany lokalnie (DPAPI)";
        }
        if (persistToken != null && persistToken.isSelected()) {
            return "Zapis przy następnym starcie";
        }
        return "Tylko pamięć sesji";
    }

    private void updatePersistControls() {
        boolean canPersist = DpapiCrypto.isWindows();
        boolean on = state.isEnabled();
        boolean hasSaved = tokenStore.exists();

        if (persistToken != null) {
            persistToken.setEnabled(!on && canPersist);
            if (!canPersist) {
                persistToken.setSelected(false);
                persistToken.setToolTipText("Zapamiętywanie tokena jest dostępne tylko na Windows.");
            }
        }

        if (deleteSavedTokenBtn != null) {
            deleteSavedTokenBtn.setEnabled(!on && canPersist && hasSaved);
        }
    }

    private void safeClose() {
        if (closeHandled) {
            return;
        }
        closeHandled = true;
        state.disableAndClearToken();
        if (onClose != null) {
            onClose.run();
        }
    }

    private void append(String s) {
        SwingUtilities.invokeLater(() -> {
            logs.append("[" + LocalTime.now().format(LOG_TIME) + "] " + s + "\n");
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
            if (is == null) {
                return null;
            }
            return ImageIO.read(is);
        } catch (Exception e) {
            return null;
        }
    }

    private JPanel sectionHeader(String titleText, String subtitleText) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(titleText);
        title.setForeground(TEXT_2);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));

        JLabel subtitle = htmlLabel(subtitleText);
        subtitle.setForeground(MUTED);
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));

        panel.add(title);
        panel.add(subtitle);
        return panel;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_2);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        return label;
    }

    private JLabel metaLabel(String text) {
        JLabel label = htmlLabel(text);
        label.setForeground(MUTED);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));
        return label;
    }

    private JLabel valueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_2);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }

    private JPanel infoRow(String labelText, JLabel value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setForeground(MUTED);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));

        row.add(label, BorderLayout.WEST);
        row.add(value, BorderLayout.CENTER);
        return lockToPreferredHeight(row);
    }

    private JComponent divider() {
        JPanel line = new JPanel();
        line.setBackground(BORDER);
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        line.setMinimumSize(new Dimension(0, 1));
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        line.setPreferredSize(new Dimension(1, 1));
        return lockToPreferredHeight(line);
    }

    private <T extends JComponent> T lockToPreferredHeight(T component) {
        Dimension preferred = component.getPreferredSize();
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
        return component;
    }

    private JComponent wrapSplitSection(JComponent content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(true);
        wrapper.setBackground(BG);
        wrapper.add(content, BorderLayout.CENTER);
        wrapper.setMinimumSize(content.getMinimumSize());
        wrapper.setPreferredSize(content.getPreferredSize());
        return wrapper;
    }

    private JLabel createChip(String text) {
        JLabel chip = new JLabel(text);
        chip.setOpaque(true);
        chip.setForeground(TEXT_2);
        chip.setBackground(new Color(13, 71, 161, 18));
        chip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(13, 71, 161, 35), 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        chip.setFont(chip.getFont().deriveFont(Font.BOLD, 12f));
        return chip;
    }

    private JLabel htmlLabel(String text) {
        return new JLabel("<html>" + text + "</html>");
    }

    private void styleCheckBox(JCheckBox checkBox) {
        checkBox.setOpaque(false);
        checkBox.setForeground(TEXT);
        checkBox.setFocusPainted(false);
    }

    private void stylePrimaryButton(JButton button) {
        button.setBackground(INK_BLUE);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
    }

    private void styleSecondaryButton(JButton button) {
        button.setBackground(PANEL_2);
        button.setForeground(TEXT_2);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
    }

    private void styleGhostButton(JButton button) {
        button.setBackground(PANEL_2);
        button.setForeground(TEXT);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(9, 14, 9, 14));
    }

    static class CardPanel extends JPanel {
        CardPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(PANEL);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 22, 22);

            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 22, 22);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class InfoBannerPanel extends JPanel {
        InfoBannerPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            RoundRectangle2D rr = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 18, 18);
            g2.setColor(PANEL_2);
            g2.fill(rr);

            Shape oldClip = g2.getClip();
            g2.setClip(rr);
            GradientPaint gp = new GradientPaint(0, 0, INK_BLUE, 0, h, INK_INDIGO);
            g2.setPaint(gp);
            g2.fillRect(0, 0, 5, h);
            g2.setClip(oldClip);

            g2.setColor(BORDER);
            g2.draw(rr);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class HeaderCard extends JPanel {
        private final Image mascot;

        HeaderCard(Image mascot) {
            this.mascot = mascot;
            setOpaque(false);
            setPreferredSize(new Dimension(900, 142));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            RoundRectangle2D rr = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 24, 24);

            g2.setColor(PANEL);
            g2.fill(rr);

            Shape oldClip = g2.getClip();
            g2.setClip(rr);

            GradientPaint rail = new GradientPaint(0, 0, INK_BLUE, 0, h, INK_INDIGO);
            g2.setPaint(rail);
            g2.fillRect(0, 0, 10, h);

            g2.setClip(oldClip);
            g2.setColor(BORDER);
            g2.draw(rr);

            if (mascot != null) {
                int size = 78;
                int x = w - size - 22;
                int y = (h - size) / 2;

                Image scaled = mascot.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                g2.drawImage(scaled, x, y, null);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class StatusPill extends JLabel {
        private Color bg = new Color(100, 116, 139, 30);
        private Color border = new Color(148, 163, 184, 55);

        StatusPill() {
            setOpaque(false);
            setBorder(new EmptyBorder(8, 12, 8, 12));
            setFont(getFont().deriveFont(Font.BOLD, 12f));
            setStateOff();
        }

        void setStateOff() {
            setText("WYŁĄCZONY");
            setForeground(MUTED);
            bg = new Color(100, 116, 139, 28);
            border = new Color(148, 163, 184, 50);
            repaint();
        }

        void setStateOn(String text) {
            setText(text);
            setForeground(SUCCESS);
            bg = new Color(34, 197, 94, 34);
            border = new Color(34, 197, 94, 60);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(bg);
            g2.fillRoundRect(0, 0, w, h, 999, 999);

            g2.setColor(border);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 999, 999);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}

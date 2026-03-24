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
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class HeraldConnectorFrame extends JFrame {

    private static final Color INK_BLUE = Color.decode("#0071e3");

    private static final Color BG = Color.decode("#f5f5f7");
    private static final Color PANEL = Color.decode("#ffffff");
    private static final Color PANEL_2 = Color.decode("#f9f9fb");
    private static final Color PANEL_3 = Color.decode("#f2f2f7");

    private static final Color TEXT = Color.decode("#424245");
    private static final Color TEXT_2 = Color.decode("#1d1d1f");
    private static final Color MUTED = Color.decode("#6e6e73");

    private static final Color BORDER = Color.decode("#e5e5ea");
    private static final Color SUCCESS = Color.decode("#248a3d");
    private static final Color DANGER = Color.decode("#c9342c");
    private static final Color SHADOW = new Color(15, 23, 42, 10);
    private static final Color TINT = new Color(0, 113, 227, 10);
    private static final Color TINT_BORDER = new Color(0, 113, 227, 28);
    private static final int HEADER_CARD_HEIGHT = 140;
    private static final int TOP_ROW_HEIGHT = 420;
    private static final int LOGS_CARD_HEIGHT = 320;

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
    private JLabel connectionValue;
    private JLabel tokenStateValue;

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
    }

    private static void setupLookAndFeelOnce() {
        if (lafInited) {
            return;
        }
        lafInited = true;

        FlatLightLaf.setup();

        UIManager.put("Component.arc", 18);
        UIManager.put("Button.arc", 20);
        UIManager.put("TextComponent.arc", 16);
        UIManager.put("Component.focusWidth", 0);
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

        setContentPane(buildScrollablePage(loadImage("/assets/herald-mascot.png")));
    }

    private JComponent buildScrollablePage(Image mascot) {
        ScrollableContentPanel page = new ScrollableContentPanel();
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBorder(new EmptyBorder(24, 24, 24, 24));
        page.setOpaque(true);
        page.setBackground(BG);

        JComponent headerCard = lockToFixedHeight(buildHeaderCard(mascot), HEADER_CARD_HEIGHT);
        headerCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComponent topRow = lockToFixedHeight(buildTopRow(), TOP_ROW_HEIGHT);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComponent logsCard = lockToFixedHeight(buildLogsCard(), LOGS_CARD_HEIGHT);
        logsCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        page.add(headerCard);
        page.add(Box.createVerticalStrut(18));
        page.add(topRow);
        page.add(Box.createVerticalStrut(18));
        page.add(logsCard);

        JScrollPane scroll = new JScrollPane(
                page,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(true);
        scroll.setBackground(BG);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel buildHeaderCard(Image mascot) {
        HeaderCard header = new HeaderCard(mascot);
        header.setLayout(new BorderLayout(24, 0));
        header.setBorder(new EmptyBorder(22, 24, 22, 116));

        JPanel textBox = new JPanel();
        textBox.setOpaque(false);
        textBox.setLayout(new BoxLayout(textBox, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("HERALD CONNECTOR");
        eyebrow.setForeground(INK_BLUE);
        eyebrow.setFont(eyebrow.getFont().deriveFont(Font.BOLD, 11f));
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Herald Copilot Connector");
        title.setForeground(TEXT_2);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 29f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Proste połączenie Heralda z GitHub Copilot");
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 14f));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        textBox.add(eyebrow);
        textBox.add(Box.createVerticalStrut(8));
        textBox.add(title);
        textBox.add(Box.createVerticalStrut(6));
        textBox.add(subtitle);

        header.add(textBox, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildTopRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);
        row.add(buildControlsCard());
        row.add(buildOverviewCard());
        return row;
    }

    private JPanel buildControlsCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(new EmptyBorder(22, 22, 22, 22));

        card.add(sectionHeader(
                "Połącz z GitHub Copilot",
                "Wklej token, zdecyduj czy ma zostać zapisany i włącz connector."
        ), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

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

        persistToken = new JCheckBox("Zapisz token na tym komputerze");
        styleCheckBox(persistToken);
        persistToken.setToolTipText("Token zostanie zapisany lokalnie w profilu użytkownika.");

        deleteSavedTokenBtn = new JButton("Usuń zapisany token");
        styleGhostButton(deleteSavedTokenBtn);
        deleteSavedTokenBtn.setForeground(DANGER);
        lockButtonToTextWidth(deleteSavedTokenBtn, 20);
        deleteSavedTokenBtn.addActionListener(e -> deleteSavedToken());

        startBtn = new JButton("Połącz connector");
        stylePrimaryButton(startBtn);
        startBtn.addActionListener(e -> start());

        stopBtn = new JButton("Wyłącz connector");
        styleSecondaryButton(stopBtn);
        stopBtn.addActionListener(e -> stop());

        JLabel tokenLabel = fieldLabel("Token GitHub Copilot");
        tokenLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel tokenRow = new JPanel(new BorderLayout(12, 0));
        tokenRow.setOpaque(false);
        tokenRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        tokenRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                Math.max(tokenField.getPreferredSize().height, showToken.getPreferredSize().height)));
        tokenRow.add(tokenField, BorderLayout.CENTER);
        tokenRow.add(showToken, BorderLayout.EAST);

        JLabel permissionHint = metaLabel("Użyj Fine-Grained PAT z uprawnieniem Copilot requests.");
        permissionHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel saveHint = metaLabel(
                DpapiCrypto.isWindows()
                        ? "Po zaznaczeniu token zostanie zapisany tylko na tym komputerze."
                        : "Na tym systemie token działa tylko w bieżącej sesji."
        );
        saveHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel saveInfoColumn = new JPanel();
        saveInfoColumn.setOpaque(false);
        saveInfoColumn.setLayout(new BoxLayout(saveInfoColumn, BoxLayout.Y_AXIS));
        saveInfoColumn.add(persistToken);
        saveInfoColumn.add(Box.createVerticalStrut(6));
        saveInfoColumn.add(saveHint);

        JPanel saveActionsRow = new JPanel(new GridBagLayout()) {
            @Override
            public Dimension getMaximumSize() {
                Dimension preferred = getPreferredSize();
                return new Dimension(Integer.MAX_VALUE, preferred.height);
            }
        };
        saveActionsRow.setOpaque(false);
        saveActionsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints saveInfoConstraints = new GridBagConstraints();
        saveInfoConstraints.gridx = 0;
        saveInfoConstraints.gridy = 0;
        saveInfoConstraints.weightx = 1;
        saveInfoConstraints.fill = GridBagConstraints.HORIZONTAL;
        saveInfoConstraints.anchor = GridBagConstraints.NORTHWEST;
        saveInfoConstraints.insets = new Insets(0, 0, 0, 12);

        GridBagConstraints deleteButtonConstraints = new GridBagConstraints();
        deleteButtonConstraints.gridx = 1;
        deleteButtonConstraints.gridy = 0;
        deleteButtonConstraints.weightx = 0;
        deleteButtonConstraints.fill = GridBagConstraints.NONE;
        deleteButtonConstraints.anchor = GridBagConstraints.NORTHEAST;

        saveActionsRow.add(saveInfoColumn, saveInfoConstraints);
        saveActionsRow.add(deleteSavedTokenBtn, deleteButtonConstraints);

        JPanel actionRow = new JPanel(new GridLayout(1, 2, 10, 0));
        actionRow.setOpaque(false);
        actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, startBtn.getPreferredSize().height));
        actionRow.add(startBtn);
        actionRow.add(stopBtn);

        content.add(tokenLabel);
        content.add(Box.createVerticalStrut(8));
        content.add(tokenRow);
        content.add(Box.createVerticalStrut(8));
        content.add(permissionHint);
        content.add(Box.createVerticalStrut(18));
        content.add(saveActionsRow);
        content.add(Box.createVerticalStrut(22));
        content.add(actionRow);
        content.add(Box.createVerticalGlue());

        card.add(content, BorderLayout.CENTER);

        updatePersistControls();
        return card;
    }

    private JPanel buildOverviewCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(new EmptyBorder(22, 22, 22, 22));

        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);

        top.add(sectionHeader(
                "Status połączenia",
                "Od razu widać, czy connector jest gotowy i czy połączenie działa."
        ), BorderLayout.CENTER);

        statusPill = new StatusPill();
        statusPill.setStateOff();
        top.add(statusPill, BorderLayout.EAST);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        stateHeadline = new JLabel("Connector wyłączony");
        stateHeadline.setForeground(TEXT_2);
        stateHeadline.setFont(stateHeadline.getFont().deriveFont(Font.BOLD, 24f));
        stateHeadline.setAlignmentX(Component.LEFT_ALIGNMENT);

        connectionValue = statusValueLabel("");
        tokenStateValue = statusValueLabel("");

        JPanel summaryRow = new JPanel(new GridLayout(1, 2, 12, 0));
        summaryRow.setOpaque(false);
        summaryRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        summaryRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        summaryRow.add(statusTile("Połączenie", connectionValue));
        summaryRow.add(statusTile("Token", tokenStateValue));

        content.add(lockToPreferredHeight(stateHeadline));
        content.add(Box.createVerticalStrut(16));
        content.add(lockToPreferredHeight(summaryRow));
        content.add(Box.createVerticalStrut(18));
        JComponent guideBanner = buildGuideBanner();
        guideBanner.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(guideBanner);
        content.add(Box.createVerticalGlue());

        card.add(top, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildLogsCard() {
        CardPanel logsWrap = new CardPanel();
        logsWrap.setLayout(new BorderLayout(0, 18));
        logsWrap.setBorder(new EmptyBorder(22, 22, 22, 22));

        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        top.add(sectionHeader(
                "Logi",
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
        logs.setBackground(PANEL_2);
        logs.setForeground(TEXT_2);
        logs.setMargin(new Insets(12, 12, 12, 12));
        logs.setBorder(BorderFactory.createEmptyBorder());

        JScrollPane scroll = new JScrollPane(logs);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        scroll.getViewport().setBackground(PANEL_2);
        scroll.setPreferredSize(new Dimension(200, 260));

        logsWrap.add(top, BorderLayout.NORTH);
        logsWrap.add(scroll, BorderLayout.CENTER);
        return logsWrap;
    }

    private JPanel buildSecurityBanner() {
        InfoBannerPanel banner = new InfoBannerPanel();
        banner.setLayout(new BorderLayout(0, 6));
        banner.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel title = new JLabel("Przechowywanie tokena");
        title.setForeground(TEXT_2);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));

        String bodyText = DpapiCrypto.isWindows()
                ? "Token może zostać zapisany lokalnie. Bez zapisu pozostaje tylko w pamięci bieżącej sesji."
                : "Na tym systemie token działa wyłącznie w pamięci bieżącej sesji.";

        JLabel body = htmlLabel(bodyText);
        body.setForeground(MUTED);

        banner.add(title, BorderLayout.NORTH);
        banner.add(body, BorderLayout.CENTER);
        return banner;
    }

    private JPanel buildGuideBanner() {
        InfoBannerPanel banner = new InfoBannerPanel();
        banner.setLayout(new BorderLayout(0, 6));
        banner.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel title = new JLabel("Jak zdobyć token");
        title.setForeground(TEXT_2);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));

        JEditorPane body = clickableHtml(
                "1. Otwórz GitHub: Settings -> Developer settings -> Personal access tokens -> Fine-grained tokens" +
                        "<br>lub wejdź: <a href='https://github.com/settings/personal-access-tokens'>" +
                        "https://github.com/settings/personal-access-tokens</a><br>" +
                        "2. Utwórz nowy token i dodaj uprawnienie <b>Copilot requests</b>.<br>" +
                        "3. Skopiuj token, wklej go tutaj i kliknij <b>Połącz connector</b>."
        );

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
                    append("Token został zapisany na tym komputerze.");
                } catch (IOException ioe) {
                    throw new IllegalArgumentException("Nie udało się zapisać tokena lokalnie: " + ioe.getMessage());
                }
            }

            state.enableWithToken(token);
            append("Connector został połączony.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Niepoprawny token", JOptionPane.WARNING_MESSAGE);
        }

        refreshFromState();
    }

    private void stop() {
        state.disableAndClearToken();
        append("Connector został wyłączony.");
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
            statusPill.setStateOn("POŁĄCZONY");
            stateHeadline.setText("Connector działa");
            stateHeadline.setForeground(TEXT_2);
            connectionValue.setText("Połączony");
        } else if (hasToken) {
            statusPill.setStateReady("GOTOWY");
            stateHeadline.setText("Token jest gotowy");
            stateHeadline.setForeground(TEXT_2);
            connectionValue.setText("Gotowy do połączenia");
        } else {
            statusPill.setStateOff();
            stateHeadline.setText("Czeka na połączenie");
            stateHeadline.setForeground(TEXT_2);
            connectionValue.setText("Niepołączony");
        }

        tokenStateValue.setText(describeTokenState(hasToken));
    }

    private String describeTokenState(boolean hasToken) {
        if (!DpapiCrypto.isWindows()) {
            return hasToken ? "Tylko ta sesja" : "Jeszcze nie wklejony";
        }
        if (tokenStore.exists()) {
            return "Zapisany na tym komputerze";
        }
        if (persistToken != null && persistToken.isSelected()) {
            return hasToken ? "Zapisze się po połączeniu" : "Będzie zapisany po wklejeniu";
        }
        return hasToken ? "Tylko w tej sesji" : "Jeszcze nie wklejony";
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
            deleteSavedTokenBtn.setVisible(canPersist && hasSaved);
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
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JLabel subtitle = htmlLabel(subtitleText);
        subtitle.setForeground(MUTED);
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 13f));

        panel.add(title);
        panel.add(subtitle);
        return panel;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_2);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12.5f));
        return label;
    }

    private JLabel metaLabel(String text) {
        JLabel label = htmlLabel(text);
        label.setForeground(MUTED);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));
        return label;
    }

    private JLabel statusValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_2);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));
        label.setVerticalAlignment(SwingConstants.TOP);
        return label;
    }

    private JPanel statusTile(String titleText, JLabel value) {
        InfoBannerPanel tile = new InfoBannerPanel();
        tile.setLayout(new BorderLayout(0, 6));
        tile.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel title = new JLabel(titleText);
        title.setForeground(MUTED);
        title.setFont(title.getFont().deriveFont(Font.PLAIN, 12f));

        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        tile.add(title, BorderLayout.NORTH);
        tile.add(value, BorderLayout.CENTER);
        return tile;
    }

    private <T extends JComponent> T lockToPreferredHeight(T component) {
        Dimension preferred = component.getPreferredSize();
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
        return component;
    }

    private <T extends JComponent> T lockToFixedHeight(T component, int height) {
        component.setMinimumSize(new Dimension(320, height));
        component.setPreferredSize(new Dimension(320, height));
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return component;
    }

    private JButton lockButtonToTextWidth(JButton button, int horizontalPadding) {
        button.putClientProperty("JButton.minimumWidth", 0);

        FontMetrics metrics = button.getFontMetrics(button.getFont());
        Insets insets = button.getInsets();
        int width = metrics.stringWidth(button.getText())
                + insets.left
                + insets.right
                + horizontalPadding;
        int height = button.getPreferredSize().height;

        Dimension size = new Dimension(width, height);
        button.setMinimumSize(size);
        button.setPreferredSize(size);
        button.setMaximumSize(size);
        return button;
    }

    private JLabel htmlLabel(String text) {
        return new JLabel("<html>" + text + "</html>");
    }

    private JEditorPane clickableHtml(String text) {
        JEditorPane pane = new JEditorPane();
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setFocusable(false);
        pane.setOpaque(false);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pane.setText(
                "<html><body style='margin:0; font-family:\"Segoe UI\"; font-size:12px; color:" + cssColor(MUTED) + ";'>" +
                        text +
                        "</body></html>"
        );
        pane.addHyperlinkListener(event -> {
            if (event.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                pane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else if (event.getEventType() == HyperlinkEvent.EventType.EXITED) {
                pane.setCursor(Cursor.getDefaultCursor());
            } else if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED && event.getURL() != null) {
                openExternalLink(event.getURL().toString());
            }
        });
        return pane;
    }

    private String cssColor(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private void openExternalLink(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception e) {
            append("Nie udało się otworzyć linku: " + url);
        }

        Toolkit.getDefaultToolkit().beep();
        JOptionPane.showMessageDialog(
                this,
                "Nie udało się otworzyć przeglądarki. Otwórz link ręcznie:\n" + url,
                "Otwórz link",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void styleCheckBox(JCheckBox checkBox) {
        checkBox.setOpaque(false);
        checkBox.setForeground(TEXT);
        checkBox.setFocusPainted(false);
        checkBox.setBorder(new EmptyBorder(4, 0, 4, 0));
    }

    private void stylePrimaryButton(JButton button) {
        button.setBackground(INK_BLUE);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 91, 190), 1, true),
                new EmptyBorder(11, 18, 11, 18)
        ));
    }

    private void styleSecondaryButton(JButton button) {
        button.setBackground(PANEL);
        button.setForeground(TEXT_2);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(11, 18, 11, 18)
        ));
    }

    private void styleGhostButton(JButton button) {
        button.setBackground(PANEL_2);
        button.setForeground(TEXT);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(10, 16, 10, 16)
        ));
    }

    static class ScrollableContentPanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(visibleRect.height - 32, 32);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
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
            RoundRectangle2D rr = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 26, 26);

            g2.setColor(SHADOW);
            g2.fillRoundRect(1, 4, w - 3, h - 5, 26, 26);
            g2.setColor(PANEL);
            g2.fill(rr);
            g2.setColor(BORDER);
            g2.draw(rr);

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
            g2.setColor(TINT);
            g2.fill(rr);
            g2.setColor(TINT_BORDER);
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
            setPreferredSize(new Dimension(900, 140));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            RoundRectangle2D rr = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 24, 24);

            g2.setColor(SHADOW);
            g2.fillRoundRect(1, 4, w - 3, h - 5, 24, 24);
            g2.setColor(PANEL);
            g2.fill(rr);
            g2.setColor(BORDER);
            g2.draw(rr);

            if (mascot != null) {
                int size = 72;
                int x = w - size - 28;
                int y = (h - size) / 2;

                Image scaled = mascot.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                g2.drawImage(scaled, x, y, null);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class StatusPill extends JLabel {
        private Color bg = new Color(110, 110, 115, 18);
        private Color border = new Color(110, 110, 115, 30);

        StatusPill() {
            setOpaque(false);
            setBorder(new EmptyBorder(8, 14, 8, 14));
            setFont(getFont().deriveFont(Font.BOLD, 11.5f));
            setStateOff();
        }

        void setStateOff() {
            setText("OCZEKUJE");
            setForeground(MUTED);
            bg = new Color(110, 110, 115, 18);
            border = new Color(110, 110, 115, 32);
            repaint();
        }

        void setStateReady(String text) {
            setText(text);
            setForeground(INK_BLUE);
            bg = new Color(0, 113, 227, 18);
            border = new Color(0, 113, 227, 32);
            repaint();
        }

        void setStateOn(String text) {
            setText(text);
            setForeground(SUCCESS);
            bg = new Color(36, 138, 61, 18);
            border = new Color(36, 138, 61, 32);
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

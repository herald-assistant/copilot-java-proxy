package com.acme.herald.copilot.ui;

import com.acme.herald.copilot.core.ConnectorState;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.swing.*;

@Component
public class UiBootstrap {

    @Autowired
    private ApplicationContext ctx;
    @Autowired
    private ConnectorState state;

    @Value("${server.port:8788}")
    private int port;

    @PostConstruct
    public void startUi() {
        SwingUtilities.invokeLater(() -> {
            var frame = new HeraldConnectorFrame(state, port, () -> {
                // zamykanie aplikacji
                System.exit(0);
            });
            frame.setVisible(true);
        });
    }
}
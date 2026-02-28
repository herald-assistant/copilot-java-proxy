package com.acme.herald.copilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;

@SpringBootApplication
public class HeraldCopilotProxyApplication {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        var app = new SpringApplication(HeraldCopilotProxyApplication.class);

        var props = new HashMap<String, Object>();
        props.put("spring.main.headless", "false");
        props.put("server.port", "8788");
        app.setDefaultProperties(props);

        app.run(args);
    }
}
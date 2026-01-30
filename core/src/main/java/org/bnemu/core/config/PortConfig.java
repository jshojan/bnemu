package org.bnemu.core.config;

public class PortConfig {
    private int port;
    private String host = "127.0.0.1";

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}

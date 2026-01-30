package org.bnemu.core.config;

public class ServerConfig {
    private PortConfig bncs;
    private PortConfig d2cs;
    private PortConfig d2gs;
    private PortConfig telnet;

    public PortConfig getBncs() {
        return bncs;
    }

    public void setBncs(PortConfig bncs) {
        this.bncs = bncs;
    }

    public PortConfig getD2cs() {
        return d2cs;
    }

    public void setD2cs(PortConfig d2cs) {
        this.d2cs = d2cs;
    }

    public PortConfig getD2gs() {
        return d2gs;
    }

    public void setD2gs(PortConfig d2gs) {
        this.d2gs = d2gs;
    }

    public PortConfig getTelnet() {
        return telnet;
    }

    public void setTelnet(PortConfig telnet) {
        this.telnet = telnet;
    }
}

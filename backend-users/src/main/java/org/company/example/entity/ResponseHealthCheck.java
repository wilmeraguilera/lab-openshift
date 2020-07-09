package org.company.example.entity;

public class ResponseHealthCheck {

    private String appTitle;

    private String version;

    private String IP;

    public ResponseHealthCheck() {
        super();
    }

    public String getAppTitle() {
        return appTitle;
    }

    public void setAppTitle(String appTitle) {
        this.appTitle = appTitle;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }
}

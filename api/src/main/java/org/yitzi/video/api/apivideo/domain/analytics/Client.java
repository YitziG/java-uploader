package org.yitzi.video.api.apivideo.domain.analytics;

public class Client {
    public final String type;
    public final String name;
    public final String version;

    public Client(String type, String name, String version) {
        this.type    = type;
        this.name    = name;
        this.version = version;
    }
}
package org.bnemu.core.config;

public class CoreConfig {
    private MongoConfig mongo;
    private ServerConfig server;

    public MongoConfig getMongo() {
        return mongo;
    }

    public void setMongo(MongoConfig mongo) {
        this.mongo = mongo;
    }

    public ServerConfig getServer() {
        return server;
    }

    public void setServer(ServerConfig server) {
        this.server = server;
    }
}

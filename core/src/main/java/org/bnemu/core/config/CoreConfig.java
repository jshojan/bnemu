package org.bnemu.core.config;

public class CoreConfig {
    private MongoConfig mongo;
    private ServerConfig server;
    private RealmConfig realm = new RealmConfig();
    private BnftpConfig bnftp = new BnftpConfig();

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

    public RealmConfig getRealm() {
        return realm;
    }

    public void setRealm(RealmConfig realm) {
        this.realm = realm;
    }

    public BnftpConfig getBnftp() {
        return bnftp;
    }

    public void setBnftp(BnftpConfig bnftp) {
        this.bnftp = bnftp;
    }
}

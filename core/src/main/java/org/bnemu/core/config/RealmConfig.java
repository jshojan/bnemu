package org.bnemu.core.config;

public class RealmConfig {
    private String name = "bnemu";
    private String description = "bnemu Realm";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

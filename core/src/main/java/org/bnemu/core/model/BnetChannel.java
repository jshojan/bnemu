package org.bnemu.core.model;

import java.util.Set;

public class BnetChannel {
    private String name;
    private Set<String> users;
    private String topic;
    private boolean isPrivate;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<String> getUsers() { return users; }
    public void setUsers(Set<String> users) { this.users = users; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean aPrivate) { isPrivate = aPrivate; }
}
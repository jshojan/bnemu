package org.bnemu.core.model;

import java.time.Instant;

public class Account {
    private String id;
    private String username;
    private String passwordHash; // legacy string hex hash, optional
    private byte[] passwordHashBytes; // raw binary password hash
    private String email;
    private Instant createDate;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public byte[] getPasswordHashBytes() { return passwordHashBytes; }
    public void setPasswordHashBytes(byte[] passwordHashBytes) { this.passwordHashBytes = passwordHashBytes; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Instant getCreateDate() { return createDate; }
    public void setCreateDate(Instant createDate) { this.createDate = createDate; }
}

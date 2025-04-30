package org.bnemu.core.model;

import java.time.Instant;

public class ChatMessage {
    private String channel;
    private String sender;
    private String text;
    private Instant timestamp;

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
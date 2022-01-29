package com.example.chatapp.models;

public class Chats {
    private String sender;
    private String receiver;
    private String message;
    private boolean isSeen;

    public Chats() {
    }

    public Chats(String sender, String receiver, String message, boolean isSeen) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.isSeen = isSeen;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSeen() {
        return isSeen;
    }
}

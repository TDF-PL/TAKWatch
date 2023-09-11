package com.atakmap.android.selfmarkerdata.debouncer;

import java.util.List;

public class MessageDebouncer {

    private LastMessage lastMessage = null;

    private static final int DEBOUNCE_TIME_MS = 1000;

    public boolean alreadyHandled(List<Object> messages) {
        if (lastMessage == null) {
            return false;
        }
        return messages.hashCode() == lastMessage.getHash() && System.currentTimeMillis() - lastMessage.getTime() < DEBOUNCE_TIME_MS;
    }

    public void remember(List<Object> messages) {
        lastMessage = new LastMessage(messages.hashCode());
    }

}

class LastMessage {
    private final int hash;
    private final long time;

    public LastMessage(int hash) {
        this.hash = hash;
        this.time = System.currentTimeMillis();
    }

    public int getHash() {
        return hash;
    }

    public long getTime() {
        return time;
    }
}
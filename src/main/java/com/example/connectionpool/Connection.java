package com.example.connectionpool;

import java.time.LocalDateTime;

public class Connection {
    private String ipAddress;
    private LocalDateTime initiationTime;
    private LocalDateTime lastUseTime;
    Connection(LocalDateTime initiationTime){
        this.initiationTime = initiationTime;
        this.lastUseTime = initiationTime;
    }
    public LocalDateTime getInitiationTime() {
        return initiationTime;
    }
    public LocalDateTime getLastUseTime() {
        return lastUseTime;
    }

    public void setLastUseTime(LocalDateTime lastUseTime) {
        this.lastUseTime = lastUseTime;
    }
}

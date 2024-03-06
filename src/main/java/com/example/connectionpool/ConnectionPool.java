package com.example.connectionpool;

import org.springframework.cglib.core.Local;
import org.springframework.web.client.ResourceAccessException;

import java.nio.channels.AcceptPendingException;
import java.rmi.AccessException;
import java.sql.Time;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class ConnectionPool {
    int MAX_CONNECTIONS;
    int MIN_CONNECTIONS;
    int TIME_TO_LEAVE_IN_SECONDS;
    int alive_connections;
    int CONNECTION_REQUEST_TIMEOUT_IN_MILLISECONDS;
    private TreeSet<Connection> availableConnections;
    private TreeSet<Connection> usedConnections;
    final Object connectionAvailabilityMonitor = new Object();
    boolean didTimeExpire;

    ConnectionPool(int MAX_CONNECTIONS,int MIN_CONNECTIONS,int TIME_TO_LEAVE_IN_SECONDS,int CONNECTION_REQUEST_TIMEOUT_IN_MILLISECONDS){
        this.MAX_CONNECTIONS = MAX_CONNECTIONS;
        this.MIN_CONNECTIONS = MIN_CONNECTIONS;
        this.TIME_TO_LEAVE_IN_SECONDS = TIME_TO_LEAVE_IN_SECONDS;
        this.CONNECTION_REQUEST_TIMEOUT_IN_MILLISECONDS = CONNECTION_REQUEST_TIMEOUT_IN_MILLISECONDS;
        initialiseConnections();
    }

    public  Connection getConnection() throws InterruptedException, AccessException {
        synchronized (connectionAvailabilityMonitor) {
            expireConnections();
            Connection allotConnection;
            if (!availableConnections.isEmpty()) {
                allotConnection = availableConnections.pollFirst();
            } else if (this.alive_connections < this.MAX_CONNECTIONS) {
                allotConnection = new Connection(LocalDateTime.now());
                alive_connections++;
            } else {
                didTimeExpire = true;
                connectionAvailabilityMonitor.wait(this.CONNECTION_REQUEST_TIMEOUT_IN_MILLISECONDS);
                //waitForConnection();
                if (didTimeExpire) {
                    throw new AccessException("Unable to get connection");
                }
                allotConnection = availableConnections.pollFirst();
            }
            usedConnections.add(allotConnection);
            updateConnectionAccessTime(allotConnection);
            return allotConnection;
        }
    }

    public void releaseConnection(Connection connection) throws InterruptedException, IllegalArgumentException{
        if( connection == null ||  !usedConnections.contains(connection)) throw new IllegalArgumentException();
        synchronized (connectionAvailabilityMonitor) {
            usedConnections.remove(connection);
            availableConnections.add(connection);
            connectionAvailabilityMonitor.notify();
            didTimeExpire = false;
        }
        //notifyConnectionAvailable();
    }

    void initialiseConnections(){
        availableConnections = new TreeSet<>(Comparator.comparing(Connection::getLastUseTime));
        usedConnections = new TreeSet<>(Comparator.comparing(Connection::getLastUseTime));
        for(int i = 0;i<MIN_CONNECTIONS;i++) {
            availableConnections.add(new Connection(LocalDateTime.now()));
            alive_connections++;
        }
    }

    private void expireConnections(){
        while(!availableConnections.isEmpty() && checkConnectionExpired(availableConnections.first())) {
            availableConnections.pollFirst();
            alive_connections--;
        }
    }

    private void updateConnectionAccessTime(Connection connection){
        connection.setLastUseTime(LocalDateTime.now());
    }



    public int getAlive_connections() {
        return alive_connections;
    }

    public int getAvailableConnections() {
        return availableConnections.size();
    }

    public boolean checkConnectionExpired(Connection connection){
        long duration = Duration.between(connection.getLastUseTime(),LocalDateTime.now()).toSeconds();
        return duration > this.TIME_TO_LEAVE_IN_SECONDS;
    }
}

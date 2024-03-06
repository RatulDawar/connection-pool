package com.example.connectionpool;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.BeforeEach;

import java.rmi.AccessException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConnectionPoolApplicationTests {
    private ConnectionPool connectionPool;
    int MIN_CONNECTIONS;
    int MAX_CONNECTIONS;
    int TIME_TO_LEAVE_IN_SECONDS;
    int CONECTION_REQUEST_TIMEOUT_IN_MILLISECONDS;
    @BeforeEach
    public void setUp() {
        this.MAX_CONNECTIONS = 10;
        this.MIN_CONNECTIONS = 5;
        this.TIME_TO_LEAVE_IN_SECONDS = 20;
        this.CONECTION_REQUEST_TIMEOUT_IN_MILLISECONDS = 10000;
        // Initialize the connection pool with 5 connections
        connectionPool = new ConnectionPool(MAX_CONNECTIONS, MIN_CONNECTIONS, TIME_TO_LEAVE_IN_SECONDS,CONECTION_REQUEST_TIMEOUT_IN_MILLISECONDS);
    }
//
    @Test
    public void testGetConnection() throws InterruptedException, AccessException {
        Connection connection = connectionPool.getConnection();
        // Check if a connection is obtained successfully
        assertTrue(connection != null && connection.getLastUseTime() != null);
    }

    @Test
    public void testReleaseConnection() throws InterruptedException, AccessException {
        Connection connection = connectionPool.getConnection();
        // Release the connection
        connectionPool.releaseConnection(connection);
        // Check if the connection is released and available again
        assertEquals(5, connectionPool.getAvailableConnections());
    }

    @Test
    public void testMaxConnections() throws InterruptedException, AccessException {
        // Obtain maximum connections
        for (int i = 0; i < MAX_CONNECTIONS; i++) {
            connectionPool.getConnection();
        }
        // Try to get one more connection, it should not be available
        try {
            Connection extraConnection = connectionPool.getConnection();
        }catch(AccessException accessException){
            assertEquals("Unable to get connection",accessException.getMessage());
        }

    }

    @Test
    public void testConnectionExpiration() throws InterruptedException, AccessException {

        Thread.sleep(this.TIME_TO_LEAVE_IN_SECONDS* 1000L + 3000);
        Connection connection = connectionPool.getConnection();
        assert(connectionPool.getAlive_connections() == 1);
    }
    
    // TODO fix this test case
    @Test
    public void testConcurrentAccessExcess() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(this.MAX_CONNECTIONS + 1);
        AtomicBoolean didAssertionSucceed = new AtomicBoolean(false);
        for (int i = 0; i < this.MAX_CONNECTIONS + 1;i++) {
            executor.submit(() -> {
                try {
                    Connection connection = connectionPool.getConnection();
                    Thread.sleep(this.CONECTION_REQUEST_TIMEOUT_IN_MILLISECONDS + 10000);
                    connectionPool.releaseConnection(connection);
                } catch (InterruptedException | AccessException e) {
                    didAssertionSucceed.set(e.getMessage().equals("Unable to get connection"));

                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        assert(didAssertionSucceed.get());

    }

    @Test
    public void testConcurrentAccessAvailable() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(this.MAX_CONNECTIONS + 1);
        AtomicBoolean didAssertionSucceed = new AtomicBoolean(false);
        for (int i = 0; i < this.MAX_CONNECTIONS + 1;i++) {
            executor.submit(() -> {
                try {
                    Connection connection = connectionPool.getConnection();
                    // keep connection timeout more than 5000 for this tc
                    Thread.sleep(this.CONECTION_REQUEST_TIMEOUT_IN_MILLISECONDS - 5000);
                    connectionPool.releaseConnection(connection);
                } catch (InterruptedException | AccessException e) {
                    didAssertionSucceed.set(e.getMessage().equals("Unable to get connection"));

                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        assert(!didAssertionSucceed.get());

    }


    @Test
    void contextLoads() {
    }

}

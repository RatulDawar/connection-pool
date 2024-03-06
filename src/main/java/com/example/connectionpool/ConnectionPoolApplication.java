package com.example.connectionpool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController



public class ConnectionPoolApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConnectionPoolApplication.class, args);
    }


}

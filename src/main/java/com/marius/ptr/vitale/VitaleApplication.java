package com.marius.ptr.vitale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VitaleApplication {

    public static void main(String[] args) {
        SpringApplication.run(VitaleApplication.class, args);
    }

}

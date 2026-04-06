package com.marius.ptr.vitale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJdbcAuditing
@EnableJdbcRepositories(basePackages = "com.marius.ptr.vitale.domain")
public class VitaleApplication {

    public static void main(String[] args) {
        SpringApplication.run(VitaleApplication.class, args);
    }

}

package cl.tarrobuild.compatibility;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class CompatibilityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompatibilityServiceApplication.class, args);
    }

}

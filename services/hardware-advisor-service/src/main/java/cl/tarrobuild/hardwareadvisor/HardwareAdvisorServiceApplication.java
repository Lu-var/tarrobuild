package cl.tarrobuild.hardwareadvisor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class HardwareAdvisorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HardwareAdvisorServiceApplication.class, args);
    }

}

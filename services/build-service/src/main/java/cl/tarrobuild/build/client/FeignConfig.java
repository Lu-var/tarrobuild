package cl.tarrobuild.build.client;

import feign.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class FeignConfig {

    @Bean
    public Request.Options feignOptions(
            @Value("${feign.client.connect-timeout:5}") int connectTimeout,
            @Value("${feign.client.read-timeout:10}") int readTimeout) {
        return new Request.Options(connectTimeout, TimeUnit.SECONDS, readTimeout, TimeUnit.SECONDS, true);
    }
}

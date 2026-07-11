package cl.tarrobuild.estimate.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder(
            @Value("${restclient.connect-timeout:5000}") int connectTimeout,
            @Value("${restclient.read-timeout:10000}") int readTimeout) {
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withTimeouts(Duration.ofMillis(connectTimeout), Duration.ofMillis(readTimeout));
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.simple().build(settings);
        return RestClient.builder()
                .requestFactory(requestFactory);
    }
}

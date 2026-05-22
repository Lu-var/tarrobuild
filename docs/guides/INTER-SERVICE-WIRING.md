# Inter-Service Wiring Guide

## When to use which

| Tool | When |
|------|------|
| **RestClient** | Single-endpoint calls (e.g. product-service -> category-service to validate 1 category) |
| **Feign Client** | Multi-endpoint orchestration (e.g. hardware-advisor -> 4 services, multiple calls per request) |

---

## RestClient Pattern (product-service -> category-service)

### 1. Add config bean

```java
// config/RestClientConfig.java
@Configuration
public class RestClientConfig {
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
```

### 2. Create the client

```java
// client/CategoryRestClient.java
@Service
@Slf4j
public class CategoryRestClient {

    private final RestClient restClient;

    public CategoryRestClient(RestClient.Builder builder,
                               @Value("${category-service.url}") String categoryServiceUrl) {
        this.restClient = builder
                .baseUrl(categoryServiceUrl)
                .build();
    }

    public CategoryClientResponse getCategoryById(Long id) {
        log.info("Calling category-service: GET /api/categories/{}", id);
        return restClient.get()
                .uri("/api/categories/{id}", id)
                .retrieve()
                .body(CategoryClientResponse.class);
    }
}
```

### 3. Define the response DTO

```java
// dto/CategoryClientResponse.java
public record CategoryClientResponse(
        Long id,
        String name,
        String slug,
        String description,
        Boolean isActive
) {}
```

### 4. Use in service

```java
// service/CategoryValidationService.java
@Service
@Slf4j
public class CategoryValidationService {

    private final CategoryRestClient client;

    public CategoryValidationService(CategoryRestClient client) {
        this.client = client;
    }

    public void validateCategoryExists(Long categoryId) {
        try {
            CategoryClientResponse response = client.getCategoryById(categoryId);
            log.info("Category validated: id={}, name={}", response.id(), response.name());
        } catch (RestClientException e) {
            throw new EntityNotFoundException("Category with ID " + categoryId + " not found");
        }
    }
}
```

### 5. Configure URL in application.yaml

```yaml
category-service:
  url: ${CATEGORY_SERVICE_URL:http://localhost:8084}
```

### 6. Add env var fallback in .env.example

```
CATEGORY_SERVICE_URL=http://localhost:8084
```

---

## Feign Client Pattern

### 1. Add dependency to pom.xml

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

### 2. Enable Feign in application

```java
@SpringBootApplication
@EnableFeignClients
public class ServiceApplication {
    // ...
}
```

### 3. Create the Feign client

```java
// client/BuildFeignClient.java
@FeignClient(name = "build-service", url = "${build-service.url}")
public interface BuildFeignClient {

    @GetMapping("/api/builds/{id}")
    BuildClientResponse getBuildById(@PathVariable Long id);

    @GetMapping("/api/builds/user/{userId}")
    List<BuildClientResponse> getBuildsByUserId(@PathVariable Long userId);
}
```

### 4. Define response DTO

```java
// dto/BuildClientResponse.java
public record BuildClientResponse(
        Long id,
        Long userId,
        String name,
        String status,
        LocalDateTime createdAt,
        List<BuildItemClientResponse> items
) {}
```

### 5. Use in service

```java
@Service
@Slf4j
public class SomeService {

    private final BuildFeignClient buildClient;
    private final ProductFeignClient productClient;

    public SomeService(BuildFeignClient buildClient, ProductFeignClient productClient) {
        this.buildClient = buildClient;
        this.productClient = productClient;
    }

    public void doSomething(Long buildId) {
        BuildClientResponse build = buildClient.getBuildById(buildId);
        log.info("Fetched build: {} with {} items", build.name(), build.items().size());

        for (BuildItemClientResponse item : build.items()) {
            ProductClientResponse product = productClient.getProductById(item.productId());
            log.info("Product: {} - ${}", product.name(), product.msrp());
        }
    }
}
```

### 6. Configure URLs in application.yaml

```yaml
build-service:
  url: ${BUILD_SERVICE_URL:http://localhost:8087}
product-service:
  url: ${PRODUCT_SERVICE_URL:http://localhost:8083}
compatibility-service:
  url: ${COMPATIBILITY_SERVICE_URL:http://localhost:8085}
notification-service:
  url: ${NOTIFICATION_SERVICE_URL:http://localhost:8090}
```

---

## Key differences

| | RestClient | Feign |
|--|-----------|-------|
| Boilerplate | More (builder, config bean, try/catch) | Less (interface-only) |
| URL config | `@Value` injected in constructor | `@FeignClient(url = "${...}")` annotation |
| Error handling | try/catch `RestClientException` | `FeignClient` + optional `FallbackFactory` |
| Best for | 1-2 endpoints per service | 3+ endpoints or multiple services |

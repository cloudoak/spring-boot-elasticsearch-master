package spring.boot.elasticsearch.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration properties for Boot Elasticsearch REST clients.
 *
 * @author OAK
 * @since 2019/06/24 19:22:00 PM.
 * @version 1.0
 */
@Data
@ConfigurationProperties(prefix = "spring.boot.elasticsearch.rest")
public class RestClientProperties {

    /**
     * Comma-separated list of the AWS Elasticsearch instances to use.
     */
    private List<String> uris = new ArrayList<>(Collections.singletonList("http://localhost:9200"));

    /**
     * Credentials username.
     */
    private String username;

    /**
     * Credentials password.
     */
    private String password;

    /**
     * Connection timeout.
     */
    private Duration connectionTimeout = Duration.ofSeconds(1);

    /**
     * Read timeout.
     */
    private Duration readTimeout = Duration.ofSeconds(30);

    /**
     * Max connection total.
     */
    private Integer maxConnTotal;

    /**
     * Max connection per route.
     */
    private Integer maxConnPerRoute;

}

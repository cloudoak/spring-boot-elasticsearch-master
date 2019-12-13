package spring.boot.elasticsearch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration Search properties for AWS Elasticsearch REST clients.
 *
 * @author OAK
 * @since 2019/06/24 19:22:00 PM.
 * @version 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.boot.elasticsearch.search")
public class ElasticsearchSearchProperties {

    /**
     * Elasticsearch Search Request settings timeout.
     */
    private Integer timeout;

}

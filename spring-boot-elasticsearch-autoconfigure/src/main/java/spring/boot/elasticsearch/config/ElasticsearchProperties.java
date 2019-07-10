package spring.boot.elasticsearch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Configuration properties for AWS Elasticsearch REST clients.
 *
 * @author OAK
 * @since 2019/06/24 19:22:00 PM.
 * @version 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.lenovo.elasticsearch")
public class ElasticsearchProperties {

    /**
     * Elasticsearch index settings collection.
     */
    private Map<String, String> index;

}

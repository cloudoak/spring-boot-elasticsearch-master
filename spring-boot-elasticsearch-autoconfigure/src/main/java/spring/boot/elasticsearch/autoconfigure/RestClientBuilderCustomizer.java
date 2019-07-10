package spring.boot.elasticsearch.autoconfigure;

import org.elasticsearch.client.RestClientBuilder;

/**
 * Callback interface that can be implemented by beans wishing to further customize the
 * {@link org.elasticsearch.client.RestClient} via a {@link RestClientBuilder} whilst
 * retaining default auto-configuration.
 *
 * @author OAK
 * @since 2019/06/24 19:22:00 PM.
 * @version  1.0
 */
@FunctionalInterface
public interface RestClientBuilderCustomizer {

    /**
     * Customize the {@link RestClientBuilder}.
     * @param builder the builder to customize
     */
    void customize(RestClientBuilder builder);

}
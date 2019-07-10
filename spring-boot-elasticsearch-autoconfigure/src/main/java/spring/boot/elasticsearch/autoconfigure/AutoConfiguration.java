package spring.boot.elasticsearch.autoconfigure;

import java.time.Duration;

import org.apache.http.HttpHost;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Boot Elasticsearch REST clients.
 *
 * @author OAK
 * @since 2019/06/24 19:22:00 PM.
 * @version  1.0
 */
@Configuration
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(RestClientProperties.class)
public class AutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RestClient restClient(RestClientBuilder builder) {
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public RestHighLevelClient restHighLevelClient(RestClientBuilder restClientBuilder) {
        return new RestHighLevelClient(restClientBuilder);
    }

    @Bean
    @ConditionalOnMissingBean
    public RestClientBuilder restClientBuilder(RestClientProperties properties,
                                               ObjectProvider<RestClientBuilderCustomizer> builderCustomizers) {

        HttpHost[] hosts = properties.getUris().stream().map(HttpHost::create).toArray(HttpHost[]::new);
        RestClientBuilder builder = RestClient.builder(hosts);
        PropertyMapper map = PropertyMapper.get();

        builder.setRequestConfigCallback((requestConfigBuilder) -> {
            map.from(properties::getConnectionTimeout).whenNonNull().asInt(Duration::toMillis)
                    .to(requestConfigBuilder::setConnectTimeout);
            map.from(properties::getReadTimeout).whenNonNull().asInt(Duration::toMillis)
                    .to(requestConfigBuilder::setSocketTimeout);
            return requestConfigBuilder;
        });

        map.from(properties::getMaxConnPerRoute).whenHasText().to((maxConnPerRoute) -> {
//            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//            Credentials credentials = new UsernamePasswordCredentials(properties.getUsername(),
//                    properties.getPassword());
//            credentialsProvider.setCredentials(AuthScope.ANY, credentials);
            builder.setHttpClientConfigCallback(httpClientConfigCallback -> {
                map.from(properties::getMaxConnTotal).whenNonNull()
                        .to(httpClientConfigCallback::setMaxConnTotal);
                map.from(properties::getMaxConnTotal).whenNonNull()
                        .to(httpClientConfigCallback::setMaxConnPerRoute);
//                httpClientConfigCallback.setDefaultCredentialsProvider(credentialsProvider);

                httpClientConfigCallback.setDefaultIOReactorConfig(IOReactorConfig.custom().setIoThreadCount(10).build());

                return httpClientConfigCallback;
            });
        });
       // builderCustomizers.getIfAvailable().customize(builder);
        return builder;
    }

}
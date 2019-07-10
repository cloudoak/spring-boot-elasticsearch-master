package spring.boot.elasticsearch.constants;

/**
 *
 * Elasticsearch common constants.
 *
 * @author OAK
 * @since 2019/06/25 14:19:00 PM.
 * @version 1.0
 *
 */
public interface ElasticsearchConstants {

    /**
     * Elasticsearch settings default date format.
     */
    String DATE_FORMAT = "strict_date_optional_time||epoch_millis";

    /**
     * Elasticsearch settings default document type.
     */
    String DEFAULT_INDEX_TYPE = "_doc";

    /**
     * Elasticsearch settings default document from size.
     */
    Integer DEFAULT_FROM_SIZE = 0;

    /**
     * Elasticsearch settings default document page size.
     */
    Integer DEFAULT_PAGE_SIZE = 10000;

    /**
     * Elasticsearch settings default document threshold.
     */
    Integer THRESHOLD = 1000;

}

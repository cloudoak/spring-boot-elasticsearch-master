package spring.boot.elasticsearch.vo;

import lombok.Data;

import java.util.Map;

/**
 * Elasticseacrh index value object.
 *
 * @author OAK
 * @since 2019/06/25 14:29:00 PM.
 * @version 1.0
 */
@Data
public class IndexVo extends BaseVo {

    /**
     * Elasticsearch index name.
     */
    private String name;

    /**
     * Elasticsearch index attribute collection.
     */
    private Map<String, String> attr;

}

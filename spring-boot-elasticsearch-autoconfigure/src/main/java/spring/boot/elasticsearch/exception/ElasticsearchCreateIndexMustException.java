package spring.boot.elasticsearch.exception;

import spring.boot.elasticsearch.constants.ElasticsearchCreatedFailStatus;
import lombok.Data;

import java.util.Map;

/**
 *
 * Elasticsearch create index must specify one exception.
 *
 * @author OAK
 * @since 2019/06/25 12:58:00 PM.
 * @version 1.0
 *
 */
@Data
public class ElasticsearchCreateIndexMustException extends ElasticsearchException {

    /**
     * Error code.
     */
    private Integer code;

    /**
     * Error message.
     */
    private String message;

    /**
     * Error attributes.
     */
    private Map<String, Object> attr;

    /**
     * Error default status.
     */
    private ElasticsearchCreatedFailStatus errorStatus = ElasticsearchCreatedFailStatus.CREATED_MUST_STATUS;

    public ElasticsearchCreateIndexMustException(){
        this.code = errorStatus.getCode();
        this.message = errorStatus.getMessage();
    }

    public ElasticsearchCreateIndexMustException(String message, Integer code){
        this.code = code;
        this.message = message;
    }

    public ElasticsearchCreateIndexMustException(String message, Integer code, Map<String, Object> attr){
        this.code = code;
        this.message = message;
        this.attr = attr;
    }

}

package spring.boot.elasticsearch.exception;

import lombok.Data;

import java.util.Map;
/**
 *
 * Elasticsearch delete index exception.
 *
 * @author OAK
 * @since 2019/06/25 12:58:00 PM.
 * @version 1.0
 *
 */
@Data
public class ElasticsearchDeleteIndexException extends ElasticsearchException {

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

    public ElasticsearchDeleteIndexException(){
        this.code = 405;
        this.message = "Delete Elasticsearch index found a fail.";
    }

    public ElasticsearchDeleteIndexException(String message, Integer code){
        this.code = code;
        this.message = message;
    }

    public ElasticsearchDeleteIndexException(String message, Integer code, Map<String, Object> attr){
        this.code = code;
        this.message = message;
        this.attr = attr;
    }

}

package spring.boot.elasticsearch.exception;

import lombok.Data;

import java.util.Map;

/**
 *
 * Elasticsearch exception.
 *
 * @author OAK
 * @since 2019/06/25 12:58:00 PM.
 * @version 1.0
 *
 */
@Data
public class ElasticsearchException extends RuntimeException {

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

    public ElasticsearchException(){
        this.code = 400;
        this.message = "Elastisearch Operation 发生错误!";
    }

    public ElasticsearchException(String message, Integer code){
        this.code = code;
        this.message = message;
    }

    public ElasticsearchException(String message, Integer code, Map<String, Object> attr){
        this.code = code;
        this.message = message;
        this.attr = attr;
    }

    public ElasticsearchException(String message, Throwable throwable){
        super(message, throwable);
    }
}

package spring.boot.elasticsearch.constants;

/**
 *
 * Elasticsearch created fail status.
 *
 *
 * @author OAK
 * @since 2019/06/25 14:22:00 PM.
 * @version 1.0
 */
public enum ElasticsearchCreatedFailStatus {

    CREATED_FAIL_STATUS(400, "Created elasticsearch index fail."),
    CREATED_INDEX_NOT_FOUND_STATUS(401, "Create elasticsearch index when index not found."),
    CREATED_MUST_STATUS(402, "Created elasticsearch index when must specify one index.");

    /**
     * Error code.
     */
    private Integer code;

    /**
     * Error message.
     */
    private String message;

    ElasticsearchCreatedFailStatus(Integer code, String message){
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

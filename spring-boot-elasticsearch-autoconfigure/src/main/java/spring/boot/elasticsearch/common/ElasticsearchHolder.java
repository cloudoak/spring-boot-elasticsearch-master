package spring.boot.elasticsearch.common;

/**
 *
 * Elasticsearch Holder
 *
 * @author OAK
 * @since 2019/06/25 16:17:00 PM.
 * @version 1.0
 *
 */
public enum ElasticsearchHolder {
    INSTANCE;

    java.util.concurrent.ConcurrentMap<String, String[]> stringConcurrentHashMap = new
            java.util.concurrent.ConcurrentHashMap<>();

    public String[] save(String indexName, String [] source){
        return stringConcurrentHashMap.putIfAbsent(indexName, source);
    }

    public String[] getOrDefault(String indexName, String [] source){
        return stringConcurrentHashMap.getOrDefault(indexName, source);
    }

    public void clear(){
        stringConcurrentHashMap.clear();
    }

}

package spring.boot.elasticsearch.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import spring.boot.elasticsearch.annotations.Field;
import spring.boot.elasticsearch.common.TimeLength;
import spring.boot.elasticsearch.config.ElasticsearchProperties;
import spring.boot.elasticsearch.config.ElasticsearchSearchProperties;
import spring.boot.elasticsearch.constants.ElasticsearchConstants;
import spring.boot.elasticsearch.config.ElasticsearchRequestProperties;
import spring.boot.elasticsearch.exception.*;
import spring.boot.elasticsearch.service.ElasticsearchService;
import spring.boot.exception.*;
import spring.boot.elasticsearch.vo.IndexVo;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import spring.boot.elasticsearch.vo.PageVo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static spring.boot.elasticsearch.constants.ElasticsearchConstants.DEFAULT_FROM_SIZE;
import static spring.boot.elasticsearch.constants.ElasticsearchConstants.DEFAULT_PAGE_SIZE;

/**
 *
 * Elasticsearch crud implements class.
 *
 * @author OAK
 * @since 2019/06/24 20:16:00 PM.
 * @version 1.0
 *
 */
@Slf4j
@Service
@Transactional
public class ElasticsearchServiceImpl implements ElasticsearchService {

    /**
     *  Elasticsearch properties instance.
     */
    @Autowired
    private ElasticsearchProperties properties;

    /**
     * Elasticsearch rest high level client instance.
     */
    @Autowired
    private RestHighLevelClient restClient;

    /**
     * Elasticsearch request properties instance.
     */
    @Autowired
    private ElasticsearchRequestProperties requestProperties;

    /**
     * Elasticsearch search request properties instance.
     */
    @Autowired
    private ElasticsearchSearchProperties searchProperties;

    /**
     * Whether Elasticsearch client contains the index.
     * @param index The Elasticsearch index value.
     * @return Whether exists the Elasticsearch index.
     * @throws IOException Throws io exception.
     */
    public Boolean exists(String index) throws ElasticsearchExistsIndexException {
        Boolean flags = false;
        try {
            GetIndexRequest request = new GetIndexRequest();
            request.indices(index);
            request.local(false);
            request.humanReadable(true);
            flags = restClient.indices().exists(request, RequestOptions.DEFAULT);
        }catch (IOException ex){
            log.error("Whether exists elasticsearch index {} found a fail, {}", index, ex);
            throw new ElasticsearchExistsIndexException();
        }
        return flags;
    }

    /**
     * According to Elasticsearch index value go delete Elasticsearch inverted index.
     * @param index Elasticsearch index value.
     * @return Whether delete Elasticsearch index.
     * @throws ElasticsearchDeleteIndexException Throws Elasticsearch delete index exception.
     */
    public Boolean delete(String index) throws  ElasticsearchDeleteIndexException {
        log.info("Delete elasticsearch index {}", index);
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(index);
        AcknowledgedResponse acknowledgedResponse = null;
        try {
            acknowledgedResponse = restClient.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("Delete elasticsearch index {} found fail, {}", index, e);
            throw new ElasticsearchDeleteIndexException();
        }
        boolean acknowledged = acknowledgedResponse.isAcknowledged();
        log.info("Delete elasticsearch index {} finish, acknowledged is {}", index, acknowledged);
        return acknowledged;
    }

    /**
     * According to Elasticsearch index type and index settings collection go mapping Elasticsearch current Index.
     * @param type Elasticsearch index type.
     * @param indexCollections Elasticsearch index settings collection.
     * @return XContent Builder After for mapping Elasticsearch current Index.
     * @throws IOException Throws io exception.
     */
    private XContentBuilder mapping(String type, Collection<? extends IndexVo> indexCollections) throws ElasticsearchMappingException {
        log.info("Create elasticsearch mapping index type {} settings {}", type,
                JSON.toJSONStringWithDateFormat(indexCollections, "yyyy-MM-dd hh:mm:ss", SerializerFeature.DisableCircularReferenceDetect));
        XContentBuilder builder = null;
        try {
            builder = JsonXContent.contentBuilder().startObject().startObject(type).startObject("properties");
            for (IndexVo indexVo : indexCollections) {
                builder = builder.startObject(indexVo.getName());
                Map<String, String> attr = indexVo.getAttr();
                if (!attr.isEmpty()) {
                    for (Map.Entry<String, String> entry : attr.entrySet()) {
                        builder = builder.field(entry.getKey(), entry.getValue());
                    }
                }
                builder = builder.endObject();
            }
            builder = builder.endObject().endObject().endObject();
        }catch (IOException ex){
            log.error("Elasticsearch mapping index type {} found a fail, {}", type, ex);
            throw new ElasticsearchMappingException();
        }
        return builder;
    }

    /**
     * According to Elasticsearch index and index type and alias name and index collection go Created Elasticsearch Index.
     * @param index Elasticsearch index.
     * @param type Elasticsearch index type.
     * @param aliasName Elasticsearch alias name.
     * @param indexCollections Elasticsearch index collection.
     * @return Whether created Elasticsearch index.
     * @throws ElasticsearchCreateIndexNotFoundException Created Elasticsearch index not found When throw exception.
     * @throws ElasticsearchCreateIndexMustException Created Elasticsearch index Must to Created When throw exception.
     * @throws ElasticsearchCreateIndexException Created Elasticsearch index When throw exception.
     * @throws ElasticsearchMappingException Mapping Elasticsearch index When throw exception.
     */
    public Boolean create(String index, String type, String aliasName, Collection<? extends IndexVo> indexCollections) throws ElasticsearchCreateIndexNotFoundException,
            ElasticsearchCreateIndexMustException, ElasticsearchCreateIndexException, ElasticsearchMappingException {

        log.info("Create elasticsearch index {} type {} alias Name {} settings {}", index, type, type,
                JSON.toJSONStringWithDateFormat(indexCollections, "yyyy-MM-dd hh:mm:ss", SerializerFeature.DisableCircularReferenceDetect));

        if(StringUtils.isEmpty(index)){
            throw new ElasticsearchCreateIndexNotFoundException();
        }

        if(StringUtils.isEmpty(type)){
            type = ElasticsearchConstants.DEFAULT_INDEX_TYPE;
        }

        if(StringUtils.isEmpty(aliasName)){
            aliasName = index.substring(0, 3);
        }

        if(indexCollections.isEmpty()){
            throw new ElasticsearchCreateIndexMustException();
        }

        CreateIndexRequest request = new CreateIndexRequest(index);
        PropertyMapper map = PropertyMapper.get();
        Settings.Builder builder = Settings.builder();
        map.from(properties::getIndex).whenNonNull().to( settings ->{
            settings.forEach( (property, value) -> builder.put(property, value));
        });
        request.settings(builder);
        request.mapping(type, mapping(type, indexCollections));
        request.alias(new Alias(aliasName));
        CreateIndexResponse createIndexResponse = null;
        try {
            createIndexResponse = restClient.indices().create(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("Create elasticsearch index {} found fail, {}", index, e);
            throw new ElasticsearchCreateIndexException();
        }
        boolean acknowledged = createIndexResponse.isAcknowledged();
        boolean shardsAcknowledged = createIndexResponse.isShardsAcknowledged();
        log.info("Create elasticsearch index {} type {} alias Name {} finish, acknowledged is {} shardsAcknowledged is {}",
                index, type, aliasName, acknowledged, shardsAcknowledged);
        return acknowledged;
    }

    /**
     * Batch add entities to Elasticsearch according elasticsearch index and type and entities collection or routing.
     * @param index Elasticsearch index.
     * @param type Elasticsearch type.
     * @param routing Elasticsearch routing.
     * @param entities Elasticsearh to save entities collection.
     * @return Whether bulk entities to Elasticsearch.
     */
    public <T> Boolean bulk(String index, String type, String routing, Collection<T> entities) throws ElasticsearchPersistenceException {
        log.info("Save elasticsearch index {} type {} Arguments {}", index, type,
                JSON.toJSONStringWithDateFormat(entities, "yyyy-MM-dd hh:mm:ss", SerializerFeature.DisableCircularReferenceDetect));

        if(StringUtils.isEmpty(type)){
            type = ElasticsearchConstants.DEFAULT_INDEX_TYPE;
        }

        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.timeout(TimeValue.timeValueMillis(requestProperties.getTimeout()));
        bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.parse(requestProperties.getRefreshPolicy()));
        bulkRequest.waitForActiveShards(requestProperties.getWaitForActiveShards());
        for(T entity : entities){
            IndexRequest indexRequest = new IndexRequest(index, type);
            if(!StringUtils.isEmpty(routing)){
                indexRequest.routing(routing);
            }
            indexRequest.opType(DocWriteRequest.OpType.INDEX).source(entity);
            bulkRequest.add(indexRequest);
        }
        BulkResponse bulkResponse = null;
        try {
            bulkResponse = restClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("Elasticsearch index {} type {} save entities {} find a fail, {}", index, type,
                    JSON.toJSONStringWithDateFormat(entities, "yyyy-MM-dd hh:mm:ss", SerializerFeature.DisableCircularReferenceDetect),
                    e);
            throw new ElasticsearchPersistenceException();
        }
        String buildFailureMessage = bulkResponse.buildFailureMessage();
        if(!StringUtils.isEmpty(buildFailureMessage)){
            log.error("Elasticsearch index {} type {} save entities {} build a fail message {}", index, type,
                    JSON.toJSONStringWithDateFormat(entities, "yyyy-MM-dd hh:mm:ss", SerializerFeature.DisableCircularReferenceDetect), buildFailureMessage);
        }else{
            Long ingestTookInMillis = bulkResponse.getIngestTookInMillis();
            Integer itemLength = bulkResponse.getItems().length;
            Long millis = TimeLength.getInstance().addAndGet(ingestTookInMillis);
            log.info("Elasticsearch index {} type {} The time {} article Execution Time： {} millisecond， Total time consuming {} millisecond.", index, type, itemLength, ingestTookInMillis, millis);
        }
        return !bulkResponse.hasFailures();
    }

    /**
     * Update entity to Elasticsearch according elasticsearch index and type and entity or routing.
     * @param index Elasticsearch index.
     * @param type Elasticsearch type.
     * @param routing Elasticsearch routing.
     * @param entity Elasticsearh to save entity.
     * @return Whether update entity to Elasticsearch.
     */
    public <T> Boolean update(String index, String type, String routing, T entity) throws ElasticsearchPersistenceException {
        log.info("Update elasticsearch index {} type {} Arguments {}", index, type,
                JSON.toJSONStringWithDateFormat(entity, "yyyy-MM-dd hh:mm:ss", SerializerFeature.DisableCircularReferenceDetect));
        TimeLength.getInstance().started();
        if(StringUtils.isEmpty(type)){
            type = ElasticsearchConstants.DEFAULT_INDEX_TYPE;
        }

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.timeout(TimeValue.timeValueMillis(requestProperties.getTimeout()));
        updateRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.parse(requestProperties.getRefreshPolicy()));
        updateRequest.waitForActiveShards(requestProperties.getWaitForActiveShards());
        updateRequest.doc(entity);
        if(!StringUtils.isEmpty(routing)){
            updateRequest.routing(routing);
        }
        UpdateResponse updateResponse = null;
        try {
            updateResponse = restClient.update(updateRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("Elasticsearch index {} type {} Update entities {} find a fail, {}", index, type,
                    JSON.toJSONStringWithDateFormat(entity, "yyyy-MM-dd hh:mm:ss", SerializerFeature.DisableCircularReferenceDetect),
                    e);
            throw new ElasticsearchPersistenceException();
        }
        boolean flags =  updateResponse.getResult() == DocWriteResponse.Result.UPDATED;
        Long millis = TimeLength.getInstance().stop();
        log.info("Elasticsearch index {} type {} The time {} article Execution Time： {} millisecond， Total time consuming {} millisecond.", index, type, millis);
        return flags;
    }

    public <Q>  Boolean delete(String index, String routing, Q  searchEntity) throws ElasticsearchPersistenceException {
        return delete(index, "", routing, searchEntity);
    }

    /**
     * Batch update entities to Elasticsearch according elasticsearch index and type and entities collection or routing.
     * @param index Elasticsearch index.
     * @param type Elasticsearch type.
     * @param routing Elasticsearch routing.
     * @param searchEntity Elasticsearh to delete entity.
     * @return Whether delete entities to Elasticsearch.
     */
    public <Q>  Boolean delete(String index, String type, String routing, Q  searchEntity) throws ElasticsearchPersistenceException {
        log.info("Update elasticsearch index {} type {} Arguments {}", index, type,
                JSON.toJSONStringWithDateFormat(searchEntity, "yyyy-MM-dd hh:mm:ss", SerializerFeature.DisableCircularReferenceDetect));

        if(StringUtils.isEmpty(type)){
            type = ElasticsearchConstants.DEFAULT_INDEX_TYPE;
        }

        DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest();
        deleteByQueryRequest.setTimeout(TimeValue.timeValueMillis(requestProperties.getTimeout()));
        deleteByQueryRequest.setWaitForActiveShards(requestProperties.getWaitForActiveShards());
        if(!StringUtils.isEmpty(routing)){
            deleteByQueryRequest.setRouting(routing);
        }
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        putMultiConditionFields(searchEntity, sourceBuilder);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        deleteByQueryRequest.setQuery(sourceBuilder.query());
        BulkByScrollResponse bulkByScrollResponse = null;
        try {
            bulkByScrollResponse = restClient.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("Elasticsearch index {} type {} Delete entity {} find a fail, {}", index, type,
                    JSON.toJSONStringWithDateFormat(searchEntity, "yyyy-MM-dd hh:mm:ss", SerializerFeature.DisableCircularReferenceDetect),
                    e);
            throw new ElasticsearchPersistenceException();
        }
        Boolean hasFailures = bulkByScrollResponse.getBulkFailures().isEmpty();
        if(hasFailures){
            log.error("Elasticsearch index {} type {} Update entities {} build a fail message {}", index, type,
                    JSON.toJSONStringWithDateFormat(searchEntity, "yyyy-MM-dd hh:mm:ss", SerializerFeature.DisableCircularReferenceDetect),
                    JSON.toJSONStringWithDateFormat(bulkByScrollResponse.getBulkFailures(), "yyyy-MM-dd hh:mm:ss", SerializerFeature.DisableCircularReferenceDetect));
        }else{
            Long ingestTookInMillis = bulkByScrollResponse.getTook().getMillis();
            Integer itemLength = bulkByScrollResponse.getBatches();
            Long millis = TimeLength.getInstance().addAndGet(ingestTookInMillis);
            log.info("Elasticsearch index {} type {} The time {} article Execution Time： {} millisecond， Total time consuming {} millisecond.", index, type, itemLength, ingestTookInMillis, millis);
        }
        return !hasFailures;
    }

    /**
     * Elasticsearch index search searchEntity to Get result collection.
     * @param index Elasticsearch index.
     * @param searchEntity search entity.
     * @return result collection.
     */
    public <T, Q> List<T> search(String index, Q  searchEntity, Class<T> clazz) throws ElasticsearchSearchException {
        return this.search(index, "", searchEntity, new PageVo(DEFAULT_FROM_SIZE, DEFAULT_PAGE_SIZE), clazz);
    }

    /**
     * Elasticsearch index search searchEntity to Get result collection.
     * @param index Elasticsearch index.
     * @param routing Elasticsearch routing.
     * @param searchEntity search entity.
     * @return result collection.
     */
    public <T, Q> List<T> search(String index, String routing, Q searchEntity, Class<T> clazz) throws ElasticsearchSearchException {
        return this.search(index, routing, searchEntity, new PageVo(DEFAULT_FROM_SIZE, DEFAULT_PAGE_SIZE), clazz);
    }

    /**
     * Elasticsearch index search searchEntity to Get result collection.
     * @param index Elasticsearch index.
     * @param routing Elasticsearch routing.
     * @param searchEntity search entity.
     * @param pageVo page vo.
     * @return result collection.
     */
    public <T, Q> List<T> search(String index, String routing, Q searchEntity, PageVo pageVo, Class<T> clazz) throws ElasticsearchSearchException {
        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.searchType(SearchType.DEFAULT);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        putMultiConditionFields(searchEntity, sourceBuilder);
        sourceBuilder.timeout(new TimeValue(searchProperties.getTimeout(), TimeUnit.MILLISECONDS));
        if(!StringUtils.isEmpty(routing)){
            searchRequest.routing(routing);
        }
        sourceBuilder.from(pageVo.getFrom()).size(pageVo.getSize());
        searchRequest.source(sourceBuilder);
        SearchResponse response = null;
        try {
            response = restClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e1) {
            log.error("Elasticsearch index {} Search Arguments {} When find a fail, {}", index,
                    JSON.toJSONStringWithDateFormat(searchEntity, "yyyy-MM-dd hh:mm:ss", SerializerFeature.DisableCircularReferenceDetect), e1);
            throw new ElasticsearchSearchException();
        }
        List<T>  searchList = mapResults(response, clazz);
        return searchList;
    }

    /**
     * Elasticsearch index scroll searchEntity to Get result collection.
     * @param index Elasticsearch index.
     * @param searchEntity search entity.
     * @return result collection.
     */
    public <T, Q> List<T> scroll(String index, Q searchEntity, Class<T> clazz) throws ElasticsearchSearchException {
        return scroll(index, "", searchEntity, new PageVo(DEFAULT_FROM_SIZE, DEFAULT_PAGE_SIZE), clazz);
    }

    /**
     * Elasticsearch index scroll searchEntity to Get result collection.
     * @param index Elasticsearch index.
     * @param routing Elasticsearch routing.
     * @param searchEntity search entity.
     * @return result collection.
     */
    public <T, Q> List<T> scroll(String index, String routing, Q searchEntity, Class<T> clazz) throws ElasticsearchSearchException {
        return scroll(index, routing, searchEntity, new PageVo(DEFAULT_FROM_SIZE, DEFAULT_PAGE_SIZE), clazz);
    }

    /**
     * Elasticsearch index scroll searchEntity to Get result collection.
     * @param index Elasticsearch index.
     * @param routing Elasticsearch routing.
     * @param searchEntity search entity.
     * @return result collection.
     */
    public <T, Q> List<T> scroll(String index, String routing, Q searchEntity, PageVo pageVo, Class<T> clazz) throws ElasticsearchSearchException {
        List<T>  searchList = new ArrayList<>();
        List<String>  scrollIdList = new ArrayList<>();
        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.searchType(SearchType.DEFAULT);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        putMultiConditionFields(searchEntity, sourceBuilder);
        sourceBuilder.timeout(new TimeValue(searchProperties.getTimeout(), TimeUnit.MILLISECONDS));
        if(!StringUtils.isEmpty(routing)){
            searchRequest.routing(routing);
        }
        sourceBuilder.from(pageVo.getFrom()).size(pageVo.getSize());
        searchRequest.source(sourceBuilder);
        SearchResponse response = null;
        try {
            response = restClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e1) {
            log.error("Elasticsearch index {} Search Arguments {} When find a fail, {}", index,
                    JSON.toJSONStringWithDateFormat(searchEntity, "yyyy-MM-dd hh:mm:ss", SerializerFeature.DisableCircularReferenceDetect), e1);
            throw new ElasticsearchSearchException();
        }
        if(response != null){
            searchList.addAll( mapResults(response, clazz) );
            String scrollId = response.getScrollId();
            if(!StringUtils.isEmpty(scrollId)){
                scrollIdList.add(scrollId);
                for(SearchHit [] searchHits = null; ((searchHits = response.getHits().getHits()) != null
                        && searchHits.length > 0); ){
                    Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
                    SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                    scrollRequest.scroll(scroll);
                    try {
                        response = restClient.scroll(scrollRequest, RequestOptions.DEFAULT);
                    } catch (IOException e) {
                        log.error("Elasticsearch index {} Cursor Search Arguments {} When find a fail, {}", index,
                                JSON.toJSONStringWithDateFormat(searchEntity, "yyyy-MM-dd hh:mm:ss", SerializerFeature.DisableCircularReferenceDetect), e);
                        throw new ElasticsearchSearchException();
                    }
                }
            }
        }
        if(!scrollIdList.isEmpty()){
            try {
                ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
                clearScrollRequest.setScrollIds(scrollIdList);
                ClearScrollResponse clearScrollResponse = restClient.clearScroll(clearScrollRequest,RequestOptions.DEFAULT);
            } catch (IOException e) {
                log.error("Elasticsearch index {} Clear Cursor Search Arguments {} When find a fail, {}", index,
                        JSON.toJSONStringWithDateFormat(searchEntity, "yyyy-MM-dd hh:mm:ss", SerializerFeature.DisableCircularReferenceDetect), e);
                throw new ElasticsearchSearchException();
            }
        }
        return searchList;
    }

    /**
     * Elasticsearch index search searchEntities to Get result collection.
     * @param index Elasticsearch index.
     * @param searchEntities search entities.
     * @return result collection.
     */
    public <T, Q> List<T> msearch(String index, List<Q> searchEntities, Class<T> clazz) throws ElasticsearchSearchException {
        return this.msearch(index, "", searchEntities, new PageVo(DEFAULT_FROM_SIZE, DEFAULT_PAGE_SIZE), clazz);
    }

    /**
     * Elasticsearch index search searchEntities to Get result collection.
     * @param index Elasticsearch index.
     * @param routing Elasticsearch routing.
     * @param searchEntities search entities.
     * @return result collection.
     */
    public <T, Q> List<T> msearch(String index, String routing, List<Q> searchEntities, Class<T> clazz) throws ElasticsearchSearchException {
        return this.msearch(index, routing, searchEntities, new PageVo(DEFAULT_FROM_SIZE, DEFAULT_PAGE_SIZE), clazz);
    }

    /**
     * Elasticsearch index search searchEntities to Get result collection.
     * @param index Elasticsearch index.
     * @param routing Elasticsearch routing.
     * @param searchEntities search entities.
     * @return result collection.
     */
    public <T, Q> List<T> msearch(String index, String routing, List<Q> searchEntities, PageVo pageVo, Class<T> clazz) throws ElasticsearchSearchException {
        Instant instant = Instant.now();
        MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
        Instant searchInstant = Instant.now();
        multiSearchRequest.maxConcurrentSearchRequests(20);
        searchEntities.stream().forEach(searchEntity ->{
            SearchRequest searchRequest = new SearchRequest(index);
//            searchRequest.searchType(SearchType.QUERY_THEN_FETCH);
            searchRequest.searchType(SearchType.DFS_QUERY_THEN_FETCH);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
//            sourceBuilder.fetchSource(FetchSourceContext.DO_NOT_FETCH_SOURCE);
            sourceBuilder.fetchSource(true);
//            sourceBuilder.explain(true);
            putMultiConditionFields(searchEntity, sourceBuilder);
            sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
            if(!StringUtils.isEmpty(routing)){
                searchRequest.routing(routing);
            }
            sourceBuilder.from(pageVo.getFrom()).size(pageVo.getSize());
            searchRequest.source(sourceBuilder);
            multiSearchRequest.add(searchRequest);
        });

        log.info("Elasticsearch searchEntities foreach used Time {} ms", Instant.now().minus(searchInstant.getMillis()).getMillis());
        MultiSearchResponse multiSearchResponse = null;
        Instant instant1 = Instant.now();
        try {
            multiSearchResponse = restClient.msearch(multiSearchRequest, RequestOptions.DEFAULT);
        } catch (IOException e1) {
            log.error("Elasticsearch index {} Search Arguments {} When find a fail, {}", index,
                    JSON.toJSONStringWithDateFormat(searchEntities, "yyyy-MM-dd hh:mm:ss", SerializerFeature.DisableCircularReferenceDetect), e1);
            throw new ElasticsearchSearchException();
        }
        log.info("Elasticsearch msearch used Time {} ms", Instant.now().minus(instant1.getMillis()).getMillis());
        Instant instant2 = Instant.now();
        List<T>  searchList = mapResults(multiSearchResponse, clazz);
        log.info("Elasticsearch mapResults used Time {} ms", Instant.now().minus(instant2.getMillis()).getMillis());
        log.info("Search Elasticsearch index {} Total time consuming：{} millisecond", index, Instant.now().minus(instant.getMillis()).getMillis());
        return searchList;
    }

    private <Q> void putMultiConditionFields(Q result, SearchSourceBuilder sourceBuilder) {
        if (sourceBuilder != null && result != null) {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            for (java.lang.reflect.Field field : result.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Field scriptedField = field.getAnnotation(Field.class);
                String name;
                Object propertyValue = null;
                if (scriptedField != null) {
                    name = scriptedField.name().isEmpty() ? field.getName() : scriptedField.name();
                } else {
                    name = field.getName();
                }
                try {
                    propertyValue = field.get(result);
                    if (!StringUtils.isEmpty(propertyValue)) {
                        if(scriptedField.in()){
                            BoolQueryBuilder childBoolQueryBuilder = QueryBuilders.boolQuery();
                            String [] propValues = null;
                            if(propertyValue instanceof String[]){
                                propValues = (String []) propertyValue;
                            }else if(propertyValue instanceof String){
                                boolean flags = ((String) propertyValue).indexOf(",") != -1;
                                if(flags){
                                    propValues = ((String) propertyValue).split(",");
                                }
                            }else{
                                propValues = new String[1];
                                propValues[0] = (String) propertyValue;
                            }
                            if(propValues != null){
                                for(String propValue : propValues){
                                    childBoolQueryBuilder.should(QueryBuilders.termsQuery(name, propValue));
                                }
                                boolQueryBuilder.must(childBoolQueryBuilder);
                            }
                        }else{
                            boolQueryBuilder.must(QueryBuilders.matchQuery(name, propertyValue));
                        }
                    }
                } catch (IllegalArgumentException e) {
                    throw new ElasticsearchException(
                            "failed to set scripted field: " + name + " with value: " + propertyValue, e);
                } catch (IllegalAccessException e) {
                    throw new ElasticsearchException("failed to access scripted field: " + name, e);
                }
            }
            sourceBuilder.query(boolQueryBuilder);
        }
    }

    private <T> void populateScriptFields(T result, SearchHit hit) {
        if (hit.getFields() != null && !hit.getFields().isEmpty() && result != null) {
            for (java.lang.reflect.Field field : result.getClass().getDeclaredFields()) {
                Field scriptedField = field.getAnnotation(Field.class);
                if (scriptedField != null) {
                    String name = scriptedField.name().isEmpty() ? field.getName() : scriptedField.name();
                    DocumentField searchHitField = hit.getFields().get(name);
                    if (searchHitField != null) {
                        field.setAccessible(true);
                        try {
                            field.set(result, searchHitField.getValue());
                        } catch (IllegalArgumentException e) {
                            throw new ElasticsearchException(
                                    "failed to set scripted field: " + name + " with value: " + searchHitField.getValue(), e);
                        } catch (IllegalAccessException e) {
                            throw new ElasticsearchException("failed to access scripted field: " + name, e);
                        }
                    }
                }
            }
        }
    }

    public <T> List<T> mapResults(MultiSearchResponse responses, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        Instant start = Instant.now();
        Arrays.stream(responses.getResponses()).forEach(response -> {
            if (!response.isFailure() ) {
                Arrays.stream(response.getResponse().getHits().getHits()).forEach(searchHit -> {
                    T result = mapEntity(searchHit.getSourceAsString(), clazz);
                    populateScriptFields(result, searchHit);
                    list.add(result);
                });
            }
        });
        log.info(" Elasticsearch map Results cost Time {} ms", Instant.now().minus(start.getMillis()).getMillis());
        return list;
    }

    public <T> List<T> mapResults(SearchResponse response, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        Arrays.stream(response.getHits().getHits()).forEach(searchHit -> {
            T result = mapEntity(searchHit.getSourceAsString(), clazz);
            populateScriptFields(result, searchHit);
            list.add(result);
        });
        return list;
    }

    public <T> T mapToObject(String source, Class<T> clazz) throws IOException {
        return JSON.parseObject(source, clazz);
    }

    @Nullable
    public <T> T mapEntity(String source, Class<T> clazz) {
        if (StringUtils.isEmpty(source)) {
            return null;
        }
        try {
            return mapToObject(source, clazz);
        } catch (IOException e) {
            throw new ElasticsearchException("failed to map source [ " + source + "] to class " + clazz.getSimpleName(), e);
        }
    }

    private <T> T mapEntity(Collection<DocumentField> values, Class<T> clazz) {
        return mapEntity(buildJSONFromFields(values), clazz);
    }

    private String buildJSONFromFields(Collection<DocumentField> values) {
        JsonFactory nodeFactory = new JsonFactory();
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            JsonGenerator generator = nodeFactory.createGenerator(stream, JsonEncoding.UTF8);
            generator.writeStartObject();
            for (DocumentField value : values) {
                if (value.getValues().size() > 1) {
                    generator.writeArrayFieldStart(value.getName());
                    for (Object val : value.getValues()) {
                        generator.writeObject(val);
                    }
                    generator.writeEndArray();
                } else {
                    generator.writeObjectField(value.getName(), value.getValue());
                }
            }
            generator.writeEndObject();
            generator.flush();
            return new String(stream.toByteArray(), Charset.forName("UTF-8"));
        } catch (IOException e) {
            return null;
        }
    }
}
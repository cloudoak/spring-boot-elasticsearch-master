package spring.boot.elasticsearch.service;

import spring.boot.elasticsearch.exception.*;
import spring.boot.exception.*;
import spring.boot.elasticsearch.vo.IndexVo;
import spring.boot.elasticsearch.vo.PageVo;

import java.util.Collection;
import java.util.List;

/**
 * Elasticseacrh Service.
 *
 * @author OAK
 * @since 2019/06/25 14:29:00 PM.
 * @version 1.0
 */
public interface ElasticsearchService {

    Boolean exists(String index) throws ElasticsearchExistsIndexException;

    Boolean delete(String index) throws ElasticsearchDeleteIndexException;

    Boolean create(String index, String type, String aliasName, Collection<? extends IndexVo> indexCollections) throws ElasticsearchCreateIndexNotFoundException,
            ElasticsearchCreateIndexMustException, ElasticsearchCreateIndexException, ElasticsearchMappingException;

    <T> Boolean bulk(String index, String type, String routing, Collection<T> entities) throws ElasticsearchPersistenceException;

    <T> Boolean update(String index, String type, String routing, T entity) throws ElasticsearchPersistenceException;

    <Q>  Boolean delete(String index, String routing, Q searchEntity) throws ElasticsearchPersistenceException;

    <Q>  Boolean delete(String index, String type, String routing, Q searchEntity) throws ElasticsearchPersistenceException;

    <T, Q> List<T> search(String index, Q searchEntity, Class<T> clazz) throws ElasticsearchSearchException;

    <T, Q> List<T> search(String index, String routing, Q searchEntities, Class<T> clazz) throws ElasticsearchSearchException;

    <T, Q> List<T> search(String index, String routing, Q searchEntities, PageVo pageVo, Class<T> clazz) throws ElasticsearchSearchException;

    <T, Q> List<T> scroll(String index, Q searchEntity, Class<T> clazz) throws ElasticsearchSearchException;

    <T, Q> List<T> scroll(String index, String routing, Q searchEntity, Class<T> clazz) throws ElasticsearchSearchException;

    <T, Q> List<T> scroll(String index, String routing, Q searchEntity, PageVo pageVo, Class<T> clazz) throws ElasticsearchSearchException;

    <T, Q> List<T> msearch(String index, List<Q> searchEntities, Class<T> clazz) throws ElasticsearchSearchException;

    <T, Q> List<T> msearch(String index, String routing, List<Q> searchEntities, Class<T> clazz) throws ElasticsearchSearchException;

    <T, Q> List<T> msearch(String index, String routing, List<Q> searchEntities, PageVo pageVo, Class<T> clazz) throws ElasticsearchSearchException;

}

package spring.boot.elasticsearch.annotations;

import org.elasticsearch.index.VersionType;

import java.lang.annotation.*;

/**
 *
 * @author OAK
 *
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Document {

	/**
	 * Name of the Elasticsearch index.
	 * <ul>
	 * <li>Lowercase only</li>
	 * <li><Cannot include \, /, *, ?, ", <, >, |, ` ` (space character), ,, #/li>
	 * <li>Cannot start with -, _, +</li>
	 * <li>Cannot be . or ..</li>
	 * <li>Cannot be longer than 255 bytes (note it is bytes, so multi-byte characters will count towards the 255 limit
	 * faster)</li>
	 * </ul>
	 */
	String indexName();

	/**
	 * Mapping type name.
	 */
	String type() default "";

	/**
	 * Use server-side settings when creating the index.
	 */
	boolean useServerConfiguration() default false;

	/**
	 * Number of shards for the index {@link #indexName()}. Used for index creation.
	 */
	short shards() default 5;

	/**
	 * Number of replicas for the index {@link #indexName()}. Used for index creation.
	 */
	short replicas() default 1;

	/**
	 * Refresh interval for the index {@link #indexName()}. Used for index creation.
	 */
	String refreshInterval() default "1s";

	/**
	 * Index storage type for the index {@link #indexName()}. Used for index creation.
	 */
	String indexStoreType() default "fs";

	/**
	 * Configuration whether to create an index on repository bootstrapping.
	 */
	boolean createIndex() default true;

	/**
	 * Configuration of version management.
	 */
	VersionType versionType() default VersionType.EXTERNAL;
}

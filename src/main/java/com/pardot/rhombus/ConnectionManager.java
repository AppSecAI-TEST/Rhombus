package com.pardot.rhombus;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.google.common.collect.Maps;

import com.pardot.rhombus.cobject.CKeyspaceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Pardot, an ExactTarget company
 * User: Michael Frank
 * Date: 4/17/13
 */
public class ConnectionManager {

	private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

	private List<String> contactPoints;
	private Map<String, ObjectMapper> objectMappers = Maps.newHashMap();
	private CKeyspaceDefinition defaultKeyspace;
	private Cluster cluster;
	private boolean logCql = false;

	public ConnectionManager(CassandraConfiguration configuration) {
		this.contactPoints = configuration.getContactPoints();
	}

	/**
	 * Build the cluster based on the CassandraConfiguration passed in the constructor
	 */
	public void buildCluster() {
		Cluster.Builder builder = Cluster.builder();
		for(String contactPoint : contactPoints) {
			builder.addContactPoint(contactPoint);
		}

		cluster = builder.build();
	}

	/**
	 * Get the default object mapper
	 * @return The default object mapper
	 */
	public ObjectMapper getObjectMapper() {
		return getObjectMapper(defaultKeyspace.getName());
	}

	/**
	 * Get an object mapper for a keyspace
	 * @return Object mapper for the specified keyspace
	 */
	public ObjectMapper getObjectMapper(String keyspace) {
		ObjectMapper objectMapper = objectMappers.get(keyspace);
		if(objectMapper == null) {
			logger.debug("Connecting to keyspace {}", defaultKeyspace.getName());
			Session session = cluster.connect(defaultKeyspace.getName());
			objectMapper = new ObjectMapper(session, defaultKeyspace);
			objectMapper.setLogCql(logCql);
			objectMappers.put(keyspace, objectMapper);
		}
		return objectMapper;

	}

	/**
	 * This method rebuilds a keyspace from a definition.  If forceRebuild is true, the process
	 * removes any existing keyspace with the same name.  This operation is immediate and irreversible.
	 *
	 * @param keyspaceDefinition The definition to build the keyspace from
	 * @param forceRebuild Force destruction and rebuild of keyspace
	 */
	public void buildKeyspace(CKeyspaceDefinition keyspaceDefinition, Boolean forceRebuild) throws Exception {
		if(keyspaceDefinition == null) {
			keyspaceDefinition = defaultKeyspace;
		}
		//Get a session for the new keyspace
		Session session = getSessionForNewKeyspace(keyspaceDefinition, forceRebuild);
		//Use this session to create an object mapper and build the keyspace
		ObjectMapper mapper = new ObjectMapper(session, keyspaceDefinition);
		mapper.setLogCql(logCql);
		mapper.buildKeyspace(forceRebuild);
		objectMappers.put(keyspaceDefinition.getName(), mapper);
	}

	/**
	 * Create and return a new session for the specified cluster.
	 * The caller is responsible for terminating the session.
	 * @return Empty session
	 */
	public Session getEmptySession() {
		return cluster.connect();
	}

	private Session getSessionForNewKeyspace(CKeyspaceDefinition keyspace, Boolean forceRebuild) throws Exception {
		//Get a new session
		Session session = cluster.connect();

		if(forceRebuild) {
			try {
				//Drop the keyspace if it already exists
				session.execute("DROP KEYSPACE " + keyspace.getName() + ";");
			} catch(Exception e) {
				//Ignore
			}
		}

		//First try to create the new keyspace
		StringBuilder sb = new StringBuilder();
		sb.append(keyspace.getName());
		sb.append(" WITH replication = { 'class' : '");
		sb.append(keyspace.getReplicationClass());
		for(String key : keyspace.getReplicationFactors().keySet()) {
			sb.append("', '");
			sb.append(key);
			sb.append("' : ");
			sb.append(keyspace.getReplicationFactors().get(key));
		}
		sb.append("};");
		try {
			session.execute("CREATE KEYSPACE " + sb.toString());
		} catch(Exception e) {
			//TODO Catch only the specific exception for keyspace already exists
			if(!forceRebuild) {
				//If we are not forcing a rebuild and the create failed, attempt to update
				session.execute("ALTER KEYSPACE " + sb.toString());
			} else {
				throw e;
			}
		}

		//Close our session and get a new one directly associated with the new keyspace
		session.shutdown();
		session = cluster.connect(keyspace.getName());
		return session;
	}

	/**
	 * Teardown all connections contained in associated object mappers
	 * and shutdown the cluster.
	 */
	public void teardown() {
		for(ObjectMapper mapper : objectMappers.values()) {
			mapper.teardown();
		}
		cluster.shutdown();
	}

	public void setDefaultKeyspace(CKeyspaceDefinition keyspaceDefinition) {
		this.defaultKeyspace = keyspaceDefinition;
	}

	public boolean isLogCql() {
		return logCql;
	}

	public void setLogCql(boolean logCql) {
		this.logCql = logCql;
	}
}

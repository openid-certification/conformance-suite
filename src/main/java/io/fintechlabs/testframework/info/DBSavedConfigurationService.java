package io.fintechlabs.testframework.info;

import java.time.Instant;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

import io.fintechlabs.testframework.security.AuthenticationFacade;

/**
 * @author jricher
 *
 */
@Service
public class DBSavedConfigurationService implements SavedConfigurationService {

	public static final String COLLECTION = "TEST_CONFIG";

	private static Logger logger = LoggerFactory.getLogger(DBSavedConfigurationService.class);

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.SavedConfigurationService#getLastConfigForCurrentUser()
	 */
	@Override
	public DBObject getLastConfigForCurrentUser() {
		ImmutableMap<String, String> user = authenticationFacade.getPrincipal();

		if (user == null) {
			return null;
		}

		List<DBObject> list = mongoTemplate.getCollection(COLLECTION)
			.find(BasicDBObjectBuilder.start()
				.add("owner", user)
				.get())
			.sort(BasicDBObjectBuilder.start()
				.add("time", -1)
				.get())
			.limit(1)
			.toArray(1);

		if (list.isEmpty()) {
			return null;
		} else {
			return list.get(0);
		}
	}

	@Override
	public void saveTestConfigurationForCurrentUser(JsonObject config, String testName) {
		ImmutableMap<String, String> user = authenticationFacade.getPrincipal();

		if (user == null) {
			throw new IllegalStateException("No user found");
		}

		clearOldConfigs(user);

		DBObject document = BasicDBObjectBuilder.start()
			.add("_id", RandomStringUtils.randomAlphanumeric(30))
			.add("owner", user)
			.add("testName", testName)
			.add("config", config)
			.add("time", Instant.now().toString())
			.get();

		mongoTemplate.insert(document, COLLECTION);

	}

	@Override
	public void savePlanConfigurationForCurrentUser(JsonObject config, String planName) {
		ImmutableMap<String, String> user = authenticationFacade.getPrincipal();

		if (user == null) {
			throw new IllegalStateException("No user found");
		}

		clearOldConfigs(user);

		DBObject document = BasicDBObjectBuilder.start()
			.add("_id", RandomStringUtils.randomAlphanumeric(30))
			.add("owner", user)
			.add("planName", planName)
			.add("config", config)
			.add("time", Instant.now().toString())
			.get();

		mongoTemplate.insert(document, COLLECTION);

	}

	private void clearOldConfigs(ImmutableMap<String, String> user) {

		// TODO: save more than just the last one

		Criteria criteria = new Criteria();
		criteria.and("owner").is(user);

		Query query = new Query(criteria);

		mongoTemplate.remove(query, COLLECTION);

	}


}

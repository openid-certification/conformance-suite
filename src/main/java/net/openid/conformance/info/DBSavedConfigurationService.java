package net.openid.conformance.info;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.variant.VariantSelection;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DBSavedConfigurationService implements SavedConfigurationService {

	public static final String COLLECTION = "TEST_CONFIG";

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@Override
	public Document getLastConfigForCurrentUser() {
		ImmutableMap<String, String> user = authenticationFacade.getPrincipal();

		if (user == null) {
			return null;
		}

		return mongoTemplate.getCollection(COLLECTION)
			.find(new Document("owner", user))
			.sort(new Document("time", -1))
			.limit(1)
			.first();
	}

	@Override
	public void saveTestConfigurationForCurrentUser(JsonObject config, String testName, VariantSelection variant) {
		ImmutableMap<String, String> user = authenticationFacade.getPrincipal();

		if (user == null) {
			throw new IllegalStateException("No user found");
		}

		clearOldConfigs(user);

		DBObject document = BasicDBObjectBuilder.start()
			.add("_id", RandomStringUtils.secure().nextAlphanumeric(30))
			.add("owner", user)
			.add("testName", testName)
			.add("config", config)
			.add("variant", variant)
			.add("time", Instant.now().toString())
			.get();

		mongoTemplate.insert(document, COLLECTION);

	}

	@Override
	public void savePlanConfigurationForCurrentUser(JsonObject config, String planName, VariantSelection variant) {
		ImmutableMap<String, String> user = authenticationFacade.getPrincipal();

		if (user == null) {
			throw new IllegalStateException("No user found");
		}

		clearOldConfigs(user);

		DBObject document = BasicDBObjectBuilder.start()
			.add("_id", RandomStringUtils.secure().nextAlphanumeric(30))
			.add("owner", user)
			.add("planName", planName)
			.add("config", config)
			.add("variant", variant)
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

	@Override
	public long migrateOwnership(String oldIss, String oldSub) {

		ImmutableMap<String, String> newOwner = authenticationFacade.getPrincipal();
		ImmutableMap<String, String> owner = ImmutableMap.of(
			"sub", oldSub,
			"iss", oldIss
		);

		// A null new owner would rewrite every matching document's owner to null,
		// orphaning the records it was supposed to hand over.
		if (newOwner == null || newOwner.equals(owner)) {
			return 0;
		}

		Criteria criteria = Criteria.where("owner").is(owner);

		Query query = new Query(criteria);

		Update udt = Update.update("owner", newOwner);

		return mongoTemplate.updateMulti(query, udt, COLLECTION).getModifiedCount();
	}


}

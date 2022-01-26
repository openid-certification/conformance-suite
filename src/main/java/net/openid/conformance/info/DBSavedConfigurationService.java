package net.openid.conformance.info;

import java.time.Instant;

import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.variant.VariantSelection;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import net.openid.conformance.testmodule.OIDFJSON;

@Service
public class DBSavedConfigurationService implements SavedConfigurationService {

	public static final String COLLECTION = "TEST_CONFIG";

	static final String imdsUrl = "/meta-data/";

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	/* (non-Javadoc)
	 * @see SavedConfigurationService#getLastConfigForCurrentUser()
	 */
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

		if (ifExists(config, "$.resource.consentUrl")) {
            String consentUrl = OIDFJSON.getString(getByPath(config, "$.resource.consentUrl"));

			if ( consentUrl.toLowerCase().indexOf(imdsUrl.toLowerCase()) != -1 ) {
				throw new IllegalStateException("Bad URL");
			}

			if ( consentUrl.toLowerCase().indexOf("http://") != -1 ) {
				throw new IllegalStateException("Bad URL - only https is allowed");
			}
        }

		if (ifExists(config, "$.resource.resourceUrl")) {
            String resourceUrl = OIDFJSON.getString(getByPath(config, "$.resource.resourceUrl"));

			if ( resourceUrl.toLowerCase().indexOf(imdsUrl.toLowerCase()) != -1 ) {
				throw new IllegalStateException("Bad URL");
			}

			if ( resourceUrl.toLowerCase().indexOf("http://") != -1)  {
				throw new IllegalStateException("Bad URL - Only https is allowed");
			}
        }

		clearOldConfigs(user);

		DBObject document = BasicDBObjectBuilder.start()
			.add("_id", RandomStringUtils.randomAlphanumeric(30))
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

		if (ifExists(config, "$.resource.consentUrl")) {
            String consentUrl = OIDFJSON.getString(getByPath(config, "$.resource.consentUrl"));

			if ( consentUrl.toLowerCase().indexOf(imdsUrl.toLowerCase()) != -1 ) {
				throw new IllegalStateException("Bad URL");
			}

			if ( consentUrl.toLowerCase().indexOf("http://") != -1 ) {
				throw new IllegalStateException("Bad URL - only https is allowed");
			}
        }

		if (ifExists(config, "$.resource.resourceUrl")) {
            String resourceUrl = OIDFJSON.getString(getByPath(config, "$.resource.resourceUrl"));

			if ( resourceUrl.toLowerCase().indexOf(imdsUrl.toLowerCase()) != -1 ) {
				throw new IllegalStateException("Bad URL");
			}

			if ( resourceUrl.toLowerCase().indexOf("http://") != -1)  {
				throw new IllegalStateException("Bad URL - Only https is allowed");
			}
        }


		clearOldConfigs(user);

		DBObject document = BasicDBObjectBuilder.start()
			.add("_id", RandomStringUtils.randomAlphanumeric(30))
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

	static public boolean ifExists(JsonObject jsonObject, String path) {
		try {
			JsonPath.read(jsonObject, path);
			return true;
		} catch (PathNotFoundException | IllegalArgumentException ex) {
			return false;
		}
	}

	private JsonElement getByPath(JsonObject jsonObject, String path ) {

		try {
			JsonElement element = JsonPath.parse(jsonObject).read(path);
			return element;
		} catch (PathNotFoundException e) {
			throw new IllegalStateException("Json Path not found");
		}
	}

}

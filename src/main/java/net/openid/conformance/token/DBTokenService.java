package net.openid.conformance.token;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import net.openid.conformance.security.AuthenticationFacade;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@SuppressWarnings("rawtypes")
public class DBTokenService implements TokenService {

	public static final String COLLECTION = "API_TOKEN";

	public static final long DEFAULT_TTL_MS = 24 * 60 * 60 * 1000;
	public static final int TOKEN_BYTES = 64;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@Override
	public Map createToken(boolean permanent) {

		String id = RandomStringUtils.secure().nextAlphanumeric(13);

		byte[] tokenBytes = new byte[TOKEN_BYTES];
		new SecureRandom().nextBytes(tokenBytes);

		Document token = new Document()
				.append("_id", id)
				.append("owner", authenticationFacade.getPrincipal())
				.append("info", null)
				.append("token", Base64.getEncoder().encodeToString(tokenBytes))
				.append("expires", permanent ? null : System.currentTimeMillis() + DEFAULT_TTL_MS);

		mongoTemplate.insert(token, COLLECTION);
		return token;
	}

	@Override
	public boolean deleteToken(String id) {

		Criteria criteria = new Criteria("_id").is(id);
		criteria.and("owner").is(authenticationFacade.getPrincipal());
		Query query = new Query(criteria);
		// wasAcknowledged() is true even when nothing matched; only a positive count is a delete
		return mongoTemplate.remove(query, COLLECTION).getDeletedCount() > 0;
	}

	@Override
	public List<Map> getAllTokens() {

		Criteria criteria = new Criteria("owner").is(authenticationFacade.getPrincipal());

		Query query = new Query(criteria);
		query.fields()
				.include("_id")
				.include("expires");

		return Lists.newArrayList(mongoTemplate.getCollection(COLLECTION).find(query.getQueryObject()).projection(query.getFieldsObject()));
	}

	@Override
	public Map<String,Object> findToken(String token) {

		Criteria criteria = new Criteria("token").is(token);
		Query query = new Query(criteria);

		return mongoTemplate.getCollection(COLLECTION).find(query.getQueryObject()).first();
	}

	@Override
	public void createIndexes() {

		MongoCollection<Document> collection = mongoTemplate.getCollection(COLLECTION);
		collection.createIndex(new Document("owner", 1));
		collection.createIndex(new Document("token", 1), new IndexOptions().unique(true));
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

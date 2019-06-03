package io.fintechlabs.testframework.token;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.mitre.openid.connect.model.DefaultUserInfo;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.mitre.openid.connect.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;

import io.fintechlabs.testframework.security.AuthenticationFacade;

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

		String id = RandomStringUtils.randomAlphanumeric(13);

		byte[] tokenBytes = new byte[TOKEN_BYTES];
		new SecureRandom().nextBytes(tokenBytes);

		BasicDBObject token = (BasicDBObject) BasicDBObjectBuilder.start()
				.add("_id", id)
				.add("owner", authenticationFacade.getPrincipal())
				.add("info", JSON.parse(authenticationFacade.getUserInfo().toJson().toString()))
				.add("token", Base64Utils.encodeToString(tokenBytes))
				.add("expires", permanent ? null : System.currentTimeMillis() + DEFAULT_TTL_MS)
				.get();

		WriteResult result = mongoTemplate.getCollection(COLLECTION).insert(token);
		if (result.wasAcknowledged()) {
			return token.toMap();
		} else {
			return null;
		}
	}

	@Override
	public boolean deleteToken(String id) {

		Criteria criteria = new Criteria("_id").is(id);
		criteria.and("owner").is(authenticationFacade.getPrincipal());
		Query query = new Query(criteria);
		return mongoTemplate.getCollection(COLLECTION).remove(query.getQueryObject()).wasAcknowledged();
	}

	@Override
	public List<Map> getAllTokens() {

		Criteria criteria = new Criteria("owner").is(authenticationFacade.getPrincipal());

		Query query = new Query(criteria);
		query.fields()
				.include("_id")
				.include("expires");

		List<DBObject> results = mongoTemplate.getCollection(COLLECTION).find(query.getQueryObject(), query.getFieldsObject()).toArray();
		return results.stream().map(DBObject::toMap).collect(Collectors.toList());
	}

	@Override
	public Authentication getAuthenticationForToken(String token) {

		Criteria criteria = new Criteria("token").is(token);
		criteria.andOperator(
				new Criteria().orOperator(
						new Criteria("expires").is(null),
						new Criteria("expires").gt(System.currentTimeMillis())));
		Query query = new Query(criteria);

		DBObject result = mongoTemplate.getCollection(COLLECTION).findOne(query.getQueryObject());

		if (result != null) {
			BasicDBObject owner = (BasicDBObject) result.get("owner");
			String iss = owner.getString("iss");
			String sub = owner.getString("sub");
			Set<GrantedAuthority> authorities = ImmutableSet.of(new SimpleGrantedAuthority("ROLE_USER"));
			UserInfo info = DefaultUserInfo.fromJson(new Gson().fromJson(((BasicDBObject) result.get("info")).toJson(), JsonObject.class));
			return new OIDCAuthenticationToken(sub, iss, info, authorities, null, null, null);
		} else {
			return null;
		}
	}

	@Override
	public void createIndexes() {

		DBCollection collection = mongoTemplate.getCollection(COLLECTION);
		collection.createIndex(new BasicDBObject("owner", 1));
		collection.createIndex(new BasicDBObject("token", 1), new BasicDBObject("unique", true));
	}
}

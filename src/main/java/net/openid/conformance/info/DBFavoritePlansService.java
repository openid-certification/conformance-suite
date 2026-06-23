package net.openid.conformance.info;

import com.google.common.collect.ImmutableMap;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import net.openid.conformance.security.AuthenticationFacade;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Mongo-backed {@link FavoritePlansService}. Stores one document per favorited plan in the
 * {@value #COLLECTION} collection:
 *
 * <pre>{ _id: &lt;random30&gt;, owner: {sub, iss}, planName: &lt;String&gt;, addedAt: &lt;ISO-8601 Instant&gt; }</pre>
 *
 * <p>This is multi-record-per-owner, mirroring {@code DBTokenService}, rather than the
 * single-latest-record approach of {@link DBSavedConfigurationService}.
 */
@Service
public class DBFavoritePlansService implements FavoritePlansService {

	public static final String COLLECTION = "FAVORITE_PLAN";

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@Override
	public List<String> getFavoritePlansForCurrentUser() {
		ImmutableMap<String, String> user = authenticationFacade.getPrincipal();

		if (user == null) {
			return new ArrayList<>();
		}

		Query query = new Query(new Criteria("owner").is(user));
		query.with(Sort.by(Sort.Direction.ASC, "addedAt"));

		List<Document> docs = mongoTemplate.find(query, Document.class, COLLECTION);

		List<String> planNames = new ArrayList<>();
		for (Document doc : docs) {
			planNames.add(doc.getString("planName"));
		}
		return planNames;
	}

	@Override
	public List<String> addFavoritePlanForCurrentUser(String planName) {
		ImmutableMap<String, String> user = authenticationFacade.getPrincipal();

		if (user == null) {
			throw new IllegalStateException("No user found");
		}

		Query existing = new Query(new Criteria("owner").is(user).and("planName").is(planName));

		if (!mongoTemplate.exists(existing, COLLECTION)) {
			Document document = new Document()
				.append("_id", RandomStringUtils.secure().nextAlphanumeric(30))
				.append("owner", user)
				.append("planName", planName)
				.append("addedAt", Instant.now().toString());

			try {
				mongoTemplate.insert(document, COLLECTION);
			} catch (MongoWriteException e) {
				// A concurrent request inserted the same owner+planName first; the unique compound
				// index rejected this duplicate. Adding is idempotent, so treat this as success.
				if (e.getError().getCategory() != ErrorCategory.DUPLICATE_KEY) {
					throw e;
				}
			}
		}

		return getFavoritePlansForCurrentUser();
	}

	@Override
	public List<String> removeFavoritePlanForCurrentUser(String planName) {
		ImmutableMap<String, String> user = authenticationFacade.getPrincipal();

		if (user == null) {
			throw new IllegalStateException("No user found");
		}

		Query query = new Query(new Criteria("owner").is(user).and("planName").is(planName));
		mongoTemplate.remove(query, COLLECTION);

		return getFavoritePlansForCurrentUser();
	}

	@Override
	public void createIndexes() {
		MongoCollection<Document> collection = mongoTemplate.getCollection(COLLECTION);
		collection.createIndex(new Document("owner", 1));
		collection.createIndex(
			new Document("owner", 1).append("planName", 1),
			new IndexOptions().unique(true));
	}

}

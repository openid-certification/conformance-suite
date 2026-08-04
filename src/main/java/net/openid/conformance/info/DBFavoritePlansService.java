package net.openid.conformance.info;

import com.google.common.collect.ImmutableMap;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import net.openid.conformance.security.AuthenticationFacade;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Mongo-backed {@link FavoritePlansService}. Stores one document per favorited plan in the
 * {@value #COLLECTION} collection:
 *
 * <pre>{ _id: &lt;random30&gt;, owner: {sub, iss}, planName: &lt;String&gt;, addedAt: &lt;BSON date&gt; }</pre>
 *
 * <p>{@code addedAt} is a BSON date rather than an ISO-8601 string because the list is sorted on
 * it: {@code Instant.toString()} emits 0, 3, 6 or 9 fractional digits depending on the value, so
 * lexicographic ordering inverts roughly one timestamp in a thousand (the {@code Z} of a
 * whole-second stamp sorts after the digits of a sub-second one).
 *
 * <p>This is multi-record-per-owner, mirroring {@code DBTokenService}, rather than the
 * single-latest-record approach of {@link DBSavedConfigurationService}.
 *
 * <p><b>Private-link viewers.</b> Every method here keys on
 * {@code AuthenticationFacade.getPrincipal()}, and for a private-link viewer
 * {@code OIDCAuthenticationFacade.getPrincipal()} returns the SHARED ASSET OWNER — not the viewer.
 * A private-link viewer reaching these methods would therefore read and mutate the sharer's
 * favorites. That is prevented one layer up: the private-link rule in
 * {@code WebSecurityResourceServerConfig} denies every API route outside a small read-only
 * allowlist, and {@code /api/favorite-plans} is deliberately not on it. Do not add it.
 * {@code FavoritePlansEndpointSecurity_UnitTest} pins both halves of that.
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

		// Cap what one account can write through this API. The check is advisory: two adds racing
		// at the boundary can both pass it and leave the user one over. That is fine — the bound
		// exists to stop unbounded growth, and the unique owner+planName index means the real
		// ceiling is the number of distinct plan names in any case.
		Query owned = new Query(new Criteria("owner").is(user));
		if (mongoTemplate.count(owned, COLLECTION) >= MAX_FAVORITES_PER_USER) {
			// Only re-adding something already favorited is still allowed at the cap, so a client
			// replaying an add never turns into an error.
			Query existing = new Query(new Criteria("owner").is(user).and("planName").is(planName));
			if (!mongoTemplate.exists(existing, COLLECTION)) {
				// Surfaced verbatim to the user in the picker's failure toast, so it names the
				// limit and the way out rather than reading as an internal diagnostic.
				throw new FavoritePlansLimitExceededException("You can save up to "
					+ MAX_FAVORITES_PER_USER + " favorite test plans. Unstar one to make room.");
			}
		}

		// A single atomic upsert keyed on owner+planName, rather than exists()-then-insert: a
		// double-clicked star fires two concurrent adds, and the read-modify-write version let
		// both observe "absent" and both insert. Every field is setOnInsert, so re-adding an
		// existing favorite is a genuine no-op that preserves the original addedAt (the list is
		// ordered by it) instead of bumping the plan to the end of the list.
		Query query = new Query(new Criteria("owner").is(user).and("planName").is(planName));
		Update update = new Update()
			.setOnInsert("_id", RandomStringUtils.secure().nextAlphanumeric(30))
			.setOnInsert("owner", user)
			.setOnInsert("planName", planName)
			.setOnInsert("addedAt", Date.from(Instant.now()));

		try {
			mongoTemplate.upsert(query, update, COLLECTION);
		} catch (DuplicateKeyException e) {
			// MongoDB documents a residual race for upserts against a unique index: two
			// simultaneous upserts can both miss the match stage and one loses to the index.
			// Adding is idempotent, so the loser's outcome is already the desired state.
			// (Not the exists()-race this replaces — that one is gone; this is the narrower
			// server-side window, and the caller still gets the correct list below.)
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

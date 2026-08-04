package net.openid.conformance.info;

import com.google.common.collect.ImmutableMap;
import net.openid.conformance.security.AuthenticationFacade;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class DBFavoritePlansService_UnitTest {

	private static final ImmutableMap<String, String> USER =
		ImmutableMap.of("sub", "the-subject", "iss", "https://issuer.example.org/");

	private MongoTemplate mongoTemplate;
	private AuthenticationFacade authenticationFacade;
	private DBFavoritePlansService service;

	@BeforeEach
	public void setUp() {
		mongoTemplate = Mockito.mock(MongoTemplate.class);
		authenticationFacade = Mockito.mock(AuthenticationFacade.class);
		service = new DBFavoritePlansService();
		ReflectionTestUtils.setField(service, "mongoTemplate", mongoTemplate);
		ReflectionTestUtils.setField(service, "authenticationFacade", authenticationFacade);
	}

	private static Document favoriteDoc(String planName, String addedAt) {
		return new Document()
			.append("_id", "id-" + planName)
			.append("owner", USER)
			.append("planName", planName)
			.append("addedAt", addedAt);
	}

	// --- get ---

	@Test
	public void getWithNoPrincipalReturnsEmptyList() {
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(null);

		List<String> result = service.getFavoritePlansForCurrentUser();

		assertThat(result).isEmpty();
		Mockito.verifyNoInteractions(mongoTemplate);
	}

	@Test
	public void getMapsDocumentsToPlanNamesInAddedAtOrder() {
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(USER);
		// The service sorts by addedAt asc in the query; the mock returns the already-sorted list
		// that Mongo would produce, and we assert the mapping preserves that order.
		Mockito.when(mongoTemplate.find(any(Query.class), eq(Document.class),
				eq(DBFavoritePlansService.COLLECTION)))
			.thenReturn(List.of(
				favoriteDoc("plan-a", "2026-06-22T10:00:00Z"),
				favoriteDoc("plan-b", "2026-06-22T11:00:00Z"),
				favoriteDoc("plan-c", "2026-06-22T12:00:00Z")));

		List<String> result = service.getFavoritePlansForCurrentUser();

		assertThat(result).containsExactly("plan-a", "plan-b", "plan-c");

		ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
		Mockito.verify(mongoTemplate).find(queryCaptor.capture(), eq(Document.class),
			eq(DBFavoritePlansService.COLLECTION));
		Query query = queryCaptor.getValue();
		assertThat(query.getQueryObject().get("owner")).isEqualTo(USER);
		// Sort is ascending on addedAt.
		assertThat(query.getSortObject().get("addedAt")).isEqualTo(1);
	}

	@Test
	public void getWithNoFavoritesReturnsEmptyList() {
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(USER);
		Mockito.when(mongoTemplate.find(any(Query.class), eq(Document.class),
				eq(DBFavoritePlansService.COLLECTION)))
			.thenReturn(List.of());

		assertThat(service.getFavoritePlansForCurrentUser()).isEmpty();
	}

	// --- add ---

	@Test
	public void addWithNoPrincipalThrows() {
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(null);

		assertThatThrownBy(() -> service.addFavoritePlanForCurrentUser("plan-a"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("No user found");

		Mockito.verifyNoInteractions(mongoTemplate);
	}

	@Test
	public void addUpsertsSetOnInsertDocumentKeyedOnOwnerAndPlanName() {
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(USER);
		// The post-write re-read returns the new favorite.
		Mockito.when(mongoTemplate.find(any(Query.class), eq(Document.class),
				eq(DBFavoritePlansService.COLLECTION)))
			.thenReturn(List.of(favoriteDoc("plan-a", "2026-06-22T10:00:00Z")));

		List<String> result = service.addFavoritePlanForCurrentUser("plan-a");

		assertThat(result).containsExactly("plan-a");

		ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
		ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
		Mockito.verify(mongoTemplate).upsert(queryCaptor.capture(), updateCaptor.capture(),
			eq(DBFavoritePlansService.COLLECTION));

		Document criteria = queryCaptor.getValue().getQueryObject();
		assertThat(criteria.get("owner")).isEqualTo(USER);
		assertThat(criteria.get("planName")).isEqualTo("plan-a");

		Document setOnInsert = (Document) updateCaptor.getValue().getUpdateObject()
			.get("$setOnInsert");
		assertThat(setOnInsert.getString("planName")).isEqualTo("plan-a");
		assertThat(setOnInsert.get("owner")).isEqualTo(USER);
		assertThat(setOnInsert.getString("_id")).hasSize(30);
		assertThat(setOnInsert.get("addedAt")).isNotNull();
	}

	@Test
	public void addDoesNotReadBeforeWriting() {
		// The write must be a single atomic upsert, not exists()-then-insert: the
		// read-modify-write version let two concurrent adds of the same plan both observe
		// "absent" and both attempt an insert, so one 500'd on the unique index.
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(USER);
		Mockito.when(mongoTemplate.find(any(Query.class), eq(Document.class),
				eq(DBFavoritePlansService.COLLECTION)))
			.thenReturn(List.of(favoriteDoc("plan-a", "2026-06-22T10:00:00Z")));

		service.addFavoritePlanForCurrentUser("plan-a");

		Mockito.verify(mongoTemplate, Mockito.never())
			.exists(any(Query.class), eq(DBFavoritePlansService.COLLECTION));
		Mockito.verify(mongoTemplate, Mockito.never())
			.insert(any(Document.class), eq(DBFavoritePlansService.COLLECTION));
	}

	@Test
	public void addIsIdempotentWhenAlreadyFavorited() {
		// Re-adding an existing favorite issues the same upsert; every field is setOnInsert, so
		// the matched document is untouched and the original addedAt (the list order) survives.
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(USER);
		Mockito.when(mongoTemplate.find(any(Query.class), eq(Document.class),
				eq(DBFavoritePlansService.COLLECTION)))
			.thenReturn(List.of(favoriteDoc("plan-a", "2026-06-22T10:00:00Z")));

		List<String> result = service.addFavoritePlanForCurrentUser("plan-a");

		assertThat(result).containsExactly("plan-a");
		ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
		Mockito.verify(mongoTemplate).upsert(any(Query.class), updateCaptor.capture(),
			eq(DBFavoritePlansService.COLLECTION));
		// No $set: nothing about an existing favorite may be rewritten.
		assertThat(updateCaptor.getValue().getUpdateObject().keySet())
			.containsExactly("$setOnInsert");
	}

	@Test
	public void addSurvivesConcurrentDuplicateKeyFromUpsert() {
		// MongoDB documents a residual race for upserts against a unique index: two simultaneous
		// upserts can both miss the match stage and one loses to the index. Spring Data
		// translates that into DuplicateKeyException. Adding is idempotent, so the loser must
		// still succeed and return the (already correct) list rather than 500 the request.
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(USER);
		Mockito.when(mongoTemplate.upsert(any(Query.class), any(Update.class),
				eq(DBFavoritePlansService.COLLECTION)))
			.thenThrow(new DuplicateKeyException("E11000 duplicate key error"));
		Mockito.when(mongoTemplate.find(any(Query.class), eq(Document.class),
				eq(DBFavoritePlansService.COLLECTION)))
			.thenReturn(List.of(favoriteDoc("plan-a", "2026-06-22T10:00:00Z")));

		List<String> result = service.addFavoritePlanForCurrentUser("plan-a");

		assertThat(result).containsExactly("plan-a");
	}

	// --- remove ---

	@Test
	public void removeWithNoPrincipalThrows() {
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(null);

		assertThatThrownBy(() -> service.removeFavoritePlanForCurrentUser("plan-a"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("No user found");

		Mockito.verifyNoInteractions(mongoTemplate);
	}

	@Test
	public void removeIssuesQueryByOwnerAndPlanNameAndReturnsUpdatedList() {
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(USER);
		Mockito.when(mongoTemplate.find(any(Query.class), eq(Document.class),
				eq(DBFavoritePlansService.COLLECTION)))
			.thenReturn(List.of(favoriteDoc("plan-b", "2026-06-22T11:00:00Z")));

		List<String> result = service.removeFavoritePlanForCurrentUser("plan-a");

		assertThat(result).containsExactly("plan-b");

		ArgumentCaptor<Query> removeCaptor = ArgumentCaptor.forClass(Query.class);
		Mockito.verify(mongoTemplate).remove(removeCaptor.capture(),
			eq(DBFavoritePlansService.COLLECTION));
		Document criteria = removeCaptor.getValue().getQueryObject();
		assertThat(criteria.get("owner")).isEqualTo(USER);
		assertThat(criteria.get("planName")).isEqualTo("plan-a");
	}
}

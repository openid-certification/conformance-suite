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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
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
	public void addInsertsDocumentWithExpectedFieldsWhenNotPresent() {
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(USER);
		Mockito.when(mongoTemplate.exists(any(Query.class),
				eq(DBFavoritePlansService.COLLECTION)))
			.thenReturn(false);
		// The post-insert re-read returns the new favorite.
		Mockito.when(mongoTemplate.find(any(Query.class), eq(Document.class),
				eq(DBFavoritePlansService.COLLECTION)))
			.thenReturn(List.of(favoriteDoc("plan-a", "2026-06-22T10:00:00Z")));

		List<String> result = service.addFavoritePlanForCurrentUser("plan-a");

		assertThat(result).containsExactly("plan-a");

		ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
		Mockito.verify(mongoTemplate).insert(docCaptor.capture(),
			eq(DBFavoritePlansService.COLLECTION));
		Document inserted = docCaptor.getValue();
		assertThat(inserted.getString("planName")).isEqualTo("plan-a");
		assertThat(inserted.get("owner")).isEqualTo(USER);
		assertThat(inserted.getString("_id")).hasSize(30);
		assertThat(inserted.getString("addedAt")).isNotBlank();
	}

	@Test
	public void addIsIdempotentAndDoesNotDoubleInsert() {
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(USER);
		// Already favorited: exists() returns true.
		Mockito.when(mongoTemplate.exists(any(Query.class),
				eq(DBFavoritePlansService.COLLECTION)))
			.thenReturn(true);
		Mockito.when(mongoTemplate.find(any(Query.class), eq(Document.class),
				eq(DBFavoritePlansService.COLLECTION)))
			.thenReturn(List.of(favoriteDoc("plan-a", "2026-06-22T10:00:00Z")));

		List<String> result = service.addFavoritePlanForCurrentUser("plan-a");

		assertThat(result).containsExactly("plan-a");
		Mockito.verify(mongoTemplate, Mockito.never())
			.insert(any(Document.class), eq(DBFavoritePlansService.COLLECTION));
	}

	@Test
	public void addQueriesExistenceByOwnerAndPlanName() {
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(USER);
		Mockito.when(mongoTemplate.exists(any(Query.class),
				eq(DBFavoritePlansService.COLLECTION)))
			.thenReturn(true);
		Mockito.when(mongoTemplate.find(any(Query.class), eq(Document.class),
				eq(DBFavoritePlansService.COLLECTION)))
			.thenReturn(List.of());

		service.addFavoritePlanForCurrentUser("plan-a");

		ArgumentCaptor<Query> existsCaptor = ArgumentCaptor.forClass(Query.class);
		Mockito.verify(mongoTemplate).exists(existsCaptor.capture(),
			eq(DBFavoritePlansService.COLLECTION));
		Document criteria = existsCaptor.getValue().getQueryObject();
		assertThat(criteria.get("owner")).isEqualTo(USER);
		assertThat(criteria.get("planName")).isEqualTo("plan-a");
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

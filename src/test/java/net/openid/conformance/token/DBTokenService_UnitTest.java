package net.openid.conformance.token;

import com.google.common.collect.ImmutableMap;
import com.mongodb.client.result.UpdateResult;
import net.openid.conformance.security.AuthenticationFacade;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * An API token keeps authenticating after its owner's first IdP login, because the
 * token value itself is unchanged — but every read it performs is scoped to the
 * owner sub-document. Left unmigrated it owns nothing: /api/plan, /api/log and
 * /api/info all come back empty, and the token disappears from getAllTokens() so
 * the user cannot even see or delete it. That failure is silent, which is why it is
 * pinned here.
 */
public class DBTokenService_UnitTest {

	private static final String LEGACY_ISS = "https://accounts.google.com";
	private static final String LEGACY_SUB = "legacy-user-id";
	private static final ImmutableMap<String, String> LEGACY_OWNER =
		ImmutableMap.of("sub", LEGACY_SUB, "iss", LEGACY_ISS);
	private static final ImmutableMap<String, String> NEW_OWNER =
		ImmutableMap.of("sub", "current-user-id", "iss", "https://idp.example.com");

	private MongoTemplate mongoTemplate;
	private AuthenticationFacade authenticationFacade;
	private DBTokenService service;

	@BeforeEach
	public void setUp() {
		mongoTemplate = Mockito.mock(MongoTemplate.class);
		authenticationFacade = Mockito.mock(AuthenticationFacade.class);
		service = new DBTokenService();
		ReflectionTestUtils.setField(service, "mongoTemplate", mongoTemplate);
		ReflectionTestUtils.setField(service, "authenticationFacade", authenticationFacade);
	}

	private void stubUpdate(long modified) {
		Mockito.when(mongoTemplate.updateMulti(Mockito.any(Query.class), Mockito.any(Update.class),
				Mockito.eq(DBTokenService.COLLECTION)))
			.thenReturn(UpdateResult.acknowledged(modified, modified, null));
	}

	@Test
	public void migrates_the_tokens_owned_by_the_legacy_identity() {
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(NEW_OWNER);
		stubUpdate(4);

		Assertions.assertEquals(4, service.migrateOwnership(LEGACY_ISS, LEGACY_SUB));
	}

	/**
	 * MongoDB matches embedded documents by exact key order, so the legacy owner has
	 * to be rebuilt as {sub, iss} — the order getPrincipal() wrote. A query built the
	 * other way round matches nothing while still reporting success.
	 */
	@Test
	public void matches_the_legacy_owner_and_writes_the_new_one() {
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(NEW_OWNER);
		stubUpdate(1);

		service.migrateOwnership(LEGACY_ISS, LEGACY_SUB);

		ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
		ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
		Mockito.verify(mongoTemplate).updateMulti(query.capture(), update.capture(),
			Mockito.eq(DBTokenService.COLLECTION));

		Assertions.assertEquals(LEGACY_OWNER, query.getValue().getQueryObject().get("owner"));
		Assertions.assertEquals(NEW_OWNER,
			((org.bson.Document) update.getValue().getUpdateObject().get("$set")).get("owner"));
	}

	/**
	 * Writing a null owner would orphan every token the migration was supposed to
	 * hand over — nobody could reach them again.
	 */
	@Test
	public void nothing_is_written_when_there_is_no_authenticated_principal() {
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(null);

		Assertions.assertEquals(0, service.migrateOwnership(LEGACY_ISS, LEGACY_SUB));
		Mockito.verify(mongoTemplate, Mockito.never())
			.updateMulti(Mockito.any(), Mockito.any(), Mockito.anyString());
	}

	@Test
	public void nothing_is_written_when_the_legacy_identity_is_already_the_current_one() {
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(LEGACY_OWNER);

		Assertions.assertEquals(0, service.migrateOwnership(LEGACY_ISS, LEGACY_SUB));
		Mockito.verify(mongoTemplate, Mockito.never())
			.updateMulti(Mockito.any(), Mockito.any(), Mockito.anyString());
	}
}

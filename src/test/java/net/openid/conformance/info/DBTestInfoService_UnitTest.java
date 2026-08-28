package net.openid.conformance.info;

import com.google.common.collect.ImmutableMap;
import com.mongodb.client.result.UpdateResult;
import net.openid.conformance.logging.DBEventLog;
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
 * A user's tests and their log entries are access-controlled independently: tests
 * carry an "owner", while EVENT_LOG documents carry their own "testOwner" — and
 * uploaded screenshots are EVENT_LOG documents too. Migrating only TEST_INFO leaves
 * the user seeing their tests in the listings but getting an empty log and no
 * screenshots when they open one, so these tests pin that both move together.
 */
public class DBTestInfoService_UnitTest {

	private static final String LEGACY_ISS = "https://accounts.google.com";
	private static final String LEGACY_SUB = "legacy-user-id";
	private static final ImmutableMap<String, String> LEGACY_OWNER =
		ImmutableMap.of("sub", LEGACY_SUB, "iss", LEGACY_ISS);
	private static final ImmutableMap<String, String> NEW_OWNER =
		ImmutableMap.of("sub", "current-user-id", "iss", "https://idp.example.com");

	private MongoTemplate mongoTemplate;
	private AuthenticationFacade authenticationFacade;
	private DBTestInfoService service;

	@BeforeEach
	public void setUp() {
		mongoTemplate = Mockito.mock(MongoTemplate.class);
		authenticationFacade = Mockito.mock(AuthenticationFacade.class);
		service = new DBTestInfoService();
		ReflectionTestUtils.setField(service, "mongoTemplate", mongoTemplate);
		ReflectionTestUtils.setField(service, "authenticationFacade", authenticationFacade);
	}

	private void stubUpdate(String collection, long modified) {
		Mockito.when(mongoTemplate.updateMulti(Mockito.any(Query.class), Mockito.any(Update.class),
				Mockito.eq(collection)))
			.thenReturn(UpdateResult.acknowledged(modified, modified, null));
	}

	@Test
	public void migrates_tests_and_their_log_entries_together() {
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(NEW_OWNER);
		stubUpdate(DBTestInfoService.COLLECTION, 3);
		stubUpdate(DBEventLog.COLLECTION, 128);

		TestInfoService.MigrationCounts counts = service.migrateOwnership(LEGACY_ISS, LEGACY_SUB);

		Assertions.assertEquals(3, counts.tests());
		Assertions.assertEquals(128, counts.logEntries(),
			"log entries — and the screenshots stored among them — must move with the test");
	}

	/**
	 * The two collections name the owner differently, and a query built on the wrong
	 * field would silently match nothing while still reporting success.
	 */
	@Test
	public void each_collection_is_matched_and_updated_on_its_own_owner_field() {
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(NEW_OWNER);
		stubUpdate(DBTestInfoService.COLLECTION, 1);
		stubUpdate(DBEventLog.COLLECTION, 1);

		service.migrateOwnership(LEGACY_ISS, LEGACY_SUB);

		ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
		ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);

		Mockito.verify(mongoTemplate).updateMulti(query.capture(), update.capture(),
			Mockito.eq(DBEventLog.COLLECTION));
		Assertions.assertEquals(LEGACY_OWNER, query.getValue().getQueryObject().get("testOwner"));
		Assertions.assertEquals(NEW_OWNER,
			((org.bson.Document) update.getValue().getUpdateObject().get("$set")).get("testOwner"));

		Mockito.verify(mongoTemplate).updateMulti(query.capture(), update.capture(),
			Mockito.eq(DBTestInfoService.COLLECTION));
		Assertions.assertEquals(LEGACY_OWNER, query.getValue().getQueryObject().get("owner"));
		Assertions.assertEquals(NEW_OWNER,
			((org.bson.Document) update.getValue().getUpdateObject().get("$set")).get("owner"));
	}

	/**
	 * getTestOwner answers from this cache for up to 30 minutes. Left populated, the
	 * owner checks in ImageApi keep comparing against the identity that was just
	 * replaced, so screenshots stay inaccessible long after the migration ran.
	 */
	@Test
	public void the_test_owner_cache_is_dropped_once_records_have_moved() {
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(NEW_OWNER);
		stubUpdate(DBTestInfoService.COLLECTION, 1);
		stubUpdate(DBEventLog.COLLECTION, 0);
		service.testOwnerCache.put("a-test-id", LEGACY_OWNER);

		service.migrateOwnership(LEGACY_ISS, LEGACY_SUB);

		Assertions.assertNull(service.testOwnerCache.getIfPresent("a-test-id"));
	}

	@Test
	public void the_cache_is_left_alone_when_nothing_moved() {
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(NEW_OWNER);
		stubUpdate(DBTestInfoService.COLLECTION, 0);
		stubUpdate(DBEventLog.COLLECTION, 0);
		service.testOwnerCache.put("a-test-id", NEW_OWNER);

		Assertions.assertTrue(service.migrateOwnership(LEGACY_ISS, LEGACY_SUB).movedNothing());
		Assertions.assertEquals(NEW_OWNER, service.testOwnerCache.getIfPresent("a-test-id"));
	}

	/**
	 * Writing a null owner would orphan every record the migration was supposed to
	 * hand over — nobody could reach them again.
	 */
	@Test
	public void nothing_is_written_when_there_is_no_authenticated_principal() {
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(null);

		Assertions.assertTrue(service.migrateOwnership(LEGACY_ISS, LEGACY_SUB).movedNothing());
		Mockito.verify(mongoTemplate, Mockito.never())
			.updateMulti(Mockito.any(), Mockito.any(), Mockito.anyString());
	}

	@Test
	public void nothing_is_written_when_the_legacy_identity_is_already_the_current_one() {
		Mockito.when(authenticationFacade.getPrincipal()).thenReturn(LEGACY_OWNER);

		Assertions.assertTrue(service.migrateOwnership(LEGACY_ISS, LEGACY_SUB).movedNothing());
		Mockito.verify(mongoTemplate, Mockito.never())
			.updateMulti(Mockito.any(), Mockito.any(), Mockito.anyString());
	}
}

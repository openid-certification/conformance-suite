package net.openid.conformance.info;

import com.google.gson.JsonObject;
import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.variant.VariantSelection;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class TemporaryOfbrCibaParticipantPlanApi_UnitTest {

	private MongoTemplate mongoTemplate;
	private AuthenticationFacade authenticationFacade;

	@BeforeEach
	public void setUp() {
		mongoTemplate = Mockito.mock(MongoTemplate.class);
		authenticationFacade = Mockito.mock(AuthenticationFacade.class);
	}

	@Test
	public void rejectsNonAdminsWithoutQueryingMongo() {
		Mockito.when(authenticationFacade.isAdmin()).thenReturn(false);

		var response = api("https://localhost.emobix.co.uk:8443").getParticipantPlans("50");

		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
		verifyNoInteractions(mongoTemplate);
	}

	@ParameterizedTest
	@CsvSource({
		"https://localhost.emobix.co.uk:8443, https://localhost.emobix.co.uk:8443/plan-detail.html?plan=plan1",
		"https://www.certification.openid.net, https://www.certification.openid.net/plan-detail.html?plan=plan1"
	})
	public void filtersPlansAndReturnsEnvironmentSpecificAbsolutePlanUrls(String baseUrl, String expectedPlanUrl) {
		Mockito.when(authenticationFacade.isAdmin()).thenReturn(true);
		Mockito.when(mongoTemplate.find(Mockito.any(Query.class), eq(Plan.class), eq(DBTestPlanService.COLLECTION)))
			.thenReturn(List.of(plan()));

		var response = api(baseUrl).getParticipantPlans("25");

		assertEquals(HttpStatus.OK, response.getStatusCode());
		var body = response.getBody();
		assertNotNull(body);
		assertEquals(25, body.limit());
		assertEquals(expectedPlanUrl, body.data().get(0).planUrl());
		assertEquals("OFB-CIBARPtest", body.data().get(0).description());

		ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
		verify(mongoTemplate).find(queryCaptor.capture(), eq(Plan.class), eq(DBTestPlanService.COLLECTION));
		Query query = queryCaptor.getValue();
		assertEquals(25, query.getLimit());
		assertEquals(new Document("started", -1), query.getSortObject());
		assertEquals(new Document("_id", 1).append("description", 1).append("started", 1).append("planName", 1),
			query.getFieldsObject());
		String queryJson = query.getQueryObject().toJson();
		assertTrue(queryJson.contains("fapi-ciba-id1-test-plan"));
		assertTrue(queryJson.contains("fapi-ciba-id1-client-test-plan"));
		assertTrue(queryJson.contains("variant.fapi_ciba_profile"));
		assertTrue(queryJson.contains("variant.variant.fapi_ciba_profile"));
		assertTrue(queryJson.contains("openbanking_brazil"));
		assertTrue(queryJson.contains("OFB-[A-Za-z0-9][A-Za-z0-9_-]*"));
	}

	@ParameterizedTest
	@CsvSource(value = {
		"NULL, 50",
		"'', 50",
		"0, 50",
		"-1, 50",
		"invalid, 50",
		"999, 500"
	}, nullValues = "NULL")
	public void normalizesLimits(String requestedLimit, int expectedLimit) {
		Mockito.when(authenticationFacade.isAdmin()).thenReturn(true);
		Mockito.when(mongoTemplate.find(Mockito.any(Query.class), eq(Plan.class), eq(DBTestPlanService.COLLECTION)))
			.thenReturn(List.of());

		var response = api("https://localhost.emobix.co.uk:8443").getParticipantPlans(requestedLimit);

		assertNotNull(response.getBody());
		assertEquals(expectedLimit, response.getBody().limit());
		ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
		verify(mongoTemplate).find(queryCaptor.capture(), eq(Plan.class), eq(DBTestPlanService.COLLECTION));
		assertEquals(expectedLimit, queryCaptor.getValue().getLimit());
	}

	private TemporaryOfbrCibaParticipantPlanApi api(String baseUrl) {
		return new TemporaryOfbrCibaParticipantPlanApi(mongoTemplate, authenticationFacade, baseUrl);
	}

	private Plan plan() {
		return new Plan(
			"plan1",
			"fapi-ciba-id1-test-plan",
			new VariantSelection(Map.of("fapi_ciba_profile", "openbanking_brazil")),
			new JsonObject(),
			Instant.parse("2026-07-09T10:15:30Z"),
			Map.of("sub", "unit-test-sub", "iss", "https://issuer.example.com"),
			"OFB-CIBARPtest",
			List.of(),
			List.of(),
			"unit-test-version",
			"unit-test-summary",
			null);
	}
}

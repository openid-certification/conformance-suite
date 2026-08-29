package net.openid.conformance.condition.as;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RevocationListRequestChecks_UnitTest {

	private static final String CWT = "application/statuslist+cwt";

	private Environment env;

	@BeforeEach
	public void setUp() {
		env = new Environment();
		env.putString(VP1FinalRevocationListRequest.SERVED_CONTENT_TYPE_ENV_KEY, CWT);
	}

	private void request(String method, String queryParams, String accept) {
		env.putObjectFromJsonString(VP1FinalRevocationListRequest.ENV_KEY, """
			{"method": "%s", "query_string_params": {%s}, "headers": {%s}}"""
			.formatted(method, queryParams, accept == null ? "" : "\"accept\": \"" + accept + "\""));
	}

	private <T extends net.openid.conformance.condition.AbstractCondition> T cond(T c) {
		c.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.INFO);
		return c;
	}

	@Test
	public void testGet_acceptsGet() {
		request("GET", "", CWT);
		assertDoesNotThrow(() -> cond(new EnsureRevocationListRequestUsedGet()).execute(env));
	}

	@Test
	public void testGet_rejectsPost() {
		request("POST", "", CWT);
		ConditionError err = assertThrows(ConditionError.class,
			() -> cond(new EnsureRevocationListRequestUsedGet()).execute(env));
		assertTrue(err.getMessage().contains("did not use GET"), err.getMessage());
	}

	@Test
	public void testQuery_acceptsNoParameters() {
		request("GET", "", CWT);
		assertDoesNotThrow(() -> cond(new EnsureRevocationListRequestQueryWasEmpty()).execute(env));
	}

	@Test
	public void testQuery_acceptsTheDefinedTimeParameter() {
		request("GET", "\"time\": \"1700000000\"", CWT);
		assertDoesNotThrow(() -> cond(new EnsureRevocationListRequestQueryWasEmpty()).execute(env));
	}

	@Test
	public void testQuery_rejectsAMisspeltTimeParameter() {
		request("GET", "\"tim\": \"1700000000\"", CWT);
		ConditionError err = assertThrows(ConditionError.class,
			() -> cond(new EnsureRevocationListRequestQueryWasEmpty()).execute(env));
		assertTrue(err.getMessage().contains("does not define"), err.getMessage());
	}

	@Test
	public void testAccept_acceptsTheServedType() {
		request("GET", "", CWT);
		assertDoesNotThrow(() -> cond(new EnsureRevocationListRequestAcceptedServedMediaType()).execute(env));
	}

	@Test
	public void testAccept_acceptsWildcardsAndWeights() {
		request("GET", "", "application/statuslist+jwt;q=0.9, */*;q=0.1");
		assertDoesNotThrow(() -> cond(new EnsureRevocationListRequestAcceptedServedMediaType()).execute(env));
		request("GET", "", "application/*");
		assertDoesNotThrow(() -> cond(new EnsureRevocationListRequestAcceptedServedMediaType()).execute(env));
	}

	@Test
	public void testAccept_absentHeaderIsNotAFinding() {
		request("GET", "", null);
		assertDoesNotThrow(() -> cond(new EnsureRevocationListRequestAcceptedServedMediaType()).execute(env));
	}

	@Test
	public void testAccept_rejectsAHeaderThatExcludesTheServedType() {
		request("GET", "", "application/statuslist+jwt");
		ConditionError err = assertThrows(ConditionError.class,
			() -> cond(new EnsureRevocationListRequestAcceptedServedMediaType()).execute(env));
		assertTrue(err.getMessage().contains("does not accept the media type"), err.getMessage());
	}
}

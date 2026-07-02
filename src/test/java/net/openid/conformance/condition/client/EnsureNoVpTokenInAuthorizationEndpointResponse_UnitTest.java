package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class EnsureNoVpTokenInAuthorizationEndpointResponse_UnitTest {

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureNoVpTokenInAuthorizationEndpointResponse cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new EnsureNoVpTokenInAuthorizationEndpointResponse();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	@Test
	public void testEvaluate_errorOnly() {
		env.putObjectFromJsonString("authorization_endpoint_response", """
			{ "error": "access_denied" }
			""");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_vpTokenReturned() {
		env.putObjectFromJsonString("authorization_endpoint_response", """
			{ "vp_token": { "my_credential": [ "eyJhbGci...QMA" ] } }
			""");

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertTrue(e.getMessage().contains("vp_token"),
			"failure message should mention vp_token, was: " + e.getMessage());
	}

	/**
	 * A vp_token returned alongside an 'error' parameter must still fail — a wallet that
	 * cannot satisfy the query must not return any credentials at all.
	 */
	@Test
	public void testEvaluate_vpTokenAlongsideError() {
		env.putObjectFromJsonString("authorization_endpoint_response", """
			{ "error": "access_denied", "vp_token": { "my_credential": [ "eyJhbGci...QMA" ] } }
			""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_emptyVpTokenReturned() {
		env.putObjectFromJsonString("authorization_endpoint_response", """
			{ "vp_token": {} }
			""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

}

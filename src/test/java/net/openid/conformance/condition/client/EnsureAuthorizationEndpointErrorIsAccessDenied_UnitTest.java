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
public class EnsureAuthorizationEndpointErrorIsAccessDenied_UnitTest {

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureAuthorizationEndpointErrorIsAccessDenied cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new EnsureAuthorizationEndpointErrorIsAccessDenied();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	@Test
	public void testEvaluate_accessDenied() {
		env.putObjectFromJsonString("authorization_endpoint_response", """
			{ "error": "access_denied" }
			""");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_differentError() {
		env.putObjectFromJsonString("authorization_endpoint_response", """
			{ "error": "invalid_request" }
			""");

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertTrue(e.getMessage().contains("access_denied"),
			"failure message should name the expected error code, was: " + e.getMessage());
	}

	@Test
	public void testEvaluate_noError() {
		env.putObjectFromJsonString("authorization_endpoint_response", """
			{ "vp_token": {} }
			""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

}

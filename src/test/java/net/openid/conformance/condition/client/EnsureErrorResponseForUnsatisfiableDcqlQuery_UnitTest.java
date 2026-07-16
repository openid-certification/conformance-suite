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
public class EnsureErrorResponseForUnsatisfiableDcqlQuery_UnitTest {

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureErrorResponseForUnsatisfiableDcqlQuery cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new EnsureErrorResponseForUnsatisfiableDcqlQuery();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	@Test
	public void testEvaluate_errorResponse() {
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

		assertTrue(e.getMessage().contains("error response"),
			"failure message should say an error response was expected, was: " + e.getMessage());
	}

	@Test
	public void testEvaluate_emptyVpTokenReturned() {
		env.putObjectFromJsonString("authorization_endpoint_response", """
			{ "vp_token": {} }
			""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_neitherErrorNorVpToken() {
		env.putObjectFromJsonString("authorization_endpoint_response", """
			{ "state": "xyz" }
			""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

}

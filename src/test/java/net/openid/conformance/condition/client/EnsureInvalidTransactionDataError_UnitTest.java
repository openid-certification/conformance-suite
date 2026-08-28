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
public class EnsureInvalidTransactionDataError_UnitTest {

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureInvalidTransactionDataError cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new EnsureInvalidTransactionDataError();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	@Test
	public void testEvaluate_expectedError() {
		env.putObjectFromJsonString("authorization_endpoint_response", """
			{ "error": "invalid_transaction_data" }
			""");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_wrongError() {
		env.putObjectFromJsonString("authorization_endpoint_response", """
			{ "error": "access_denied" }
			""");

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertTrue(e.getMessage().contains("unexpected value"),
			"failure message should say the error code was unexpected, was: " + e.getMessage());
	}

	@Test
	public void testEvaluate_missingError() {
		env.putObjectFromJsonString("authorization_endpoint_response", """
			{ "vp_token": { "my_credential": [ "eyJhbGci...QMA" ] } }
			""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

}

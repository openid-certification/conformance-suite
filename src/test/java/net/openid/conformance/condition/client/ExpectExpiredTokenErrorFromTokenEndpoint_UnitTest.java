package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ExpectExpiredTokenErrorFromTokenEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ExpectExpiredTokenErrorFromTokenEndpoint cond;

	private JsonObject tokenEndpointResponse;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ExpectExpiredTokenErrorFromTokenEndpoint();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		tokenEndpointResponse = new JsonObject();

		env.putObject("token_endpoint_response", tokenEndpointResponse);
	}

	@Test
	public void testEvaluate_NoErrorField() {
		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_ErrorFieldNotCorrect() {
		assertThrows(ConditionError.class, () -> {
			tokenEndpointResponse.addProperty("error", "access_denied");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_ErrorFieldValid() {
		tokenEndpointResponse.addProperty("error", "expired_token");
		cond.execute(env);
	}
}

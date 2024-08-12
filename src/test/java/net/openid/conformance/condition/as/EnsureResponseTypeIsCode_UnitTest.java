package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsureResponseTypeIsCode_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject authorizationEndpointRequest;

	private JsonObject invalidAuthorizationEndpointRequest;

	private EnsureResponseTypeIsCode cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new EnsureResponseTypeIsCode();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		String token =  "code";
		String invalidToken = "invalidToken";

		JsonObject sampleParams = new JsonObject();
		sampleParams.addProperty(CreateEffectiveAuthorizationRequestParameters.RESPONSE_TYPE, token);

		JsonObject invalidParams = new JsonObject();
		invalidParams.addProperty(CreateEffectiveAuthorizationRequestParameters.RESPONSE_TYPE, invalidToken);

		authorizationEndpointRequest = sampleParams;
		invalidAuthorizationEndpointRequest = invalidParams;
	}

	@Test
	public void testEvaluate_valuePresent() {

		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, authorizationEndpointRequest);

		cond.execute(env);

		assertNotNull(env.getElementFromObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.RESPONSE_TYPE));
	}

	@Test
	public void testEvaluate_valueMismatch() {
		assertThrows(ConditionError.class, () -> {

			env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, invalidAuthorizationEndpointRequest);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_valueMissing() {
		assertThrows(ConditionError.class, () -> {

			cond.execute(env);

		});

	}

}

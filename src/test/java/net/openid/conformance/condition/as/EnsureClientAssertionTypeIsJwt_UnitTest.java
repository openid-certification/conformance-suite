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
public class EnsureClientAssertionTypeIsJwt_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject tokenEndpointRequest;

	private JsonObject invalidTokenEndpointRequest;

	private EnsureClientAssertionTypeIsJwt cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new EnsureClientAssertionTypeIsJwt();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		String clientAssertionType =  "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
		String invalidClientAssertionType = "invalidAssertionType";

		JsonObject sampleParams = new JsonObject();
		sampleParams.addProperty("client_assertion_type", clientAssertionType);

		JsonObject invalidParams = new JsonObject();
		invalidParams.addProperty("client_assertion_type", invalidClientAssertionType);

		tokenEndpointRequest = new JsonObject();
		tokenEndpointRequest.add("body_form_params", sampleParams);

		invalidTokenEndpointRequest = new JsonObject();
		invalidTokenEndpointRequest.add("body_form_params", invalidParams);
	}

	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("token_endpoint_request", tokenEndpointRequest);

		cond.execute(env);

		assertNotNull(env.getElementFromObject("token_endpoint_request", "body_form_params.client_assertion_type"));
	}

	@Test
	public void testEvaluate_valueMismatch() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("token_endpoint_request", invalidTokenEndpointRequest);

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

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
public class EnsureInvalidRequestInvalidRequestObjectInvalidRequestUriOrAccessDeniedError_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureInvalidRequestInvalidRequestObjectInvalidRequestUriOrAccessDeniedError cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new EnsureInvalidRequestInvalidRequestObjectInvalidRequestUriOrAccessDeniedError();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		JsonObject authorizationEndpointResponse = new JsonObject();

		authorizationEndpointResponse.addProperty("error", "invalid_request");

		authorizationEndpointResponse.addProperty("error_description", "[A167307] Failed to find a client application whose ID matches the value of the 'iss' claim in the request object included in the backchannel authentication request.");

		authorizationEndpointResponse.addProperty("error_uri", "https://www.authlete.com/documents/apis/result_codes#A167307");

		env.putObject("authorization_endpoint_response", authorizationEndpointResponse);

	}

	@Test
	public void testEvaluate_notExistErrorField() {
		assertThrows(ConditionError.class, () -> {

			JsonObject authorizationEndpointResponse = env.getObject("authorization_endpoint_response");

			authorizationEndpointResponse.remove("error");

			env.putObject("authorization_endpoint_response", authorizationEndpointResponse);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_errorIsNotOneOfInvalidRequestInvalidRequestObjectOrAccessDenied() {
		assertThrows(ConditionError.class, () -> {

			JsonObject authorizationEndpointResponse = env.getObject("authorization_endpoint_response");

			authorizationEndpointResponse.addProperty("error", "slow_down");

			env.putObject("authorization_endpoint_response", authorizationEndpointResponse);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_successWithInvalidRequestError() {
		cond.execute(env);
	}

	@Test
	public void testEvaluate_successWithInvalidRequestObjectError() {

		JsonObject authorizationEndpointResponse = env.getObject("authorization_endpoint_response");

		authorizationEndpointResponse.addProperty("error", "invalid_request_object");

		env.putObject("authorization_endpoint_response", authorizationEndpointResponse);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_successWithAccessDeniedError() {

		JsonObject authorizationEndpointResponse = env.getObject("authorization_endpoint_response");

		authorizationEndpointResponse.addProperty("error", "access_denied");

		env.putObject("authorization_endpoint_response", authorizationEndpointResponse);

		cond.execute(env);

	}

}

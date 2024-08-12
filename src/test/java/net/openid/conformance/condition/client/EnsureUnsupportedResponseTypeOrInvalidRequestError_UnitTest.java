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
public class EnsureUnsupportedResponseTypeOrInvalidRequestError_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureUnsupportedResponseTypeOrInvalidRequestError cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new EnsureUnsupportedResponseTypeOrInvalidRequestError();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		JsonObject authorizationEndpointResponse = new JsonObject();

		authorizationEndpointResponse.addProperty("error", "unsupported_response_type");

		authorizationEndpointResponse.addProperty("error_description", "[A009305] The response type 'code' is not supported by this service.");

		authorizationEndpointResponse.addProperty("error_uri", "https://docs.authlete.com/#A009305");

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
	public void testEvaluate_errorIsNotOneOfUnsupportedResponseTypeOrInvalidRequest() {
		assertThrows(ConditionError.class, () -> {

			JsonObject authorizationEndpointResponse = env.getObject("authorization_endpoint_response");

			authorizationEndpointResponse.addProperty("error", "access_denied");

			env.putObject("authorization_endpoint_response", authorizationEndpointResponse);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_successWithErrorUnsupportedResponseType() {
		cond.execute(env);
	}

	@Test
	public void testEvaluate_successWithInvalidRequestError() {

		JsonObject authorizationEndpointResponse = env.getObject("authorization_endpoint_response");

		authorizationEndpointResponse.addProperty("error", "invalid_request");

		env.putObject("authorization_endpoint_response", authorizationEndpointResponse);

		cond.execute(env);

	}

}

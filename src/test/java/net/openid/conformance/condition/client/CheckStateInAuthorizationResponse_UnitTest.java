package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
public class CheckStateInAuthorizationResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckStateInAuthorizationResponse cond;

	private JsonObject responseWithState;

	private JsonObject responseWithoutState;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new CheckStateInAuthorizationResponse();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		responseWithState = JsonParser.parseString("{"
			+ "\"code\":\"SplxlOBeZQQYbYS6WxSbIA\","
			+ "\"state\":\"xyz\""
			+ "}").getAsJsonObject();

		responseWithoutState = JsonParser.parseString("{"
			+ "\"code\":\"SplxlOBeZQQYbYS6WxSbIA\""
			+ "}").getAsJsonObject();
	}

	@Test
	public void testEvaluate_goodStateResponse() {

		env.putString("state", "xyz");
		env.putObject("authorization_endpoint_response", responseWithState);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingStateRequestAndResponse() {

		env.putObject("authorization_endpoint_response", responseWithoutState);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingStateResponse() {
		assertThrows(ConditionError.class, () -> {

			env.putString("state", "xyz");
			env.putObject("authorization_endpoint_response", responseWithoutState);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_wrongStateResponse() {
		assertThrows(ConditionError.class, () -> {

			env.putString("state", "abc_xyz");
			env.putObject("authorization_endpoint_response", responseWithState);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingStateResponseWithInvalidRequestObject() {

		env.putString("state", "xyz");
		env.putObject("authorization_endpoint_response", JsonParser.parseString("{\"error_description\": \"Invalid request parameter JWS\",\"error\": \"invalid_request_object\"}").getAsJsonObject());

		cond.execute(env);
	}

	@Test
	public void testEvaluate_wrongStateResponseWithInvalidRequestObject() {
		assertThrows(ConditionError.class, () -> {

			env.putString("state", "abc_xyz");
			env.putObject("authorization_endpoint_response", JsonParser.parseString("{\"error_description\": \"Invalid request parameter JWS\",\"error\": \"invalid_request_object\",\"state\":\"xyz\"}").getAsJsonObject());

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingStateResponseWithInvalidRequest() {
		assertThrows(ConditionError.class, () -> {

			env.putString("state", "xyz");
			env.putObject("authorization_endpoint_response", JsonParser.parseString("{\"error_description\": \"Invalid request parameter\",\"error\": \"invalid_request\"}").getAsJsonObject());

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_goodStateResponseWithInvalidRequest() {

		env.putString("state", "xyz");
		env.putObject("authorization_endpoint_response", JsonParser.parseString("{\"error_description\": \"Invalid request parameter\",\"error\": \"invalid_request\",\"state\":\"xyz\"}").getAsJsonObject());

		cond.execute(env);
	}
}

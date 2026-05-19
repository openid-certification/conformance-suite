package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VerifyNoStateInAuthorizationResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VerifyNoStateInAuthorizationResponse cond;

	private JsonObject responseWithState;

	private JsonObject responseWithoutState;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new VerifyNoStateInAuthorizationResponse();

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
	public void testEvaluate_noError() {
		env.putObject("authorization_endpoint_response", responseWithoutState);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_presentState() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("authorization_endpoint_response", responseWithState);
			cond.execute(env);
		});
	}

}

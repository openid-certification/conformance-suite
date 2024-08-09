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
public class CheckErrorFromTokenEndpointResponseErrorInvalidGrant_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckErrorFromTokenEndpointResponseErrorInvalidGrant cond;

	private JsonObject tokenEndpointResponse;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckErrorFromTokenEndpointResponseErrorInvalidGrant();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		tokenEndpointResponse = JsonParser.parseString("{"
			+ "\"error_description\":\"[A200308] The end-user has not been authenticated yet.\","
			+ "\"error\":\"invalid_grant\","
			+ "\"error_uri\":\"https://www.authlete.com/documents/apis/result_codes#A200308\"}").getAsJsonObject();

		env.putObject("token_endpoint_response", tokenEndpointResponse);
	}

	@Test
	public void testEvaluate_NotExistTokenEndpointResponse() {
		assertThrows(ConditionError.class, () -> {
			env.removeObject("token_endpoint_response");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_ErrorValueEmpty() {
		assertThrows(ConditionError.class, () -> {
			tokenEndpointResponse.addProperty("error", "");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_ErrorValueValid() {
		cond.execute(env);
	}

	@Test
	public void testEvaluate_ErrorValueInvalid() {
		assertThrows(ConditionError.class, () -> {
			tokenEndpointResponse.addProperty("error", "invalid_request");
			cond.execute(env);
		});
	}
}

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
public class CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB cond;

	private JsonObject tokenEndpointResponse;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		tokenEndpointResponse = JsonParser.parseString("{"
			+ "\"error_description\":\"[A200308] The end-user has not been authenticated yet.\","
			+ "\"error\":\"authorization_pending\","
			+ "\"error_uri\":\"https://www.authlete.com/documents/apis/result_codes#A200308\"}").getAsJsonObject();

		env.putObject("token_endpoint_response", tokenEndpointResponse);
	}

	@Test
	public void testEvaluate_ErrorDescriptionFieldValid() {
		cond.execute(env);
	}

	@Test
	public void testEvaluate_ErrorDescriptionFieldInvalidWithTab() {
		assertThrows(ConditionError.class, () -> {
			tokenEndpointResponse.addProperty("error_description", "[A200308] The end-user has not been \t authenticated yet.");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_ErrorDescriptionFieldInvalidWithCR() {
		assertThrows(ConditionError.class, () -> {
			tokenEndpointResponse.addProperty("error_description", "[A200308] The end-user has not been \n authenticated yet.");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_ErrorDescriptionFieldInvalidWithLF() {
		assertThrows(ConditionError.class, () -> {
			tokenEndpointResponse.addProperty("error_description", "[A200308] The end-user has not been \r authenticated yet.");
			cond.execute(env);
		});
	}
}

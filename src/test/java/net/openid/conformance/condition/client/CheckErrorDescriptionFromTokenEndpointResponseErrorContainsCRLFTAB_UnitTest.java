package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB cond;

	private JsonObject tokenEndpointResponse;

	@Before
	public void setUp() throws Exception {
		cond = new CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		tokenEndpointResponse = new JsonParser().parse("{"
			+ "\"error_description\":\"[A200308] The end-user has not been authenticated yet.\","
			+ "\"error\":\"authorization_pending\","
			+ "\"error_uri\":\"https://www.authlete.com/documents/apis/result_codes#A200308\"}").getAsJsonObject();

		env.putObject("token_endpoint_response", tokenEndpointResponse);
	}

	@Test
	public void testEvaluate_ErrorDescriptionFieldValid() {
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_ErrorDescriptionFieldInvalidWithTab() {
		tokenEndpointResponse.addProperty("error_description", "[A200308] The end-user has not been \t authenticated yet.");
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_ErrorDescriptionFieldInvalidWithCR() {
		tokenEndpointResponse.addProperty("error_description", "[A200308] The end-user has not been \n authenticated yet.");
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_ErrorDescriptionFieldInvalidWithLF() {
		tokenEndpointResponse.addProperty("error_description", "[A200308] The end-user has not been \r authenticated yet.");
		cond.execute(env);
	}
}

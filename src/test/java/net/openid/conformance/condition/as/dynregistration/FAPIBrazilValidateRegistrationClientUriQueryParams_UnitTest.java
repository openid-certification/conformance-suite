package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
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
public class FAPIBrazilValidateRegistrationClientUriQueryParams_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private FAPIBrazilValidateRegistrationClientUriQueryParams cond;

	@Before
	public void setUp() throws Exception {

		cond = new FAPIBrazilValidateRegistrationClientUriQueryParams();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noErrorClientIdAsQuery() {

		JsonObject registrationClientUri = new JsonObject();
		registrationClientUri.addProperty("fullUrl", "https://server.example.com/connect/register?client_id=s6BhdRkqt3");
		env.putObject("registration_client_uri", registrationClientUri);

		JsonObject queryStringParams = JsonParser.parseString( "{"
			+ "  'query_string_params': {"
			+ "    'client_id': 's6BhdRkqt3'"
			+ "  }"
			+ "}").getAsJsonObject();
		env.putObject("client_request", queryStringParams);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_noError() {

		JsonObject registrationClientUri = new JsonObject();
		registrationClientUri.addProperty("fullUrl", "https://server.example.com/register/s6BhdRkqt3");
		env.putObject("registration_client_uri", registrationClientUri);

		JsonObject queryStringParams = JsonParser.parseString( "{"
			+ "  'query_string_params': {"
			+ "  }"
			+ "}").getAsJsonObject();
		env.putObject("client_request", queryStringParams);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingQueryParameters() {
		JsonObject registrationClientUri = new JsonObject();
		registrationClientUri.addProperty("fullUrl", "https://server.example.com/connect/register?client_id=s6BhdRkqt3&R44s5miZVmIM7JXU=1fl7brbcGbK55dc6");
		env.putObject("registration_client_uri", registrationClientUri);

		JsonObject queryStringParams = JsonParser.parseString( "{"
			+ "  'query_string_params': {"
			+ "    'client_id': 's6BhdRkqt3'"
			+ "  }"
			+ "}").getAsJsonObject();
		env.putObject("client_request", queryStringParams);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_extraQueryParameters() {

		JsonObject registrationClientUri = new JsonObject();
		registrationClientUri.addProperty("fullUrl", "https://server.example.com/connect/register?client_id=s6BhdRkqt3");
		env.putObject("registration_client_uri", registrationClientUri);

		JsonObject queryStringParams = JsonParser.parseString( "{"
			+ "  'query_string_params': {"
			+ "    'client_id': 's6BhdRkqt3',"
			+ "    'R44s5miZVmIM7JXU': '1fl7brbcGbK55dc6'"
			+ "  }"
			+ "}").getAsJsonObject();
		env.putObject("client_request", queryStringParams);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidQueryParameters() {

		JsonObject registrationClientUri = new JsonObject();
		registrationClientUri.addProperty("fullUrl", "https://server.example.com/connect/register?client_id=s6BhdRkqt3");
		env.putObject("registration_client_uri", registrationClientUri);

		JsonObject queryStringParams = JsonParser.parseString( "{"
			+ "  'query_string_params': {"
			+ "    'client_id': 'invalid'"
			+ "  }"
			+ "}").getAsJsonObject();
		env.putObject("client_request", queryStringParams);

		cond.execute(env);
	}
}

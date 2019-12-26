package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class VerifyScopesReturnedInAuthorizationEndpointIdToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private VerifyScopesReturnedInAuthorizationEndpointIdToken cond;

	private JsonObject authorizationEndpointIdToken;

	private JsonObject authorizationEndpointRequest;

	@Before
	public void setUp() throws Exception {

		cond = new VerifyScopesReturnedInAuthorizationEndpointIdToken();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		authorizationEndpointIdToken = new JsonParser().parse("{"
			+ "\"claims\":{"
				+ "\"sub\":\"1001\","
				+ "\"aud\":\"899532949009612\","
				+ "\"s_hash\":\"uPd-19KwcgDsqixsclXUjA\","
				+ "\"c_hash\":\"RoOjB2VE9I1C5l-1N7GiwA\","
				+ "\"nonce\":\"R4aIpghKxu\","
				+ "\"address\":{"
					+ "\"country\":\"USA\""
				+ "},"
				+ "\"auth_time\": 1577350731,"
				+ "\"iss\":\"https://fapidev-as.authlete.net/\","
				+ "\"exp\": 1577351031,"
				+ "\"iat\": 1577350731,"
				+ "\"email\":\"john@example.com\""
			+ "}}").getAsJsonObject();

		authorizationEndpointRequest = new JsonParser().parse("{"
				+ "\"scope\":\"openid address\""
			+ "}").getAsJsonObject();
	}

	@Test
	public void testEvaluate_noError() {
		env.putObject("authorization_endpoint_id_token", authorizationEndpointIdToken);
		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingClaimsInIdToken() {
		authorizationEndpointIdToken.remove("claims");
		env.putObject("authorization_endpoint_id_token", authorizationEndpointIdToken);
		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_claimsIsNotJsonObject() {
		authorizationEndpointIdToken.addProperty("claims", "is not JsonObject");
		env.putObject("authorization_endpoint_id_token", authorizationEndpointIdToken);
		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingScopeInAuthorizationEndpointRequest() {
		env.putObject("authorization_endpoint_id_token", authorizationEndpointIdToken);
		authorizationEndpointRequest.remove("scope");
		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_allScopeNamesIsNotReturnedInIdToken() {
		env.putObject("authorization_endpoint_id_token", authorizationEndpointIdToken);

		// 'email_verified' is not returned in claims of id_token
		authorizationEndpointRequest.addProperty("scope", "openid address email");
		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		cond.execute(env);
	}

}

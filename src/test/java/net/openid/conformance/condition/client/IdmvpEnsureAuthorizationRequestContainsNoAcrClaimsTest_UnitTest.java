package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationPARRequestParameters;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class IdmvpEnsureAuthorizationRequestContainsNoAcrClaimsTest_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private IdmvpEnsureAuthorizationRequestContainsNoAcrClaims cond;

	private JsonObject request;


	private void addRequestClaims(Environment env, JsonObject claims) {
		env.putObject(CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY, claims);
	}

	private void addIdtokenClaims(Environment env, JsonObject claims) {
		env.putObject(CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY, "claims.id_token", claims);
	}

	@Before
	public void setUp() throws Exception {
		cond = new IdmvpEnsureAuthorizationRequestContainsNoAcrClaims();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		String requestStr =
			"    {\n" +
				"      \"client_id\": \"52480754053\",\n" +
				"      \"redirect_uri\": \"https://example.com:8443/test/a/oidf-fapi2-op-test/callback\",\n" +
				"      \"scope\": \"openid\",\n" +
				"      \"response_type\": \"code\",\n" +
				"      \"code_challenge\": \"5RXriS_3zf4ASe_cX4LqerLGvNN1rGfUu5_EsqE99lg\",\n" +
				"      \"code_challenge_method\": \"S256\",\n" +
				"      \"claims\": {\n" +
				"        \"id_token\": {\n" +
				"          \"name\": null,\n" +
				"          \"given_name\": {},\n" +
				"          \"family_name\": {\n" +
				"            \"essential\": true\n" +
				"          },\n" +
				"          \"email\": {\n" +
				"            \"7EimPyJ0oq\": \"eRpxS9SF3u\"\n" +
				"          },\n" +
				"          \"birthdate\": {\n" +
				"            \"essential\": false\n" +
				"          },\n" +
				"          \"phone_number\": null,\n" +
				"          \"address\": {}\n" +
				"        }\n" +
				"      }\n" +
				"    }";

		JsonObject requestObj = (JsonObject) JsonParser.parseString(requestStr);
		addRequestClaims(env, requestObj);
	}


	@Test
	public void testEvaluate_noError() {
		addRequestClaims(env, request);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_idtokenSingleAcr() {

		String idtokenRequestStr =
			"{\n" +
			"  \"name\": null,\n" +
			"  \"acr\": {\"value\": \"urn:openbanking:psd2:sca\"}\n" +
			"}\n";

		JsonObject idtokenRequestObj = (JsonObject) JsonParser.parseString(idtokenRequestStr);

		addRequestClaims(env, request);
		addIdtokenClaims(env, idtokenRequestObj);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_idtokenMultipleAcr() {

		String idtokenRequestStr =
			"{\n" +
			"  \"name\": null,\n" +
			"  \"acr\": {\"values\": [ \"urn:openbanking:psd2:sca\", \"urn:openbanking:psd2:ca\" ] }\n" +
			"}\n";

		JsonObject idtokenRequestObj = (JsonObject) JsonParser.parseString(idtokenRequestStr);

		addRequestClaims(env, request);
		addIdtokenClaims(env, idtokenRequestObj);
		cond.execute(env);
	}


	@Test(expected = ConditionError.class)
	public void testEvaluate_acrvalues() {
		addRequestClaims(env, request);
		env.putString(CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY, "acr_values", "urn:openbanking:psd2:sca urn:openbanking:psd2:ca");
		cond.execute(env);
	}
}

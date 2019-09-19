package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ValidateIdTokenACRClaimAgainstRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateIdTokenACRClaimAgainstRequest cond;

	@Before
	public void setUp() throws Exception {
		cond = new ValidateIdTokenACRClaimAgainstRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_caseGoodEmpty() {

		env.putObject("authorization_endpoint_request", new JsonObject());

		env.putObject("id_token", new JsonObject());

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseBadEmpty() {
		String request =
			"{\n" +
				"  \"claims\": {\n" +
				"    \"id_token\": {\n" +
				"      \"acr\": {\n" +
				"        \"value\": \"urn:openbanking:psd2:sca\",\n" +
				"        \"essential\": true\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";
		JsonObject req = new JsonParser().parse(request).getAsJsonObject();
		env.putObject("authorization_endpoint_request", req);

		env.putObject("id_token", new JsonObject());

		cond.execute(env);

	}

	@Test
	public void testEvaluate_caseSingleGood() {
		String request =
			"{\n" +
				"  \"claims\": {\n" +
				"    \"id_token\": {\n" +
				"      \"acr\": {\n" +
				"        \"value\": \"urn:openbanking:psd2:sca\",\n" +
				"        \"essential\": true\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";
		JsonObject req = new JsonParser().parse(request).getAsJsonObject();
		env.putObject("authorization_endpoint_request", req);

		JsonObject idToken = new JsonParser().parse("{\"claims\": {\"acr\": \"urn:openbanking:psd2:sca\"}}").getAsJsonObject();
		env.putObject("id_token", idToken);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseSingleBad() {
		String request =
			"{\n" +
				"  \"claims\": {\n" +
				"    \"id_token\": {\n" +
				"      \"acr\": {\n" +
				"        \"value\": \"urn:openbanking:psd2:ca\",\n" +
				"        \"essential\": true\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		JsonObject req = new JsonParser().parse(request).getAsJsonObject();
		env.putObject("authorization_endpoint_request", req);

		JsonObject idToken = new JsonParser().parse("{\"claims\": {\"acr\": \"urn:mace:incommon:iap:silver\"}}").getAsJsonObject();
		env.putObject("id_token", idToken);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_caseArrayGood() {
		String request =
			"{\n" +
				"  \"claims\": {\n" +
				"    \"id_token\": {\n" +
				"      \"acr\": {\n" +
				"        \"values\": [\n" +
				"          \"urn:openbanking:psd2:sca\",\n" +
				"          \"urn:openbanking:psd2:ca\"\n" +
				"        ],\n" +
				"        \"essential\": true\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		JsonObject req = new JsonParser().parse(request).getAsJsonObject();
		env.putObject("authorization_endpoint_request", req);

		JsonObject idToken = new JsonParser().parse("{\"claims\": {\"acr\": \"urn:openbanking:psd2:sca\"}}").getAsJsonObject();
		env.putObject("id_token", idToken);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseArrayBad() {
		String request =
			"{\n" +
				"  \"claims\": {\n" +
				"    \"id_token\": {\n" +
				"      \"acr\": {\n" +
				"        \"values\": [\n" +
				"          \"urn:openbanking:psd2:sca\",\n" +
				"          \"urn:openbanking:psd2:ca\"\n" +
				"        ],\n" +
				"        \"essential\": true\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		JsonObject req = new JsonParser().parse(request).getAsJsonObject();
		env.putObject("authorization_endpoint_request", req);

		JsonObject idToken = new JsonParser().parse("{\"claims\": {\"acr\": \"urn:openbanking:psd2:s\"}}").getAsJsonObject();
		env.putObject("id_token", idToken);

		cond.execute(env);

	}
}

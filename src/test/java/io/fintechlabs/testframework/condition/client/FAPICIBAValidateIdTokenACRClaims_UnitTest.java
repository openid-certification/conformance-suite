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
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FAPICIBAValidateIdTokenACRClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private FAPICIBAValidateIdTokenACRClaims cond;

	@Before
	public void setUp() throws Exception {
		cond = new FAPICIBAValidateIdTokenACRClaims();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_caseGoodEmpty() {

		env.putObject("authorization_endpoint_request", new JsonObject());

		env.putObject("id_token", new JsonObject());

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseBadEmpty() {
		JsonObject req = new JsonObject();
		req.addProperty("acr_values", "urn:openbanking:psd2:sca");
		env.putObject("authorization_endpoint_request", req);

		env.putObject("id_token", new JsonObject());

		cond.evaluate(env);

	}

	@Test
	public void testEvaluate_caseSingleGood() {
		JsonObject req = new JsonObject();
		req.addProperty("acr_values", "urn:openbanking:psd2:sca urn:mace:incommon:iap:silver");
		env.putObject("authorization_endpoint_request", req);

		JsonObject idToken = new JsonParser().parse("{\"claims\": {\"acr\": \"urn:openbanking:psd2:sca\"}}").getAsJsonObject();
		env.putObject("id_token", idToken);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseSingleBad() {
		JsonObject req = new JsonObject();
		req.addProperty("acr_values", "urn:openbanking:psd2:sca");
		env.putObject("authorization_endpoint_request", req);

		JsonObject idToken = new JsonParser().parse("{\"claims\": {\"acr\": \"urn:mace:incommon:iap:silver\"}}").getAsJsonObject();
		env.putObject("id_token", idToken);

		cond.evaluate(env);

	}

	@Test
	public void testEvaluate_caseArrayGood() {
		JsonObject req = new JsonObject();
		req.addProperty("acr_values", "urn:openbanking:psd2:sca");
		env.putObject("authorization_endpoint_request", req);

		JsonObject idToken = new JsonParser().parse("{\"claims\": {\"acr\": \"urn:openbanking:psd2:sca  urn:mace:incommon:iap:silver\"}}").getAsJsonObject();
		env.putObject("id_token", idToken);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseArrayBad() {
		JsonObject req = new JsonObject();
		req.addProperty("acr_values", "urn:openbanking:psd2:sca");
		env.putObject("authorization_endpoint_request", req);

		JsonObject idToken = new JsonParser().parse("{\"claims\": {\"acr\": \"urn:mace:incommon:iap:silver urn:openbanking:psd2:ca\"}}").getAsJsonObject();
		env.putObject("id_token", idToken);

		cond.evaluate(env);

	}
}

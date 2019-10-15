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

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseBadEmpty() {
		JsonObject req = new JsonObject();
		req.addProperty("acr_values", "urn:openbanking:psd2:sca");
		env.putObject("authorization_endpoint_request", req);

		env.putObject("id_token", new JsonObject());

		cond.execute(env);

	}

	@Test
	public void testEvaluate_caseSingleGood() {
		JsonObject req = new JsonObject();
		req.addProperty("acr_values", "urn:openbanking:psd2:sca urn:mace:incommon:iap:silver");
		env.putObject("authorization_endpoint_request", req);

		JsonObject idToken = new JsonParser().parse("{\"claims\": {\"acr\": \"urn:openbanking:psd2:sca\"}}").getAsJsonObject();
		env.putObject("id_token", idToken);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseSingleBad() {
		JsonObject req = new JsonObject();
		req.addProperty("acr_values", "urn:openbanking:psd2:sca");
		env.putObject("authorization_endpoint_request", req);

		JsonObject idToken = new JsonParser().parse("{\"claims\": {\"acr\": \"urn:mace:incommon:iap:silver\"}}").getAsJsonObject();
		env.putObject("id_token", idToken);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_caseArrayGood() {
		JsonObject req = new JsonObject();
		req.addProperty("acr_values", "urn:openbanking:psd2:sca");
		env.putObject("authorization_endpoint_request", req);

		JsonObject idToken = new JsonParser().parse("{\"claims\": {\"acr\": \"urn:openbanking:psd2:sca  urn:mace:incommon:iap:silver\"}}").getAsJsonObject();
		env.putObject("id_token", idToken);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseArrayBad() {
		JsonObject req = new JsonObject();
		req.addProperty("acr_values", "urn:openbanking:psd2:sca");
		env.putObject("authorization_endpoint_request", req);

		JsonObject idToken = new JsonParser().parse("{\"claims\": {\"acr\": \"urn:mace:incommon:iap:silver urn:openbanking:psd2:ca\"}}").getAsJsonObject();
		env.putObject("id_token", idToken);

		cond.execute(env);

	}
}

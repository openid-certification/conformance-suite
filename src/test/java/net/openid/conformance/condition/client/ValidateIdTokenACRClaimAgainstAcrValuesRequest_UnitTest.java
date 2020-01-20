package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
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
public class ValidateIdTokenACRClaimAgainstAcrValuesRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateIdTokenACRClaimAgainstAcrValuesRequest cond;

	@Before
	public void setUp() throws Exception {
		cond = new ValidateIdTokenACRClaimAgainstAcrValuesRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_caseGood() {
		JsonObject req = new JsonObject();
		req.addProperty("acr_values", "1 2");
		env.putObject("authorization_endpoint_request", req);

		JsonObject id_token_claims = new JsonObject();
		id_token_claims.addProperty("acr", "2");
		JsonObject id_token = new JsonObject();
		id_token.add("claims", id_token_claims);
		env.putObject("id_token", id_token);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseGoodOne() {
		JsonObject req = new JsonObject();
		req.addProperty("acr_values", "1");
		env.putObject("authorization_endpoint_request", req);

		JsonObject id_token_claims = new JsonObject();
		id_token_claims.addProperty("acr", "1");
		JsonObject id_token = new JsonObject();
		id_token.add("claims", id_token_claims);
		env.putObject("id_token", id_token);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_badNotMatching() {
		JsonObject req = new JsonObject();
		req.addProperty("acr_values", "1 2");
		env.putObject("authorization_endpoint_request", req);

		JsonObject id_token_claims = new JsonObject();
		id_token_claims.addProperty("acr", "3");
		JsonObject id_token = new JsonObject();
		id_token.add("claims", id_token_claims);
		env.putObject("id_token", id_token);

		cond.execute(env);
	}


	@Test(expected = ConditionError.class)
	public void testEvaluate_badMissing() {
		JsonObject req = new JsonObject();
		req.addProperty("acr_values", "1 2");
		env.putObject("authorization_endpoint_request", req);

		JsonObject id_token_claims = new JsonObject();
		JsonObject id_token = new JsonObject();
		id_token.add("claims", id_token_claims);
		env.putObject("id_token", id_token);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_badNotAString() {
		JsonObject req = new JsonObject();
		req.addProperty("acr_values", "1 2");
		env.putObject("authorization_endpoint_request", req);

		JsonObject id_token_claims = new JsonObject();
		id_token_claims.addProperty("acr", 3);
		JsonObject id_token = new JsonObject();
		id_token.add("claims", id_token_claims);
		env.putObject("id_token", id_token);

		cond.execute(env);
	}
}

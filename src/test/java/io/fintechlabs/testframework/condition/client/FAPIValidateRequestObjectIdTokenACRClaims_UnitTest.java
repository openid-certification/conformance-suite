package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class FAPIValidateRequestObjectIdTokenACRClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private FAPIValidateRequestObjectIdTokenACRClaims cond;

	private JsonObject acrObject;

	private JsonObject acrName;

	private JsonObject idToken;

	private JsonObject claims;

	private JsonArray acrValues;

	private JsonArray invalidAcrValues;


	@Before
	public void setUp() throws Exception {

		cond = new FAPIValidateRequestObjectIdTokenACRClaims();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		acrValues = new JsonArray();
		acrValues.add("urn:openbanking:psd2:sca");
		acrValues.add("urn:openbanking:psd2:ca");

		acrObject = new JsonObject();
		acrObject.addProperty("essential", true);
		acrObject.add("values", acrValues);

		acrName = new JsonObject();
		acrName.add("acr", acrObject);

		idToken = new JsonObject();
		idToken.add("id_token", acrName);

		claims = new JsonObject();
		claims.add("claims", idToken);

	}

	private void addRequestObject(Environment env, JsonObject claims) {
		JsonObject requestObject = new JsonObject();
		requestObject.getAsJsonObject().add("claims", claims);
		env.putObject("authorization_request_object", requestObject);
	}


	@Test
	public void testEvaluate_noError() {

		addRequestObject(env, claims);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getElementFromObject("authorization_request_object", "claims.claims.id_token.acr.values");
		verify(env, atLeastOnce()).getElementFromObject("authorization_request_object", "claims.claims.id_token.acr.essential");

		assertThat(env.getElementFromObject("authorization_request_object", "claims.claims.id_token.acr.values")).isEqualTo(acrObject.get("values"));

	}

	@Test
	public void testEvaluate_successWithValidAndInvalidAcrValues() {

		invalidAcrValues = new JsonArray();
		invalidAcrValues.add("invalid:psd2:sca");
		invalidAcrValues.add("urn:openbanking:psd2:sca");

		acrObject.remove("values");
		acrObject.add("values", invalidAcrValues);

		addRequestObject(env, claims);

		cond.evaluate(env);
	}


	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidBooleanEssential() {

		acrObject.remove("essential");
		acrObject.addProperty("essential", false);

		addRequestObject(env, claims);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidAcrValue() {

		invalidAcrValues = new JsonArray();
		invalidAcrValues.add("invalid:psd2:sca");
		invalidAcrValues.add("invalid:psd2:ca");

		acrObject.remove("values");
		acrObject.add("values", invalidAcrValues);

		addRequestObject(env, claims);

		cond.evaluate(env);
	}

}

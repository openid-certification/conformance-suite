package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AddACRClaimToIdTokenClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddACRClaimToIdTokenClaims cond;

	private JsonObject claims;

	private JsonArray randomValue;

	private String acrValuesString;


	@Before
	public void setUp() throws Exception {

		cond = new AddACRClaimToIdTokenClaims();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		randomValue = new JsonArray();
		randomValue.add("randomvalue1");

		claims = new JsonObject();
		claims.add("claims", randomValue);

		acrValuesString = ("[\"urn:openbanking:psd2:sca\",\"urn:openbanking:psd2:ca\"]");

	}

	@Test
	public void testEvaluate_noError() {

		env.putObject("id_token_claims", claims);
		env.putString("requested_id_token_acr_values", acrValuesString);

		cond.execute(env);

		verify(env, atLeastOnce()).getObject("id_token_claims");
		assertEquals("urn:openbanking:psd2:sca", env.getString("id_token_claims", "acr"));

	}

	@Test
	public void testEvaluate_evaluateCorrectPrecedence() {

		env.putObject("id_token_claims", claims);
		env.putString("requested_id_token_acr_values", "[\"urn:openbanking:psd2:ca\"]");

		cond.execute(env);

		verify(env, atLeastOnce()).getObject("id_token_claims");
		assertEquals("urn:openbanking:psd2:ca", env.getString("id_token_claims", "acr"));
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidACRValue() {

		env.putObject("id_token_claims", claims);
		env.putString("requested_id_token_acr_values", "[\"urn:openbanking:invalid:ca\"]");

		cond.execute(env);

	}

}

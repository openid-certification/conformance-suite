package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AddACRClaimToIdTokenClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddACRClaimToIdTokenClaims cond;

	private JsonObject claims;

	private JsonArray randomValue;

	private String acrValuesString;


	@BeforeEach
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

	@Test
	public void testEvaluate_invalidACRValue() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("id_token_claims", claims);
			env.putString("requested_id_token_acr_values", "[\"urn:openbanking:invalid:ca\"]");

			cond.execute(env);

		});

	}

}

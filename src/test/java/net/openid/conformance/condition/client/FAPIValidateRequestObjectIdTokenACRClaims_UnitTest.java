package net.openid.conformance.condition.client;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
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

	@BeforeEach
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


	/**
	 * Test for case:
	 *     acr: { essential: true, values: ['urn:openbanking:psd2:sca', 'urn:openbanking:psd2:ca'] }
	 */
	@Test
	public void testEvaluate_noErrorWithAcrValues() {

		addRequestObject(env, claims);

		cond.execute(env);

		verify(env, atLeastOnce()).getElementFromObject("authorization_request_object", "claims.claims.id_token.acr");

		assertThat(env.getElementFromObject("authorization_request_object", "claims.claims.id_token.acr.values")).isEqualTo(acrObject.get("values"));

	}

	/**
	 * Test for case:
	 *     acr: { essential: true, value: 'urn:openbanking:psd2:sca' }
	 */
	@Test
	public void testEvaluate_noErrorWithAcrValue() {

		acrObject.remove("values");
		acrObject.addProperty("value", "urn:openbanking:psd2:sca");

		addRequestObject(env, claims);

		cond.execute(env);

		verify(env, atLeastOnce()).getElementFromObject("authorization_request_object", "claims.claims.id_token.acr");

		assertThat(env.getElementFromObject("authorization_request_object", "claims.claims.id_token.acr.value")).isEqualTo(acrObject.get("value"));

	}

	/**
	 * Test for case:
	 *     acr: { essential: true, values: ['invalid:psd2:sca', 'urn:openbanking:psd2:ca'] }
	 */
	@Test
	public void testEvaluate_successWithValidAndInvalidAcrValues() {

		invalidAcrValues = new JsonArray();
		invalidAcrValues.add("invalid:psd2:sca");
		invalidAcrValues.add("urn:openbanking:psd2:sca");

		acrObject.remove("values");
		acrObject.add("values", invalidAcrValues);

		addRequestObject(env, claims);

		cond.execute(env);
	}


	@Test
	public void testEvaluate_essentialFalse() {

		acrObject.remove("essential");
		acrObject.addProperty("essential", false);

		addRequestObject(env, claims);

		cond.execute(env);
	}

	/**
	 * Test for case:
	 *     acr: { essential: true, values: ['invalid:psd2:sca', 'invalid:psd2:ca'] }
	 */
	@Test
	public void testEvaluate_invalidAcrValues() {
		assertThrows(ConditionError.class, () -> {

			invalidAcrValues = new JsonArray();
			invalidAcrValues.add("invalid:psd2:sca");
			invalidAcrValues.add("invalid:psd2:ca");

			acrObject.remove("values");
			acrObject.add("values", invalidAcrValues);

			addRequestObject(env, claims);

			cond.execute(env);
		});
	}

	/**
	 * Test for case:
	 *     acr: { essential: true, value: 'invalid:psd2:sca' }
	 */
	@Test
	public void testEvaluate_invalidAcrValue() {
		assertThrows(ConditionError.class, () -> {

			acrObject.remove("values");
			acrObject.addProperty("value", "invalid:psd2:sca");

			addRequestObject(env, claims);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingBothAcrValueAndValues() {

		acrObject.remove("values");

		addRequestObject(env, claims);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_errorWithAcrValuesIsNotArray() {
		assertThrows(ConditionError.class, () -> {

			JsonObject invalidAcrValues = new JsonObject();
			invalidAcrValues.addProperty("invalidAcr", "invalid:psd2:sca");

			acrObject.remove("values");
			acrObject.add("values", invalidAcrValues);

			addRequestObject(env, claims);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingAcrClaim() {

		acrName.remove("acr");

		addRequestObject(env, claims);

		cond.execute(env);
	}

}
